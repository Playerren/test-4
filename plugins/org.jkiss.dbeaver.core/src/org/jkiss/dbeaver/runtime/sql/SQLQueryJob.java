/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public class SQLQueryJob extends DataSourceJob
{
    static final Log log = LogFactory.getLog(SQLQueryJob.class);

    private static final int SUBTASK_COUNT = 5;
    //private static final int DEFAULT_MAX_ROWS = 500;

    private List<SQLStatementInfo> queries;
    private DBDDataReceiver dataReceiver;

    private SQLScriptCommitType commitType;
    private SQLScriptErrorHandling errorHandling;
    private boolean fetchResultSets;
    private long rsOffset;
    private long rsMaxRows;
    private long rowCount;

    private DBCStatement curStatement;
    private DBCResultSet curResultSet;
    //private boolean statementCancel = false;
    private boolean statementCanceled = false;
    private Throwable lastError = null;

    private List<ISQLQueryListener> queryListeners = new ArrayList<ISQLQueryListener>();

    public SQLQueryJob(
        String name,
        DBPDataSource dataSource,
        List<SQLStatementInfo> queries,
        DBDDataReceiver dataReceiver)
    {
        super(
            name,
            DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor(),
            dataSource);
        this.queries = queries;
        this.dataReceiver = dataReceiver;

        {
            // Read config form preference store
            IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
            this.commitType = SQLScriptCommitType.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_COMMIT_TYPE));
            this.errorHandling = SQLScriptErrorHandling.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_ERROR_HANDLING));
            this.fetchResultSets = (queries.size() == 1);
            this.rsMaxRows = preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);
        }
    }

    public SQLStatementInfo getLastQuery()
    {
        return queries.isEmpty() ? null : queries.get(0);
    }

    public void setDataReciever(DBDDataReceiver dataReceiver)
    {
        this.dataReceiver = dataReceiver;
    }

    public boolean hasLimits()
    {
        return rsOffset >= 0 && rsMaxRows > 0;
    }

    public void setResultSetLimit(long offset, long maxRows)
    {
        this.rsOffset = offset;
        this.rsMaxRows = maxRows;
    }

    public void addQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.add(listener);
    }

    public void removeQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.remove(listener);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            DBCExecutionContext context = getDataSource().openContext(monitor, queries.size() > 1 ? DBCExecutionPurpose.USER_SCRIPT : DBCExecutionPurpose.USER, "SQL Query");
            try {
// Set transaction settings (only if autocommit is off)
                QMUtils.getDefaultHandler().handleScriptBegin(context);

                DBCTransactionManager txnManager = context.getTransactionManager();
                boolean oldAutoCommit = txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (!oldAutoCommit && newAutoCommit != oldAutoCommit) {
                    txnManager.setAutoCommit(newAutoCommit);
                }

                monitor.beginTask(this.getName(), queries.size());

                // Notify job start
                for (ISQLQueryListener listener : queryListeners) {
                    listener.onStartJob();
                }

                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLStatementInfo query = queries.get(queryNum);

                    boolean runNext = executeSingleQuery(context, query, true);
                    if (!runNext) {
                        // Ask to continue
                        if (lastError != null) {
                            log.error(lastError);
                        }
                        SQLQueryErrorJob errorJob = new SQLQueryErrorJob(
                            lastError,
                            queryNum < queries.size() - 1);
                        errorJob.schedule();
                        try {
                            errorJob.join();
                        }
                        catch (InterruptedException e) {
                            log.error(e);
                        }

                        boolean stopScript = false;
                        switch (errorJob.getResponse()) {
                            case STOP:
                                // just stop execution
                                stopScript = true;
                                break;
                            case RETRY:
                                // just make it again
                                continue;
                            case IGNORE:
                                // Just do nothing
                                break;
                            case IGNORE_ALL:
                                errorHandling = SQLScriptErrorHandling.IGNORE;
                                break;
                        }

                        if (stopScript) {
                            break;
                        }
                    }

                    // Check monitor
                    if (monitor.isCanceled()) {
                        break;
                    }
                    monitor.worked(1);
                    queryNum++;
                }
                monitor.done();

                // Commit data
                if (!oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
                    if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                        if (commitType != SQLScriptCommitType.NO_COMMIT) {
                            monitor.beginTask("Commit data", 1);
                            txnManager.commit();
                            monitor.done();
                        }
                    } else {
                        monitor.beginTask("Rollback data", 1);
                        txnManager.rollback(null);
                        monitor.done();
                    }
                }

                // Restore transactions settings
                if (!oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(oldAutoCommit);
                }

/*
            if (lastError != null) {
                int code = lastError instanceof DBException ? ((DBException)lastError).getErrorCode() : 0;
                // Return error
                return new Status(
                    Status.ERROR,
                    DBeaverCore.getInstance().getPluginID(),
                    code,
                    lastError.getMessage(),
                    null);
            } else {
                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getInstance().getPluginID(),
                    "SQL job completed");
            }
*/
                QMUtils.getDefaultHandler().handleScriptEnd(context);

                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getInstance().getPluginID(),
                    "SQL job completed");
            }
            finally {
                context.close();
            }
        }
        catch (Throwable ex) {
            return new Status(
                Status.ERROR,
                DBeaverCore.getInstance().getPluginID(),
                "Error during SQL job execution: " + ex.getMessage());
        }
        finally {
            // Notify job end
            for (ISQLQueryListener listener : queryListeners) {
                listener.onEndJob(lastError != null);
            }
        }
    }

    private boolean executeSingleQuery(DBCExecutionContext context, SQLStatementInfo sqlStatement, boolean fireEvents)
    {
        lastError = null;

        if (fireEvents) {
            // Notify query start
            for (ISQLQueryListener listener : queryListeners) {
                listener.onStartQuery(sqlStatement);
            }
        }

        long startTime = System.currentTimeMillis();
        String sqlQuery = sqlStatement.getQuery();
        SQLQueryResult result = new SQLQueryResult(sqlStatement);
        if (rsOffset > 0) {
            result.setRowOffset(rsOffset);
        }
        //if (rsMaxRows > 0) {
        //    result.setRowCount(rsMaxRows);
        //}

        try {
            // Prepare statement
            closeStatement();
            curStatement = DBUtils.prepareStatement(context, sqlQuery, rsOffset, rsMaxRows);
            curStatement.setUserData(dataReceiver);

            // Bind parameters
            if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
                List<SQLStatementParameter> unresolvedParams = new ArrayList<SQLStatementParameter>();
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (!param.isResolved()) {
                        unresolvedParams.add(param);
                    }
                }
                if (!CommonUtils.isEmpty(unresolvedParams)) {
                    if (!bindStatementParameters(unresolvedParams)) {
                        return false;
                    }
                }
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (param.isResolved()) {
                        param.getValueHandler().bindValueObject(
                            context,
                            curStatement,
                            param,
                            param.getIndex(),
                            param.getValue());
                    }
                }
            }

            // Execute statement
            try {
                //monitor.subTask("Execute query");
                boolean hasResultSet = curStatement.executeStatement();
                // Show results only if we are not in the script execution
                if (hasResultSet && fetchResultSets) {
                    fetchQueryData(result, context);
                }
                long updateCount = curStatement.getUpdateRowCount();
                if (updateCount >= 0) {
                    result.setUpdateCount(updateCount);
                }
            }
            finally {
                //monitor.subTask("Close query");
                if (!keepStatementOpen()) {
                    closeStatement();
                }

                // Release parameters
                if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
                    for (SQLStatementParameter param : sqlStatement.getParameters()) {
                        if (param.isResolved()) {
                            param.getValueHandler().releaseValueObject(param.getValue());
                        }
                    }
                }
            }
        }
        catch (Throwable ex) {
            result.setError(ex);
            lastError = ex;
        }
        result.setQueryTime(System.currentTimeMillis() - startTime);

        if (fireEvents) {
            // Notify query end
            for (ISQLQueryListener listener : queryListeners) {
                listener.onEndQuery(result);
            }
        }

        if (result.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        return true;
    }

    private boolean bindStatementParameters(final List<SQLStatementParameter> parameters)
    {
        final Shell shell = DBeaverCore.getActiveWorkbenchShell();
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            public void run()
            {
                SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                    shell,
                    parameters);
                result = (dialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(shell, binder);
        return binder.getResult();
    }

    private void fetchQueryData(SQLQueryResult result, DBCExecutionContext context)
        throws DBCException
    {
        if (dataReceiver == null) {
            // No data pump - skip fetching stage
            return;
        }
        DBRProgressMonitor monitor = context.getProgressMonitor();
        monitor.subTask("Fetch result set");
        closeResultSet();
        curResultSet = curStatement.openResultSet();
        if (curResultSet != null) {
            rowCount = 0;

            dataReceiver.fetchStart(context, curResultSet);

            try {
                while ((!hasLimits() || rowCount < rsMaxRows) && curResultSet.nextRow()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    rowCount++;

                    if (rowCount % 100 == 0) {
                        monitor.subTask(rowCount + " rows fetched");
                        monitor.worked(100);
                    }

                    dataReceiver.fetchRow(context, curResultSet);
                }
            }
            finally {
                if (!keepStatementOpen()) {
                    closeResultSet();
                }

                try {
                    dataReceiver.fetchEnd(context);
                } catch (DBCException e) {
                    log.error("Error while handling end of result set fetch", e);
                }
                dataReceiver.close();
            }

            result.setRowCount(rowCount);
            monitor.subTask(rowCount + " rows fetched");
        }
    }

    private boolean keepStatementOpen()
    {
        // Only in single query mode and if pref option set to true
        return queries.size() == 1 &&
            getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.KEEP_STATEMENT_OPEN);
    }

    private void closeStatement()
    {
        closeResultSet();
        if (curStatement != null) {
            curStatement.close();
            curStatement = null;
        }
    }

    private void closeResultSet()
    {
        if (curResultSet != null) {
            curResultSet.close();
            curResultSet = null;
        }
    }

/*
    protected void canceling()
    {
        // Cancel statement only for the second time cancel is called
        */
/*if (!statementCancel) {
            statementCancel = true;
        } else *//*

        {
            if (!statementCanceled && curStatement != null) {
                try {
                    curStatement.cancelBlock();
                } catch (DBException e) {
                    log.error("Can't cancel execution: " + e.getMessage());
                }
                statementCanceled = true;
            }
        }
    }
*/

    public long extractData(DBCExecutionContext context)
        throws DBException
    {
        if (queries.size() != 1) {
            throw new DBException("Invalid state of SQL Query job");
        }
        boolean result = executeSingleQuery(context, queries.get(0), true);
        if (!result && lastError != null) {
            if (lastError instanceof DBException) {
                throw (DBException) lastError;
            } else {
                throw new DBException(lastError);
            }
        }
        return rowCount;
    }

    public void close()
    {
        closeStatement();
    }

}
