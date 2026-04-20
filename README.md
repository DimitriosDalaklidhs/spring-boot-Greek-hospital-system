# Greek Hospital Management REST API

A secure, production-structured REST API for managing hospital patients, hospitalizations, and medical tests. Built with **Java 24**, **Spring Boot 4**, and **MySQL**.

---

## Features

- **JWT Authentication** : stateless token-based auth with role extraction
- **Role-Based Access Control** : fine-grained endpoint permissions per HTTP method for `DOCTOR`, `CLERK`, and `ADMIN` roles
- **Patient Management** : register and look up patients by ΑΜΚΑ (Greek social security number)
- **ΑΜΚΑ Validation** : full Luhn algorithm implementation with birth-date extraction
- **Hospitalization Workflow** : admit patients, assign hospitals, discharge with date tracking
- **Medical Tests** : record and retrieve patient test history with cost tracking
- **Atomic Clerk Operations** : patient registration + hospitalization in a single transactional request
- **Structured Error Handling** : consistent JSON error responses across all failure types

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 4.0.2 |
| Security | Spring Security + JWT (jjwt) |
| Database | MySQL |
| DB Access | Spring JdbcTemplate |
| Cloud Hosting | Railway |
| Database Hosting | Railway MySQL |
| Connection Pool | HikariCP |
| Build Tool | Maven |

---

## Architecture

```
com.example.demo
├── auth/
│   ├── JwtUtil.java                # Token generation & validation
│   ├── JwtAuthFilter.java         # Per-request JWT filter
│   ├── UserDetailsServiceImpl.java
│   └── AuthController.java        # /api/auth/login, /api/auth/register
├── config/
│   └── SecurityConfig.java        # Filter chain, role rules, CORS
├── error/
│   └── GlobalExceptionHandler.java
├── PatientController.java
├── PatientDao.java
├── HospitalizationController.java
├── HospitalizationDao.java
├── PatientTestController.java
├── PatientTestDao.java
├── DoctorService.java             # Core business logic
├── ClerkController.java
├── ClerkService.java              # Atomic registration workflow
├── AmkaValidator.java             # Luhn + birth-date logic
└── Db.java                        # DataSource config + parsing utils

src/test/java/com.example.demo
└── auth/
    ├── AuthControllerTest.java     # 8 tests — login + register flows
    ├── JwtUtilTest.java            # 6 tests — generate, parse, validate, expiry
    └── UserDetailsServiceImplTest.java  # 2 tests — found + not found
```

The project follows a strict **Controller → Service → DAO** layering. Controllers handle HTTP, services own transactions and business rules, DAOs handle SQL.

---

## API Overview

### Auth
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Get JWT token |
| POST | `/api/auth/register` | ADMIN | Create a new user account |

### Patients
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/api/patients/{amka}` | DOCTOR, CLERK | Look up patient by ΑΜΚΑ |
| POST | `/api/patients` | DOCTOR, CLERK | Create patient if not exists |
| GET | `/api/patients/{amka}/tests` | DOCTOR, CLERK | Get full test history |
| POST | `/api/patients/{amka}/tests` | DOCTOR | Record a new medical test |

### Hospitalizations
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/hospitalizations` | DOCTOR, CLERK | Admit a patient |
| PUT | `/api/hospitalizations/{id}/discharge` | DOCTOR | Set discharge date |

### Clerk Workflow
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/clerk/hospitalizations` | CLERK | Register patient + admit in one atomic request |

---

## Getting Started

### Prerequisites
- Java 24+
- MySQL 8+

### Configuration

Set the following in `application.properties` or as environment variables:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_db
spring.datasource.username=your_user
spring.datasource.password=your_password

jwt.secret=your-secret-key-at-least-32-characters-long
jwt.expiration-ms=86400000
```

### Run Locally

```bash
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

---

## Example Requests

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "doctor1", "password": "password123"}'
```

**Register a patient and admit (Clerk workflow)**
```bash
curl -X POST http://localhost:8080/api/clerk/hospitalizations \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amka": "01015012345",
    "firstName": "Γιώργος",
    "lastName": "Παπαδόπουλος",
    "birthDate": "1950-01-01",
    "gender": "M",
    "hospitalId": 1,
    "admissionDate": "2024-03-15"
  }'
```

**Get patient test history**
```bash
curl http://localhost:8080/api/patients/01015012345/tests \
  -H "Authorization: Bearer <token>"
```

---

## ΑΜΚΑ Validation

ΑΜΚΑ (Αριθμός Μητρώου Κοινωνικής Ασφάλισης) is the Greek social security number — 11 digits encoding birth date (DDMMYY) + a sequential number + a Luhn check digit.

The validator (`AmkaValidator.java`) implements:
- Length and digit-only checks
- Full Luhn algorithm (doubling even-position digits from the left)
- Birth-date extraction with century pivot logic

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "code": "NOT_FOUND",
  "message": "Δεν βρέθηκε ασθενής με ΑΜΚΑ: 01015012345",
  "status": 404,
  "timestamp": "2024-03-15T10:30:00Z"
}
```

---

## Notes on the Database Schema

The database uses Greek-language table and column names (e.g. `ασθενεισ`, `νοσηλειεσ_ασθενων`) as this project was built against a university-provided schema modelling the Greek public healthcare system.

---

## Live Deployment

The API is deployed on Railway and publicly accessible:

**Base URL**

https://spring-boot-greek-hospital-system-production.up.railway.app

Example:

POST: https://spring-boot-greek-hospital-system-production.up.railway.app/api/auth/login

**Note:**

This project provides a secured REST API and does not include a browser-based user interface.
Most endpoints require JWT authentication and must be accessed using an API client such as Postman, curl, or a frontend application.

Typical workflow:
1. Authenticate via `POST /api/auth/login`
2. Receive a JWT token
3. Include the token in subsequent requests using the `Authorization: Bearer <token>` header

## Deployment

The application is deployed using **Railway** with automatic GitHub integration.

Pipeline:

GitHub → Railway Build → Cloud Deployment → Public HTTPS Endpoint

Every push to the `main` branch triggers an automatic redeployment.

## Environment variables used in production:

SPRING_DATASOURCE_URL

SPRING_DATASOURCE_USERNAME

SPRING_DATASOURCE_PASSWORD

JWT_SECRET

JWT_EXPIRATION_MS

## Author

**Dimitrios Dalaklidis**
[LinkedIn](https://www.linkedin.com/in/dimitris-dalaklidis-a72838397/) · [GitHub](https://github.com/DimitriosDalaklidhs)