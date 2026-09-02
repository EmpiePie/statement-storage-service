# 📄 Statement Storage Service

A Spring Boot service for storing PDF bank statements and generating secure, time-limited download tokens.
Includes a simple browser UI for registering, logging in, uploading, and downloading statements.

The stack now runs entirely in Docker: the app, a **Keycloak** identity provider, a **PostgreSQL**
database, and a **MinIO** (S3-compatible) object store — all started with a single command.

# 🚀 Features

- **Authentication via Keycloak (IDP) + JWT** — register/log in through the app; a signed JWT is issued on login and required to call the statement API
- **Per-customer authorization** — a logged-in user may only access statements for their own customer ID
- Upload monthly statements as PDF (filename format: `statement_customerId_year_month.pdf`)
- Auto-extract `customerId`, `year`, and `month` from the file name
- **PDFs stored in a MinIO (mock S3) bucket** — no longer in memory
- **Download tokens persisted in PostgreSQL** — survive restarts; valid for a few minutes
- Public endpoint to download a PDF via a temporary token
- Paginated listing per customer
- HTML/JS UI at http://localhost:8080/statements
- Fully Dockerized (no local JDK or Maven required)

# 📦 Requirements

- Rancher Desktop or Docker Desktop installed

That's it. You do not need Java or Maven installed to run it.

# 🛠️ Running Locally

Build and start everything with Docker Compose:

```bash
docker compose up --build
```

To tear everything down (and wipe the Postgres/MinIO volumes):

```bash
docker compose down -v
```

## Services

| Service                | URL                                   | Notes                              |
|------------------------|---------------------------------------|------------------------------------|
| Statement Service API  | http://localhost:8080                 | Spring Boot app                    |
| Statement Service UI   | http://localhost:8080/statements      | Requires login                     |
| Login / Register       | http://localhost:8080/login           | and `/register`                    |
| Swagger API Docs       | http://localhost:8080/swagger-ui.html |                                    |
| Keycloak Admin Console | http://localhost:8180                 | admin / admin                      |
| MinIO Console          | http://localhost:9001                 | minioadmin / minioadmin            |
| PostgreSQL             | localhost:5432                        | statements / statements (or postgres / postgres) |

# 📝 Usage

## 1. Register & log in
1. Open http://localhost:8080/register and create an account, choosing a **Customer ID** (e.g. `123`).
   This creates a user in Keycloak plus a local profile linking your username to that customer ID.
2. Log in at http://localhost:8080/login. The UI stores your JWT and redirects to the statements page.

## 2. Uploading statements
On the UI, upload a PDF named `statement_<customerId>_<year>_<month>.pdf`
(e.g. `statement_123_2024_10.pdf`). The customer ID in the filename **must match your account's
customer ID**, otherwise the upload is rejected with `403 Forbidden`. Uploaded PDFs are written to
the `statements` bucket in MinIO (browse them at http://localhost:9001).

## 3. Downloading statements
Generate a time-limited download token from the UI, then open the public link:
`http://localhost:8080/api/public/download/<token>`. The token is valid for the configured TTL
(default 5 minutes) and is persisted in PostgreSQL.

## Calling the API directly

```bash
# Log in and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret"}' | jq -r .access_token)

# Call a protected endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/statements/123
```

# 🏗️ Architecture

- **Keycloak** is the identity provider. The app is an OAuth2 **resource server** that validates
  RS256 JWTs against the realm's JWKS. Login uses the OIDC password grant; registration uses the
  Keycloak Admin API. Keycloak's own data is stored in the `keycloak` database in Postgres.
- **PostgreSQL** stores the app's download tokens (`download_token`) and user→customer profiles
  (`user_profile`) in the `statements` database. The app and Keycloak connect as the `statements`
  role. A `postgres` superuser (password `postgres`) is also created by `docker/postgres/init.sql`
  purely so DB tools that default to the `postgres` username can connect without extra config.
- **MinIO** provides an S3-compatible bucket for statement PDFs, accessed via the AWS SDK v2.
- A realm named `statements` and a confidential client `statement-service` are imported into
  Keycloak on startup from `docker/keycloak/realm-export.json`.

# 📄 API Documentation
Swagger UI: `http://localhost:8080/swagger-ui.html`.

# 🧪 Testing
The test suite runs under the `test` profile with an in-memory H2 database, in-memory storage,
and permit-all security — so no containers are needed:

```bash
mvn test
```

# 📬 Postman Collection
A Postman collection is provided in the `postman` directory. Note that statement endpoints now
require an `Authorization: Bearer <token>` header obtained from `/api/auth/login`.

# 📄 Sample Statement
A sample PDF (`pdf/statement_123_2024_10.pdf`) is included for upload testing — register with
customer ID `123` to use it.

# ⚠️ Disclaimer
This service is intended for demonstration and testing purposes only. The Keycloak, MinIO, and
Postgres credentials are hard-coded demo defaults and must not be used in production.
