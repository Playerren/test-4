/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;

/**
 * AbstractColumn
 */
public abstract class AbstractColumn implements DBSColumnBase
{
    protected String name;
    protected int valueType;
    protected long maxLength;
    protected boolean notNull;
    protected int scale;
    protected int precision;
    protected String typeName;
    protected int ordinalPosition;

    protected AbstractColumn()
    {
    }

    protected AbstractColumn(
        String name,
        String typeName,
        int valueType,
        int ordinalPosition,
        long maxLength,
        int scale,
        int precision,
        boolean notNull)
    {
        this.name = name;
        this.valueType = valueType;
        this.maxLength = maxLength;
        this.scale = scale;
        this.precision = precision;
        this.notNull = notNull;
        this.typeName = typeName;
        this.ordinalPosition = ordinalPosition;
    }

    @Property(name = "Column Name", viewable = true, order = 10)
    public String getName()
    {
        return name;
    }

    public void setName(String columnName)
    {
        this.name = columnName;
    }

    @Property(name = "Data Type", viewable = true, order = 20)
    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    @Property(name = "Ordinal Position", viewable = true, order = 30)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    public int getValueType()
    {
        return valueType;
    }

    public void setValueType(int valueType)
    {
        this.valueType = valueType;
    }

    @Property(name = "Length", viewable = true, order = 40)
    public long getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(long maxLength)
    {
        this.maxLength = maxLength;
    }

    @Property(name = "Not Null", viewable = true, order = 50)
    public boolean isNotNull()
    {
        return notNull;
    }

    public void setNotNull(boolean notNull)
    {
        this.notNull = notNull;
    }

    @Property(name = "Scale", viewable = false, order = 60)
    public int getScale()
    {
        return scale;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Property(name = "Precision", viewable = false, order = 61)
    public int getPrecision()
    {
        return precision;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    public String getDescription()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

}
