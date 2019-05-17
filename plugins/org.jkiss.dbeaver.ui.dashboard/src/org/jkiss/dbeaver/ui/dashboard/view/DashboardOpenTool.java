/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

import java.util.Collection;

public class DashboardOpenTool implements IUserInterfaceTool {


    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException {
        // Just open dashboard view
        if (objects.isEmpty()) {
            return;
        }
        DBSObject object = objects.iterator().next();
        DBPDataSourceContainer dataSourceContainer = object.getDataSource().getContainer();
        if (dataSourceContainer == null) {
            return;
        }
        try {
            window.getActivePage().showView(DashboardView.VIEW_ID, dataSourceContainer.getId(), IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Dashboard view", "Can't open dashboard view", e);
        }
    }

}