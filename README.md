# jdbc-template

Minimal Java + Gradle template for connecting to PostgreSQL using JDBC.

This project is intended as a learning and starter template.

Database credentials are **not** stored in code and must be provided via
environment variables.

---

## Requirements

- Java JDK installed
- PostgreSQL running locally or remotely
- Gradle Wrapper (included)

---

## Configuration (Environment Variables)

The application reads database configuration from environment variables.

### Required variables

- DB_URL  
  Default: jdbc:postgresql://localhost:5432/uoc_databases

- DB_USER  
  Default: postgres

- DB_PASSWORD  
  No default (must be provided)

---

## Running the application

### Windows (PowerShell)

Environment variables set this way are temporary and apply only to the
current terminal session.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/uoc_databases"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_password_here"
./gradlew run
```

---

### macOS / Linux (bash / zsh)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/uoc_databases"
export DB_USER="postgres"
export DB_PASSWORD="your_password_here"
./gradlew run
```

---

### IntelliJ IDEA

1. Run → Edit Configurations…
2. Select the Application configuration
3. Add Environment variables:
   - DB_URL=jdbc:postgresql://localhost:5432/uoc_databases
   - DB_USER=postgres
   - DB_PASSWORD=your_password_here
4. Apply and run

---

## Notes

- Do NOT commit real credentials to the repository
- Use `.env.example` as a reference for required variables
- The application will fail to connect if DB_PASSWORD is not provided
