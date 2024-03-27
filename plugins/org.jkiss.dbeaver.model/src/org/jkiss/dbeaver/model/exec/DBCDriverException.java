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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.connection.DBPDriver;

public class DBCDriverException extends DBCException {
    private static final long serialVersionUID = 1L;
    private String driverClassName;
    private String driverFullName;
    private DBPDriver driver;

    public DBCDriverException(String message, String driverClassName, String driverFullName, DBPDriver driver,  Throwable e) {
        super(message, e);
        this.driverClassName = driverClassName;
        this.driverFullName = driverFullName;
        this.driver =  driver;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDriverFullName() {
        return driverFullName;
    }
    
    public DBPDriver getDriver() {
        return driver;
    }
    
    @Override
    public String getMessage() {
        return "Error of instantiation driver name: '"
            + getDriverFullName()
            + "',  class: '"
            + getDriverClassName()
            + "'"
            + "\nCause: "
            + "\n\t 1. Driver libraries (.jars) is not supplied in the product delivery"
            + "\n\t 2. Driver libraries (.jars) not found in path"
            + "\n\t 3. Driver configuration is not valid";
    }
}
