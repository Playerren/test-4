/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintDefferability;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;

/**
 * Generic foreign manager
 */
public class GenericForeignKeyManager extends JDBCForeignKeyManager<GenericTableForeignKey, GenericTable> {


    @Override
    protected GenericTableForeignKey createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, GenericTable table, Object from)
    {
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(
            workbenchWindow.getShell(),
            "Create foreign key",
            activeEditor,
            table,
            new DBSConstraintModifyRule[] {
                DBSConstraintModifyRule.NO_ACTION,
                DBSConstraintModifyRule.CASCADE, DBSConstraintModifyRule.RESTRICT,
                DBSConstraintModifyRule.SET_NULL,
                DBSConstraintModifyRule.SET_DEFAULT });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final GenericTableForeignKey foreignKey = new GenericTableForeignKey(
            table,
            null,
            null,
            (GenericPrimaryKey) editDialog.getUniqueConstraint(),
            editDialog.getOnDeleteRule(),
            editDialog.getOnUpdateRule(),
            DBSConstraintDefferability.NOT_DEFERRABLE,
            false);
        foreignKey.setName(DBObjectNameCaseTransformer.transformName(foreignKey,
                CommonUtils.escapeIdentifier(table.getName()) + "_" +
                        CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getTable().getName()) + "_FK"));
        int colIndex = 1;
        for (EditForeignKeyDialog.FKColumnInfo tableColumn : editDialog.getColumns()) {
            foreignKey.addColumn(
                new GenericTableForeignKeyColumnTable(
                    foreignKey,
                    (GenericTableColumn) tableColumn.getOwnColumn(),
                    colIndex++,
                    (GenericTableColumn) tableColumn.getRefColumn()));
        }
        return foreignKey;
    }

}
