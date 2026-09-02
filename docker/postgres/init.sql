-- The 'statements' database is created via POSTGRES_DB.
-- Keycloak needs its own database in the same Postgres instance.
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO statements;

-- Convenience superuser matching the libpq/JDBC default username, so DB tools
-- (IDE database views, psql, DBeaver) can connect without overriding the user.
-- Demo default only; do not use in production.
CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';
