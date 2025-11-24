# 📄 Statement Storage Service

A lightweight Spring Boot service for storing PDF bank statements in memory and generating secure, time-limited download tokens.
Includes a very simple UI for uploading and downloading statements in the browser.

Perfect for demos, PoCs, and testing secure download workflows — no external databases or storage systems required.

# 🚀 Features

- Upload monthly statements as PDF
- Auto-extract customerId, year, and month from file name
- Format: statement_customerId_year_month.pdf
- Store statements in memory (lost on restart)
- Generate temporary download tokens
- Token valid for a few minutes
- Public endpoint to download PDF via token
- Paginated listing per customer
- Basic HTML/JS UI at http://localhost:8080/statements
- Fully Dockerized (no local JDK required)

# 📦 Requirements

- Rancher Desktop or Docker Desktop installed

That’s it. You do not need Java or Maven installed.

# 🛠️ Running Locally

Clone the repository:

```bash
git clone https://github.com/EmpiePie/statement-storage-service.git
```
```bash
cd statement-service
```

Build and start the service using Docker Compose:

```bash
docker compose up --build
```

Should you wish to destroy the data and start afresh, you can use the following command:

```bash
docker compose down
```

## Services will start at:

Statement Service API   -	http://localhost:8080

Statement Service UI    -	http://localhost:8080/statements

Swagger API Docs       -	http://localhost:8080/swagger-ui.html



# 📝 Usage
## Uploading Statements
Use the web UI at `http://localhost:8080/statements` to upload PDF statements.
Ensure the file names follow the format: `statement_customerId_year_month.pdf`.

For example: `statement_12345_2023_01.pdf`.

Customer ID will be extracted as `12345`, year as `2023`, and month as `01`.

Use this Customer ID for listing and downloading statements.

## Downloading Statements
After uploading, you can generate a download token for a statement.
Use the token to download the statement via the public endpoint:
`http://localhost:8080/statements/download?token=<token>`.
The token is valid for a limited time (e.g., 5 minutes).

# 📄 API Documentation
The API is documented using Swagger.
Access the Swagger UI at: `http://localhost:8080/swagger-ui.html`.

# 🧪 Testing
You can run the unit and integration tests using Maven.
If you have Maven installed locally, run:
```bash
mvn test
```
Alternatively, you can use the Maven wrapper:
```bash
./mvnw test
```

# 📬 Postman Collection
A Postman collection is provided in the `postman` directory for easy testing of the API endpoints.
Import `statement-api-collection.json` into Postman to get started.

# 📄 Sample Statement
A sample PDF statement file is also included in the `pdf` directory for upload testing.

# 🙋 Support
If you encounter issues, open a GitHub issue or contact the maintainer.

# ⚠️ Disclaimer
This service is intended for demonstration and testing purposes only.
Do not use it in production as it stores statements in memory and does not persist data.