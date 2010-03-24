package org.jkiss.dbeaver.ui.views.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.runtime.load.tree.TreeLoadService;
import org.jkiss.dbeaver.runtime.load.tree.TreeLoadVisualizer;
import org.jkiss.dbeaver.runtime.load.NullLoadService;

import java.lang.reflect.InvocationTargetException;

/**
 * NavigatorTreeContentProvider
*/
class NavigatorTreeContentProvider implements IStructuredContentProvider, ITreeContentProvider
{
    static Log log = LogFactory.getLog(NavigatorTreeContentProvider.class);

    private static final Object[] EMPTY_CHILDREN = new Object[0];

    private NavigatorTreeView view;

    NavigatorTreeContentProvider(NavigatorTreeView view)
    {
        this.view = view;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    public void dispose()
    {
    }

    public Object[] getElements(Object parent)
    {
        return getChildren(parent);
    }

    public Object getParent(Object child)
    {
        DBMNode node = view.getMetaModel().findNode(child);
        if (node == null || node.getParentNode() == null) {
            return null;
        }
        return node.getParentNode().getObject();
    }

    public Object[] getChildren(final Object parent)
    {
        if (!(parent instanceof DBSObject)) {
            log.error("Bad parent type: " + parent);
            return null;
        }
        final DBMNode parentNode = view.getMetaModel().findNode(parent);
        if (parentNode == null) {
            log.error("Can't find parent node in model");
            return EMPTY_CHILDREN;
        }
        if (!parentNode.hasChildren()) {
            return EMPTY_CHILDREN;
        }
        if (parentNode.isLazyNode()) {
            return TreeLoadVisualizer.expandChildren(view.getViewer(), new TreeLoadService("Loading", parent) {
                public Object[] evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        return DBMNode.convertNodesToObjects(
                            parentNode.getChildren(this));
                    } catch (Throwable ex) {
                        if (ex instanceof InvocationTargetException) {
                            throw (InvocationTargetException)ex;
                        } else {
                            throw new InvocationTargetException(ex);
                        }
                    }
                }
            });
        }
        try {
            return DBMNode.convertNodesToObjects(
                parentNode.getChildren(new NullLoadService()));
        }
        catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                ex = ((InvocationTargetException)ex).getTargetException();
            }
            DBeaverUtils.showErrorDialog(
                view.getSite().getShell(),
                "Navigator error",
                ex.getMessage(),
                ex);
            // Collapse this item
            view.getSite().getShell().getDisplay().asyncExec(new Runnable() {
                public void run()
                {
                    view.getViewer().collapseToLevel(parent, 1);
                    view.getViewer().refresh(parent);
                }
            });
            return EMPTY_CHILDREN;
        }
    }

    public boolean hasChildren(Object parent)
    {
        DBMNode node = view.getMetaModel().findNode(parent);
        return node != null && node.hasChildren();
    }

/*
    public void cancelLoading(Object parent)
    {
        if (!(parent instanceof DBSObject)) {
            log.error("Bad parent type: " + parent);
        }
        DBSObject object = (DBSObject)parent;
        object.getDataSource().cancelCurrentOperation();
    }
*/

}
