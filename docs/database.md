# Database

CMS Platform requires a PostgreSQL database.

## Features

- **Open source** &ndash; no per-core/per-instance licensing costs.
- **ACID + MVCC** &ndash; concurrent reads/writes without blocking, keeps pages fast under load.
- **PostGIS** &ndash; powers maps and location/store-finder widgets.
- **Full text search** &ndash; `TSVECTOR` indexes search pages, wikis, and catalog items without a separate search engine.
- **LISTEN/NOTIFY** &ndash; invalidates caches in real time across clustered app instances.
- **Rich indexing** &ndash; B-tree, GIN, GiST for fast content, search, and spatial queries.
- **JSON/JSONB** &ndash; flexible storage for preferences, workflows, and form data.
- **Mature tooling** &ndash; `pg_dump`/`pg_restore`, replication, PITR, broad managed-service support (RDS, Azure, etc.).
- **Extensible** &ndash; new extensions can be added as features require.

## Maintenance

Flyway runs schema migrations automatically on startup, so admins don't manage schema changes by hand.

Ongoing DBA tasks:

- **Back up regularly** &ndash; schedule `pg_dump`/`pg_dumpall` or snapshots; test restores periodically.
- **Monitor autovacuum** &ndash; prevent bloat and transaction ID wraparound on high-write tables.
- **Watch disk/WAL growth** &ndash; especially on analytics- or e-commerce-heavy sites.
- **Size connection pools** &ndash; keep app/job/messaging pools under `max_connections`.
- **Manage roles/access** &ndash; least-privilege roles, rotate credentials, enable SSL (`DB_SSL`).
- **Watch slow queries** &ndash; use `pg_stat_statements` or logs to catch missing indexes.
- **Reindex as needed** &ndash; `REINDEX CONCURRENTLY` on bloated indexes.

## Upgrade

Major version upgrades (e.g. 15 → 18) change the on-disk format, so in-place binary upgrades aren't supported — admins must dump/restore or run `pg_upgrade`. This is separate from Flyway, which only manages schema.

### Before you start

1. Review release notes for 16, 17, and 18 for breaking changes.
2. Confirm PostGIS/other extensions support PostgreSQL 18.
3. Test the upgrade in staging first.
4. Take a full backup and keep the old data volume until verified.
5. Plan a maintenance window with no app writes.

### Dump and restore (recommended for multi-version jumps)

1. Stand up a PostgreSQL 18 instance (e.g. bump the image tag to `ghcr.io/rajkowski/cms-platform-db:18` in [docker-compose.yaml](../docker-compose.yaml)).
2. Create the database and required extensions on PostgreSQL 18.
3. Export: `pg_dump -Fc -d cms-platform -f cms-platform.dump`.
4. Restore: `pg_restore -d cms-platform --no-owner cms-platform.dump`.
5. Run `ANALYZE;` to refresh planner stats.
6. Point the app's connection settings at the new instance and start it; Flyway confirms the schema.
7. Validate the site before decommissioning the old server.

### After the upgrade

- Confirm backups target the new instance/version.
- Update infra/docs referencing the old major version.
- Monitor logs and query performance for a few days.

## References

- <https://www.postgresql.org/docs/current/upgrading.html>
