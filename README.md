# Sunrise Dental Clinic System

Staff-only appointment, patient, and billing system for Sunrise Dental Clinic, Colombo.

This is a Java Maven web application. There is one shared login for two roles:

- **ADMIN** manages staff accounts, the clinic dentist list, and can view clinic records
- **STAFF** registers patients, appointments, and bills

Patients and dentists do not log in. There is no public registration page and no patient portal.

## Initial administrator account

The first time the application starts, it creates only this administrator account:

| Field | Value |
| --- | --- |
| Username | `admin` |
| Password | `Admin#Sunrise26` |

Log in as this administrator, then register Staff accounts from **Staff management**. Do not seed demo staff, patients, appointments, or bills.

## Requirements

- Java 17
- Maven 3.8+
- MySQL 8

## Database setup

1. Start MySQL.
2. Create `src/main/resources/application.local.properties` if your MySQL user is not `root` with a blank password:

```
db.user=root
db.password=YOUR_PASSWORD
```

3. The application creates the `sunrise_clinic` database and tables automatically on startup.
4. You can also run `database/schema.sql` manually in MySQL if you prefer.

The schema stores:

- `users` for Admin and Staff accounts
- `dentists` for the clinic dentist list used when booking appointments
- `patients`
- `appointments` with a unique appointment number and a foreign key to the patient
- `bills` with one bill per appointment

## Run the application

```
mvn test
mvn exec:java
```

Then open http://localhost:8080

The shared login page is `/login.html`. After authentication the system opens the matching workspace.

Alternatively:

```
mvn jetty:run
```

## First-use steps

1. Sign in as `admin` / `Admin#Sunrise26`.
2. Open **Staff** and register a staff member with a username and password.
3. Open **Dentists** and add the clinic dentists. Dentists do not receive login accounts.
4. Log out.
5. Sign in as that staff member.
6. Register patients, create appointments by choosing a dentist from the clinic list, search by appointment number, calculate bills, and print receipts.

## Architecture

The project uses a layered design:

- **Model** — `UserAccount`, `Dentist`, `Patient`, `Appointment`, `Bill`
- **DAO** — JDBC repositories created by `DaoFactory`
- **Service** — authentication, staff, dentist, patient, appointment, billing, and reports
- **Web** — JSON servlets and an authentication filter
- **UI** — HTML/CSS/JavaScript staff workspace

SQL stays in the DAO layer. Business rules stay in services. HTML pages do not contain business logic.

Role checks are enforced in the service layer. Hiding a menu item is not enough: a Staff user who calls `/api/staff` or tries to add a dentist receives HTTP 403.

## Design patterns used

- **Singleton** — `AppSettings` and `DatabaseManager` for configuration and database connections
- **Factory** — `DaoFactory` and `BillingFormulaFactory`
- **DAO / Repository** — data access interfaces with JDBC implementations
- **Service layer** — use-case logic and authorization
- **Strategy** — `BillingFormula` for treatment cost plus consultation fee
- **Intercepting filter** — `AuthFilter` for session authentication

## Clinic assumptions

- Clinic hours are 08:00 to 17:30, Monday to Saturday, in 30-minute slots
- Sundays are closed
- Appointment numbers are assigned by the system as `APT-YYYYMMDD-0001`
- A dentist cannot have two active appointments at the same date and time
- A patient cannot have two active appointments at the same date and time
- Bill total = treatment cost + consultation fee (LKR 1,500.00)
- Staff accounts are blocked rather than deleted

## Tests

```
mvn test
```

Automated tests cover authentication, authorization, staff registration and blocking, patient records, appointment booking and search, billing calculation, and validation rules. See `docs/TEST-PLAN.md`.

## GitHub

This project continues the existing repository history at [sunrise-dental-clinic-system](https://github.com/Tarini13491/sunrise-dental-clinic-system).
