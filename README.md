# uoc-reservations-jdbc

Educational Java backend project built from a JDBC template, focused on learning **relational databases**, **JDBC**, and **clean backend architecture** step by step.

This project is part of a learning path where database access is implemented **manually** (without Spring or JPA) to fully understand how Java applications interact with PostgreSQL.

---

## Goals

- Understand how JDBC works under the hood
- Practice clean separation of concerns:
    - database connection
    - data access (DAO)
    - domain models
    - application entry point
- Learn how to:
    - read data from a database
    - insert data safely
    - handle missing data correctly
- Build a solid foundation before moving to frameworks like Spring and JPA

---

## Project Structure

```
src/main/java/edu/uoc/
├─ Main.java              # Application entry point (CLI)
├─ db/
│  └─ Database.java       # Centralized JDBC connection helper
├─ model/
│  └─ Customer.java       # Domain model (maps to customers table)
└─ dao/
   └─ CustomerDao.java    # Data Access Object (SQL + JDBC)
```

---

## Database

This project uses **PostgreSQL**.

Example table used in the project:

```sql
CREATE TABLE customers (
  customer_id BIGSERIAL PRIMARY KEY,
  full_name   VARCHAR(100) NOT NULL,
  email       VARCHAR(255),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Configuration (Environment Variables)

Database credentials are **not stored in code**.

The application expects the following environment variables:

- `DB_URL`  
  Example: `jdbc:postgresql://localhost:5432/uoc_databases`

- `DB_USER`  
  Example: `postgres`

- `DB_PASSWORD`  
  Your PostgreSQL password

### IntelliJ
Set them in **Run → Edit Configurations → Environment variables**.

### PowerShell (temporary)
```powershell
$Env:DB_URL="jdbc:postgresql://localhost:5432/uoc_databases"
$Env:DB_USER="postgres"
$Env:DB_PASSWORD="your_password"
```

---

## Running the Project

Using Gradle wrapper:

```powershell
.\gradlew.bat run
```

Or run `Main` directly from IntelliJ.

---

## What This Project Covers

Currently implemented features:

- Connect to PostgreSQL using JDBC
- Retrieve all customers (`findAll`)
- Retrieve a customer by ID (`findById`)
- Insert a new customer (`insert`)
- Proper resource management (`try-with-resources`)
- Safe SQL using `PreparedStatement`
- Explicit mapping from SQL rows to Java objects

---

## What Comes Next

Planned next steps:

- Simple CLI menu (list / add / find customers)
- Reservation entity and DAO (foreign keys)
- Transactions
- Error handling improvements
- Preparing the backend to serve a web/API layer

---

## Notes

- This project intentionally avoids frameworks (Spring, JPA) at first.
- The goal is **understanding**, not speed.
- Once the fundamentals are clear, migrating to Spring/JPA becomes much easier.

---

## License

Educational use only.
