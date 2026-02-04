# uoc-reservations-jdbc

Educational Java backend project built from a JDBC template, focused on learning relational databases, JDBC, and clean backend architecture step by step.

This project is part of a learning path where database access is implemented manually (without Spring or JPA) to fully understand how Java applications interact with PostgreSQL and how relational models are used from Java code.

The project currently models a **simple restaurant reservation system**.

---

## Goals

- Understand how JDBC works under the hood
- Practice clean separation of concerns:
  - database connection
  - data access (DAO)
  - domain models
  - application entry point (CLI)
- Learn how to:
  - read data from a relational database
  - insert data safely
  - work with foreign keys and constraints
  - handle missing or invalid data correctly
- Build a solid foundation before moving to frameworks like Spring and JPA

---

## Project Structure

src/main/java/edu/uoc/

- Main.java  
  Application entry point.  
  Simple CLI used to interact with the database.

- db/  
  Database.java  
  Centralized JDBC connection helper.

- model/  
  Customer.java  
  Reservation.java  
  Domain models mapping database tables to Java objects.

- dao/  
  CustomerDao.java  
  ReservationDao.java  
  Data Access Objects containing SQL and JDBC logic.

---

## Database

This project uses **PostgreSQL** with a normalized relational schema.

The database models a restaurant reservation domain with the following entities:

- customers
- reservations
- physical restaurant tables
- reservation–table mapping (many-to-many)

The schema enforces data integrity using:

- primary keys
- foreign keys
- NOT NULL, CHECK, and UNIQUE constraints

Business rules such as availability checks, overlapping reservations, or capacity validation are intentionally not implemented yet and will be introduced progressively.

---

## Configuration (Environment Variables)

Database credentials are not stored in code.

The application expects the following environment variables:

- DB_URL  
  Example: jdbc:postgresql://localhost:5432/uoc_databases

- DB_USER  
  Example: postgres

- DB_PASSWORD  
  Your PostgreSQL password

### IntelliJ

Set them in:  
Run → Edit Configurations → Environment variables

### PowerShell (temporary)

- `$Env:DB_URL="jdbc:postgresql://localhost:5432/uoc_databases"`
- `$Env:DB_USER="postgres"`
- `$Env:DB_PASSWORD="your_password"`

---

## Running the Project

Using the Gradle wrapper:

- `.\gradlew.bat run`

Or run `Main` directly from IntelliJ.

---

## What This Project Covers

Currently implemented features:

- JDBC connection to PostgreSQL
- DAO-based data access layer
- Customer management (list, insert, find by ID)
- Reservation creation and listing
- Use of foreign keys and constraints
- Safe SQL using PreparedStatement
- Explicit mapping from SQL rows to Java objects
- Proper resource management using try-with-resources
- CLI-based interaction for learning and debugging

---

## What Comes Next

Planned next steps:

- Assigning physical tables to reservations
- Availability and capacity logic
- Transactions spanning multiple DAO operations
- Improved CLI flow (find-or-create customer)
- Error handling and validation improvements
- Preparing the backend to serve a web or API layer

---

## Notes

- This project intentionally avoids frameworks (Spring, JPA) at first
- The goal is understanding and correctness, not speed
- All database access is explicit to make behavior visible
- Once the fundamentals are clear, migrating to Spring/JPA becomes much easier

---

## License

Educational use only.
