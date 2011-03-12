/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import net.sf.jkiss.utils.xml.XMLBuilder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hierarchy
 * @author Serge Rieder
 */
public class EntityDiagram extends ERDObject<DBSObject>
{

	private String name;
	private List<ERDTable> tables = new ArrayList<ERDTable>();
	private boolean layoutManualDesired = true;
	private boolean layoutManualAllowed = false;
    private Map<DBSTable, ERDTable> tableMap = new IdentityHashMap<DBSTable, ERDTable>();

    public EntityDiagram(DBSObject container, String name)
	{
		super(container);
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		this.name = name;
	}

	public synchronized void addTable(ERDTable table, boolean reflect)
	{
        addTable(table, -1, reflect);
	}

	public synchronized void addTable(ERDTable table, int i, boolean reflect)
	{
        if (i < 0) {
            tables.add(table);
        } else {
		    tables.add(i, table);
        }
        tableMap.put(table.getObject(), table);

        if (reflect) {
		    firePropertyChange(CHILD, null, table);
/*
            for (ERDAssociation rel : table.getPrimaryKeyRelationships()) {
                table.firePropertyChange(INPUT, null, rel);
            }
            for (ERDAssociation rel : table.getForeignKeyRelationships()) {
                table.firePropertyChange(OUTPUT, null, rel);
            }
*/
        }

        resolveRelations(reflect);

        if (reflect) {
            for (ERDAssociation rel : table.getPrimaryKeyRelationships()) {
                rel.getForeignKeyTable().firePropertyChange(OUTPUT, null, rel);
            }
        }
	}

    private void resolveRelations(boolean reflect)
    {
        // Resolve incomplete relations
        for (ERDTable erdTable : getTables()) {
            erdTable.resolveRelations(tableMap, reflect);
        }
    }

	public synchronized void removeTable(ERDTable table, boolean reflect)
	{
        tableMap.remove(table.getObject());
		tables.remove(table);
        if (reflect) {
		    firePropertyChange(CHILD, table, null);
        }
	}

    /**
	 * @return the Tables for the current schema
	 */
	public synchronized List<ERDTable> getTables()
	{
		return tables;
	}

	/**
	 * @return the name of the schema
	 */
	public String getName()
	{
		return name;
	}

    public void setName(String name)
    {
        this.name = name;
    }

	/**
	 * @param layoutManualAllowed
	 *            The layoutManualAllowed to set.
	 */
	public void setLayoutManualAllowed(boolean layoutManualAllowed)
	{
		this.layoutManualAllowed = layoutManualAllowed;
	}

	/**
	 * @return Returns the layoutManualDesired.
	 */
	public boolean isLayoutManualDesired()
	{
		return layoutManualDesired;
	}

	/**
	 * @param layoutManualDesired
	 *            The layoutManualDesired to set.
	 */
	public void setLayoutManualDesired(boolean layoutManualDesired)
	{
		this.layoutManualDesired = layoutManualDesired;
	}

	/**
	 * @return Returns whether we can lay out individual tables manually using the XYLayout
	 */
	public boolean isLayoutManualAllowed()
	{
		return layoutManualAllowed;
	}

    public int getEntityCount() {
        return tables.size();
    }

    public void load(InputStream in)
        throws IOException
    {

    }

    public void save(OutputStream out)
        throws IOException
    {
        XMLBuilder xml = new XMLBuilder(out, ContentUtils.DEFAULT_FILE_CHARSET);

        xml.startElement("diagram");
        xml.addAttribute("version", 1);
        xml.addAttribute("name", name);

        {
            xml.startElement("entities");
            for (ERDTable erdTable : tables) {
                final DBSTable table = erdTable.getObject();
                xml.startElement("entity");
                xml.addAttribute("ds", table.getDataSource().getContainer().getId());
                xml.addAttribute("name", table.getName());
                xml.addAttribute("fq-name", table.getFullQualifiedName());
                for (DBSObject parent = table.getParentObject(); parent != null && !(parent instanceof DBSDataSourceContainer); parent = parent.getParentObject()) {
                    xml.startElement("path");
                    xml.addText(parent.getName());
                    xml.endElement();
                }
                xml.endElement();
            }
            xml.endElement();
        }
        {
            xml.startElement("relations");
            xml.endElement();
        }
        {
            xml.startElement("notes");
            xml.endElement();
        }

        xml.endElement();

        xml.flush();
    }

    public EntityDiagram copy()
    {
        EntityDiagram copy = new EntityDiagram(getObject(), getName());
        copy.tables.addAll(this.tables);
        copy.tableMap.putAll(this.tableMap);
        copy.layoutManualDesired = this.layoutManualDesired;
        copy.layoutManualAllowed = this.layoutManualAllowed;
        return copy;
    }

    public void fillTables(DBRProgressMonitor monitor, Collection<DBSTable> tables, DBSObject dbObject)
    {
        // Load entities
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            ERDTable erdTable = ERDTable.fromObject(monitor, table);
            erdTable.setPrimary(table == dbObject);

            addTable(erdTable, false);
            tableMap.put(table, erdTable);
        }

        // Load relations
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            final ERDTable erdTable = tableMap.get(table);
            if (erdTable != null) {
                erdTable.addRelations(monitor, tableMap, false);
            }
        }
    }

    public boolean containsTable(DBSTable table)
    {
        for (ERDTable erdTable : tables) {
            if (erdTable.getObject() == table) {
                return true;
            }
        }
        return false;
    }

    public Map<DBSTable,ERDTable> getTableMap()
    {
        return tableMap;
    }
}