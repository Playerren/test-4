/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.jkiss.code.Nullable;

/**
 * IDialogPageProvider
 */
public interface IDialogPageProvider {

    @Nullable
    IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate);

    /**
     * Pages what should be saved during creation, even if they were
     * never opened
     *
     * @return array of IDialogPage[] to create at the initialisation,
     *      null if no additional pages should be created.
     */
    @Nullable
    default IDialogPage[] getRequiredDialogPages() {
        return null;
    }


}