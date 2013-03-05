/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DataTransferNodeDescriptor
 */
public class DataTransferNodeDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTransformer"; //$NON-NLS-1$

    enum NodeType {
        PRODUCER,
        CONSUMER
    }

    private String id;
    private String name;
    private String description;
    private Image icon;
    private NodeType nodeType;
    private ObjectType implType;
    private ObjectType settingsType;
    private List<ObjectType> sourceTypes = new ArrayList<ObjectType>();
    private List<ObjectType> pageTypes = new ArrayList<ObjectType>();
    private List<DataTransferProcessorDescriptor> processors = new ArrayList<DataTransferProcessorDescriptor>();

    public DataTransferNodeDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        String iconPath = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }
        nodeType = NodeType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase());
        implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        settingsType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_SETTINGS));
        for (IConfigurationElement typeCfg : CommonUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }
        for (IConfigurationElement pageConfig : CommonUtils.safeArray(config.getChildren(RegistryConstants.TAG_PAGE))) {
            pageTypes.add(new ObjectType(pageConfig.getAttribute(RegistryConstants.ATTR_CLASS)));
        }
        for (IConfigurationElement processorConfig : CommonUtils.safeArray(config.getChildren(RegistryConstants.TAG_PROCESSOR))) {
            processors.add(new DataTransferProcessorDescriptor(processorConfig));
        }
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Image getIcon()
    {
        return icon;
    }

    public Class<? extends IDataTransferNode> getNodeClass()
    {
        return implType.getObjectClass(IDataTransferNode.class);
    }

    public IDataTransferNode createNode() throws DBException
    {
        try {
            return implType.getObjectClass(IDataTransferNode.class).newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create data transformer node", e);
        }
    }

    public IDataTransferSettings createSettings() throws DBException
    {
        try {
            return settingsType.getObjectClass(IDataTransferSettings.class).newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create node settings", e);
        }
    }

    public IWizardPage[] createWizardPages()
    {
        List<IWizardPage> pages = new ArrayList<IWizardPage>();
        for (ObjectType type : pageTypes) {
            try {
                pages.add(type.getObjectClass(IWizardPage.class).newInstance());
            } catch (Throwable e) {
                log.error("Can't create wizard page", e);
            }
        }
        return pages.toArray(new IWizardPage[pages.size()]);
    }

    public NodeType getNodeType()
    {
        return nodeType;
    }

    public boolean appliesToType(Class objectType)
    {
        if (!sourceTypes.isEmpty()) {
            for (ObjectType sourceType : sourceTypes) {
                if (sourceType.matchesType(objectType)) {
                    return true;
                }
            }
        }
        for (DataTransferProcessorDescriptor processor : processors) {
            if (processor.appliesToType(objectType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns data exporter which supports ALL specified object types
     * @param objectTypes object types
     * @return list of editors
     */
    public Collection<DataTransferProcessorDescriptor> getAvailableProcessors(Collection<Class<?>> objectTypes)
    {
        List<DataTransferProcessorDescriptor> editors = new ArrayList<DataTransferProcessorDescriptor>();
        for (DataTransferProcessorDescriptor descriptor : processors) {
            boolean supports = true;
            for (Class objectType : objectTypes) {
                if (!descriptor.appliesToType(objectType)) {
                    supports = false;
                    break;
                }
            }
            if (supports) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

    public DataTransferProcessorDescriptor getProcessor(String id)
    {
        for (DataTransferProcessorDescriptor descriptor : processors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

}
