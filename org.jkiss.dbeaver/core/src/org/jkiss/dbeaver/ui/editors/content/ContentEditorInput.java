/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.swt.SWTError;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.DBDStreamHandler;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDStreamKind;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.*;

/**
 * LOBEditorInput
 */
public class ContentEditorInput implements IFileEditorInput, IPathEditorInput //IDatabaseEditorInput
{
    static Log log = LogFactory.getLog(ContentEditorInput.class);

    private DBDValueController valueController;
    private IContentEditorPart[] editorParts;
    private IFile contentFile;

    ContentEditorInput(
        DBDValueController valueController,
        IContentEditorPart[] editorParts,
        IProgressMonitor monitor)
        throws CoreException
    {
        this.valueController = valueController;
        this.editorParts = editorParts;
        this.saveDataToFile(monitor);
    }

    public DBDValueController getValueController()
    {
        return valueController;
    }

    IContentEditorPart[] getEditors()
    {
        return editorParts;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBIcon.LOB.getImageDescriptor();
    }

    public String getName()
    {
        String tableName = valueController.getColumnMetaData().getTableName();
        return CommonUtils.isEmpty(tableName) ?
            valueController.getColumnMetaData().getColumnName() :
            tableName + "." + valueController.getColumnMetaData().getColumnName();
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return "LOB column editorPart";
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == IFile.class) {
            return contentFile;
        }
        return null;
    }

    private DBDStreamHandler getStreamHandler()
        throws DBCException
    {
        DBDStreamHandler streamHandler;
        if (valueController.getValueHandler() instanceof DBDStreamHandler) {
            streamHandler = (DBDStreamHandler) valueController.getValueHandler();
        } else {
            throw new DBCException("Value do not support streaming");
        }
        return streamHandler;
    }

    private void saveDataToFile(IProgressMonitor monitor)
        throws CoreException
    {
        try {
            DBDStreamHandler streamHandler = getStreamHandler();

            // Construct file name
            String fileName;
            try {
                fileName = makeFileName();
            } catch (DBException e) {
                throw new CoreException(
                    DBeaverUtils.makeExceptionStatus(e));
            }
            // Create file
            contentFile = DBeaverCore.getInstance().makeTempFile(
                fileName,
                "data",
                monitor);

            // Write value to file
            Object value = valueController.getValue();
            if (value != null) {
                File file = contentFile.getLocation().toFile();
                Object contents = streamHandler.getContents(value);
                long contentLength = streamHandler.getContentLength(value);
                if (contents != null) {
                    copyContentToFile(contents, contentLength, file, monitor);
                }
            }

            // Mark file as readonly
            if (valueController.isReadOnly()) {
                ResourceAttributes attributes = contentFile.getResourceAttributes();
                if (attributes != null) {
                    attributes.setReadOnly(true);
                    contentFile.setResourceAttributes(attributes);
                }
            }

        }
        catch (DBCException e) {
            throw new CoreException(
                DBeaverUtils.makeExceptionStatus(e));
        }
        catch (IOException e) {
            throw new CoreException(
                DBeaverUtils.makeExceptionStatus(e));
        }
    }

    private String makeFileName() throws DBException {
        StringBuilder fileName = new StringBuilder(valueController.getColumnId());
        if (valueController.getValueLocator() != null) {
            fileName.append(valueController.getValueLocator().getKeyId(valueController.getRow()));
        }
        return fileName.toString();
    }

    void release(IProgressMonitor monitor)
    {
        if (contentFile != null) {
            try {
                contentFile.delete(true, false, monitor);
            }
            catch (CoreException e) {
                log.warn(e);
            }
            //contentFile = null;
        }
    }

    public IFile getFile() {
        return contentFile;
    }

    public IStorage getStorage()
        throws CoreException
    {
        return contentFile;
    }

    public IPath getPath()
    {
        return contentFile == null ? null : contentFile.getFullPath();
    }

    public boolean isReadOnly() {
        return valueController.isReadOnly();
    }

    void saveToExternalFile(File file, IProgressMonitor monitor)
        throws CoreException
    {
        try {
            InputStream contents = contentFile.getContents(true);
            try {
                OutputStream os = new FileOutputStream(file);
                try {
                    ContentUtils.copyStreams(contents, file.length(), os, monitor);
                }
                finally {
                    os.close();
                }
                // Check for cancel
                if (monitor.isCanceled()) {
                    // Delete output file
                    if (!file.delete()) {
                        log.warn("Could not delete incomplete file '" + file.getAbsolutePath() + "'");
                    }
                }
            }
            finally {
                contents.close();
            }
        }
        catch (IOException e) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(e));
        }
    }

    void loadFromExternalFile(File extFile, IProgressMonitor monitor)
        throws CoreException
    {
        try {
            InputStream inputStream = new FileInputStream(extFile);
            try {
                File intFile = contentFile.getLocation().toFile();
                OutputStream outputStream = new FileOutputStream(intFile);
                try {
                    ContentUtils.copyStreams(inputStream, extFile.length(), outputStream, monitor);
                }
                finally {
                    outputStream.close();
                }
                // Append zero empty content to trigger content refresh
                contentFile.appendContents(
                    new ByteArrayInputStream(new byte[0]),
                    true,
                    false,
                    monitor);
            }
            finally {
                inputStream.close();
            }
        }
        catch (Throwable e) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(e));
        }
    }

    private void copyContentToFile(Object contents, long contentLength, File file, IProgressMonitor monitor)
        throws DBCException, IOException
    {
        if (contents instanceof InputStream) {
            InputStream inputStream = (InputStream)contents;
            try {
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    ContentUtils.copyStreams(inputStream, contentLength, outputStream, monitor);
                }
                finally {
                    outputStream.close();
                }
            }
            finally {
                inputStream.close();
            }
        } else if (contents instanceof Reader) {
            Reader reader = (Reader)contents;
            try {
                Writer writer = new FileWriter(file);
                try {
                    ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                }
                finally {
                    writer.close();
                }
            }
            finally {
                reader.close();
            }
        } else {
            throw new DBCException("Unsupported content stream type: " + contents);
        }
    }

    void updateContentFromFile(IProgressMonitor monitor)
        throws DBCException, IOException
    {
        if (valueController.isReadOnly()) {
            throw new DBCException("Could not update read-only value");
        }

/*
        DBDStreamHandler streamHandler = getStreamHandler();
        DBDStreamKind streamKind = streamHandler.getContentKind(valueController.getColumnMetaData());
        Object originalValue = valueController.getValue();

        File file = contentFile.getLocation().toFile();

        InputStream inputStream = new FileInputStream(file);
        try {
            Object newContent;
            if (streamKind == DBDStreamKind.BINARY) {
                newContent = inputStream;
            } else {
                newContent = new InputStreamReader(inputStream);
            }
            streamHandler.updateContents(originalValue, newContent, file.length(), monitor);
        }
        finally {
            inputStream.close();
        }
*/
    }

}