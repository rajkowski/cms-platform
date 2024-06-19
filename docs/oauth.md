---
id: oauth
title: OAuth Setup
# prettier-ignore
description: CMS Platform can be configured for OAuth users, roles, and groups
---

CMS Platform has built-in user logins and management. OAuth can be configured for validating user logins instead, and the OAuth provider can also provide roles and groups to be used within CMS Platform.

Roles map to the built-in portal roles, such as System Administrator, Content Manager, and Data Manager. Web pages, specific widgets, and data directories can be limited to specific roles.

Groups map to the customized portal groups which users can be assigned to. Web pages, specific widgets, and data directories can be limited to specific groups.

## OAuth Provider (Environment Variables example)

In OAuth Provider, configure the client and redirect URL, a dev example would be: `http://localhost:8080/oauth/callback`. Record the client id and secret.

On CMS Platform startup, configure the following environment variables:

The Site's URL, used in general places and redirects:

```bash
CMS_SITE_URL=http://localhost:8080
```

The OAuth details:

```bash
OAUTH_ENABLED=true
OAUTH_SERVER_URL=https://localhost/realms/example
OAUTH_CLIENT_ID=cms-platform
OAUTH_CLIENT_SECRET=client-secret
OAUTH_REDIRECT_URI=/oauth/callback
```

Optional variables:

```bash
CMS_OAUTH_ROLE_ATTRIBUTE=roles
CMS_OAUTH_GROUP_ATTRIBUTE=groups
CMS_OAUTH_ROLE_ADMIN=name_of_role
CMS_OAUTH_GROUP_LIST=group1, group-2, group_3
```

## OAuth Provider Login (Keycloak example)

In Keycloak:

1. Create a realm or use an existing one
2. Add a client: `cms-platform`

In the CMS Platform Database, if you're not using environment variables, configure the OAuth provider:

```sql
UPDATE site_properties SET property_value = 'true' WHERE property_name = 'oauth.enabled';
UPDATE site_properties SET property_value = 'Keycloak' WHERE property_name = 'oauth.provider';
UPDATE site_properties SET property_value = 'https://localhost/realms/example' WHERE property_name = 'oauth.serverUrl';
UPDATE site_properties SET property_value = 'cms-platform' WHERE property_name = 'oauth.clientId';
UPDATE site_properties SET property_value = 'client-secret' WHERE property_name = 'oauth.clientSecret';
UPDATE site_properties SET property_value = true WHERE property_name = 'oauth.redirectGuests';
```

## OAuth Groups and Roles Mapping to CMS Platform

Groups and roles can be created in Active Directory and Keycloak. During SSO, CMS Platform can check the user's info and group memberships.

The following can be set using the environment variables above, or in the database...

For Keycloak:

1. Add Client Roles to Keycloak: system-administrator, content-manager, community-manager, data-manager, ecommerce-manager
2. Add Realm Groups to Keycloak: employees, supervisors, global-data-manager, etc.
3. Create Client Mappers and Tokens: User Client Role (roles), Group Membership (groups)
4. Create users and choose roles and groups for the user

In the CMS Platform Database, configure the roles mappings to existing CMS Platform roles:

```sql
UPDATE site_properties SET property_value = 'roles' WHERE property_name = 'oauth.role.attribute';
UPDATE lookup_role SET oauth_path = 'system-administrator' where code = 'admin';
UPDATE lookup_role SET oauth_path = 'content-manager' where code = 'content-manager';
UPDATE lookup_role SET oauth_path = 'community-manager' where code = 'community-manager';
UPDATE lookup_role SET oauth_path = 'data-manager' where code = 'data-manager';
UPDATE lookup_role SET oauth_path = 'ecommerce-manager' where code = 'ecommerce-manager';
```

In the CMS Platform Database, for new or existing groups you can configure the groups mappings:

```sql
UPDATE site_properties SET property_value = 'groups' WHERE property_name = 'oauth.group.attribute';
UPDATE groups SET oauth_path = '/learners' WHERE unique_id = 'learners';
UPDATE groups SET oauth_path = '/instructors' WHERE unique_id = 'instructors';
```

Reset the cache
