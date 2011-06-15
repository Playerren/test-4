/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCStringValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCStringValueHandler INSTANCE = new JDBCStringValueHandler();

    private static final int MAX_STRING_LENGTH = 0xffff;

    protected Object getColumnValue(DBCExecutionContext context, ResultSet resultSet, DBSTypedObject column,
                                    int columnIndex)
        throws SQLException
    {
        return resultSet.getString(columnIndex);
    }

    @Override
    public void bindParameter(DBCExecutionContext context, PreparedStatement statement, DBSTypedObject paramType,
                              int paramIndex, Object value)
        throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getValueType());
        } else {
            statement.setString(paramIndex, value.toString());
        }
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {

            Object value = controller.getValue();
            Text editor = new Text(controller.getInlinePlaceholder(), SWT.BORDER);
            editor.setText(value == null ? "" : value.toString());
            editor.setEditable(!controller.isReadOnly());
            editor.setTextLimit(MAX_STRING_LENGTH);
            editor.selectAll();
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<Text>() {
                public Object getValueFromControl(Text control)
                {
                    return control.getText();
                }
            });
            return true;
        } else {
            TextViewDialog dialog = new TextViewDialog(controller);
            dialog.open();
            return true;
        }
    }

    public Class getValueObjectType()
    {
        return String.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        // String are immutable
        return value;
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getColumnMetaData().getMaxLength());
    }

}