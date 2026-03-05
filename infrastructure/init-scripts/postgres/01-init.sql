-- This script runs automatically when PostgreSQL container
-- starts for the first time (only on first startup)

-- Create separate schema for Keycloak
-- Schemas are like namespaces inside a database
-- Keycloak tables won't mix with our application tables
CREATE SCHEMA IF NOT EXISTS keycloak;

-- Grant all permissions on keycloak schema to our user
GRANT ALL PRIVILEGES ON SCHEMA keycloak TO streamcms;

-- Our application schemas (one per service - good practice)
CREATE SCHEMA IF NOT EXISTS media;
CREATE SCHEMA IF NOT EXISTS cms;
CREATE SCHEMA IF NOT EXISTS live;
CREATE SCHEMA IF NOT EXISTS billing;

GRANT ALL PRIVILEGES ON SCHEMA media TO streamcms;
GRANT ALL PRIVILEGES ON SCHEMA cms TO streamcms;
GRANT ALL PRIVILEGES ON SCHEMA live TO streamcms;
GRANT ALL PRIVILEGES ON SCHEMA billing TO streamcms;
