---
id: tenants
title: Multi-Tenant Setup
# prettier-ignore
description: CMS Platform can be configured for multiple tenants with a single application
---

CMS Platform supports multiple workspaces (tenants) in one application. Each workspace has its own canonical domain, file root, user access grants, and a dedicated database connection. The default site is considered the landing page CMS and management console.

## Application properties

Set the following values in the application environment or properties files:

```dotenv
CMS_TENANT_ROUTING_ENABLED=true
CMS_TENANT_DEFAULT_URL=http://localhost:8080
```

Set `CMS_TENANT_ROUTING_ENABLED=true` to resolve an incoming host through the
`workspace_domains` table.
`CMS_TENANT_DEFAULT_URL=http://localhost:8080` is the application's landing URL.

Tenant datasource connection limits are configured in
`database.properties`:

```properties
tenant.maximumPoolSize=8
```

`tenant.maximumPoolSize` is the maximum number of physical connections shared
by tenant datasources in the default pool group. If it is omitted, the value
of `application.maximumPoolSize` is used.

The application's base datasource properties are also used as defaults for the
application, background-job, and messaging pools. They do not create tenant
databases:

```properties
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.serverName=localhost
dataSource.portNumber=5432
dataSource.databaseName=cms-platform
dataSource.user=postgres
dataSource.password=
```

## Adding a tenant

The current codebase does not yet provide an admin screen or command for
creating workspaces. An administrator must create the records below through a
database migration or an approved administrative database tool. Run the
tenant-workspace migration first; it creates `workspaces`, `workspace_domains`,
`workspace_data_sources`, and `workspace_access_grants`.

1. Create an active workspace in `workspaces`.

   - `name`: display name shown to users.
   - `canonical_domain`: unique host users are sent to when selecting this workspace.
   - `file_root`: tenant file storage root.
   - `active`: set to `true` when the workspace is ready.

2. Add each host that should resolve to the workspace in `workspace_domains`.
   Set `wildcard` to `false` for an exact host such as `customer.example.com`
   or `true` for a pattern such as `*.example.com`. The host pattern must be
   unique and the mapping must be active.

3. Configure the workspace database in `workspace_data_sources`. Required
   values are `workspace_id`, `jdbc_url`, `username`, and `driver_class_name`.
   `password` is encrypted by the application when saved through its datasource
   store. `pool_group` can be used to share a connection budget with other
   tenants. For Azure service-principal authentication, set `auth_method` to
   `azure-sql-spn` and provide `azure_tenant_id`, `azure_client_id`, and
   `azure_client_secret`.

4. Grant users access by adding active rows to `workspace_access_grants`:

   ```sql
   INSERT INTO workspace_access_grants (workspace_id, user_id, active)
   VALUES (<workspace_id>, <user_id>, TRUE);
   ```

5. Restart the application, or use the application's tenant datasource
   registration lifecycle, so the new datasource is loaded. Verify that the
   workspace database has been initialized before directing users to it.

Do not put tenant database passwords or Azure client secrets in source control.

## Adding a tenant from the console

After building the application, an administrator can create the workspace,
domain mapping, and datasource record in one transaction with:

```shell
java -cp "target/cms-platform.jar:lib/compile/*:lib/build/*" com.zeroio.platform.AddTenant \
   --name "Customer Example" \
   --site-url https://customer.example.com \
   --domain customer.example.com \
   --file-root /var/lib/cms/customer-example \
   --jdbc-url jdbc:postgresql://localhost:5432/customer_example \
   --username postgres \
   --password '<database-password>'
```

The default database connection is loaded from
`target/cms-platform/WEB-INF/classes/database.properties`. Use
`--database-properties <path>` to load another default connection, or add
`--driver <class>` and `--pool-group <name>` for datasource-specific settings.
The tenant password is encrypted before it is stored. Do not retain the
password in shell history or source control.

## Switching between tenants

1. Sign in.
2. Open a page containing the **Workspace Selector** widget.
3. Select a workspace. The selector lists only active workspaces for which the
   signed-in user has an active row in `workspace_access_grants`.
4. Follow the workspace's canonical domain. The request is then handled in that
   workspace's context and uses its registered datasource.

Users can also open a mapped tenant domain directly when tenant routing is
enabled. An inactive workspace, inactive domain mapping, or missing access
grant prevents the workspace from being selected or resolved.

