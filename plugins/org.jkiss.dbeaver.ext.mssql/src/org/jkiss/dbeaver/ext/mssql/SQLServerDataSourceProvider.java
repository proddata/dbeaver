/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerAuthentication;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class SQLServerDataSourceProvider extends JDBCDataSourceProvider {

    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public SQLServerDataSourceProvider()
    {
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        if (connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return connectionInfo.getUrl();
        }
        
        StringBuilder url = new StringBuilder();
        boolean isJtds = SQLServerUtils.isDriverJtds(driver);
        boolean isSqlServer = SQLServerUtils.isDriverSqlServer(driver);
        boolean isDriverAzure = isSqlServer && SQLServerUtils.isDriverAzure(driver);

        if (isSqlServer) {
            // SQL Server
            if (isJtds) {
                url.append("jdbc:jtds:sqlserver://");
                url.append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort()) && !connectionInfo.getHostPort().equals(driver.getDefaultPort())) {
                    url.append(":").append(connectionInfo.getHostPort());
                }
            } else {
                url.append("jdbc:sqlserver://");
                url.append(";serverName=").append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort()) && !connectionInfo.getHostPort().equals(driver.getDefaultPort())) {
                    url.append(";port=").append(connectionInfo.getHostPort());
                }
            }
            if (isJtds) {
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("/").append(connectionInfo.getDatabaseName());
                }
            } else {
                url.append(";");
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("databaseName=").append(connectionInfo.getDatabaseName());
                }

                if (isDriverAzure) {
                    url.append(";encrypt=true"); // ;hostNameInCertificate=*.database.windows.net
                }
            }
/*
            if ("TRUE".equalsIgnoreCase(connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH))) {
                url.append(";integratedSecurity=true");
            }
*/
        } else {
            // Sybase
            if (isJtds) {
                url.append("jdbc:jtds:sybase://");
                url.append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    url.append(":").append(connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("/").append(connectionInfo.getDatabaseName());
                }
            } else {
                url.append("jdbc:sybase:Tds:");
                url.append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    url.append(":").append(connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("?ServiceName=").append(connectionInfo.getDatabaseName());
                }
            }
        }

        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container)
            throws DBException
    {
        return new SQLServerDataSource(monitor, container);
    }

    @Override
    public DBPAuthModelDescriptor detectConnectionAuthModel(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        if (driver.getProviderDescriptor().matchesId(SQLServerConstants.PROVIDER_SQL_SERVER) &&
            (CommonUtils.isEmpty(connectionInfo.getAuthModelId()) ||
            connectionInfo.getAuthModelId().equals(AuthModelDatabaseNative.ID)))
        {
            // Convert legacy config to auth model
            SQLServerAuthentication authSchema = SQLServerUtils.detectAuthSchema(connectionInfo);
            String amId = authSchema.getReplacedByAuthModelId();
            DBPAuthModelDescriptor authModel = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getAuthModel(amId);
            if (authModel != null) {
                return authModel;
            }
            log.error("Replacement auth model " + amId + " not found");
        }
        return super.detectConnectionAuthModel(driver, connectionInfo);
    }

}
