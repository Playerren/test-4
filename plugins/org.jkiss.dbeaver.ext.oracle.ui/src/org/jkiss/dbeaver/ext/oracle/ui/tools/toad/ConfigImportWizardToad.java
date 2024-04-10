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
package org.jkiss.dbeaver.ext.oracle.ui.tools.toad;

import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizard;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;

import java.io.File;

public class ConfigImportWizardToad extends ConfigImportWizard {

    private ConfigImportWizardPageFile pageFile;

    @Override
    protected ConfigImportWizardPage createMainPage() {
        return new ConfigImportWizardPageToadConnections();
    }

    @Override
    public void addPages() {
        pageFile = new ConfigImportWizardPageFile();
        addPage(pageFile);
        super.addPages();
    }

    public File getInputFile() {
        return pageFile.getInputFile();
    }
}
