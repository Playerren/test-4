/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.properties.tabbed.SourceEditSection;

/**
 * OracleSourceViewSection
 */
public class OracleSourceViewSection extends SourceEditSection {

    private OracleSourceObject sourceObject;
    private boolean body;

    public OracleSourceViewSection(IDatabaseNodeEditor editor, boolean body)
    {
        super(editor);
        this.sourceObject = (OracleSourceObject) editor.getEditorInput().getDatabaseObject();
        this.body = body;
    }

    @Override
    protected boolean isSourceRead()
    {
        return false;
    }

    @Override
    protected String loadSources(DBRProgressMonitor monitor) throws DBException
    {
        return OracleUtils.getSource(monitor, sourceObject, body);
    }

    protected void updateSources(String source)
    {
        getEditor().getEditorInput().getPropertySource().setPropertyValue(
            body ?
                OracleConstants.PROP_SOURCE_DEFINITION :
                OracleConstants.PROP_SOURCE_DECLARATION,
            source);
    }

}