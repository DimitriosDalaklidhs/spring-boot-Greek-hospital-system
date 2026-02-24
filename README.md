# Greek Hospital Management REST API

A secure, production-structured REST API for managing hospital patients, hospitalizations, and medical tests. Built with **Java 17**, **Spring Boot 3**, and **MySQL**.

---

## Features

- **JWT Authentication** — stateless token-based auth with role extraction
- **Role-Based Access Control** — fine-grained endpoint permissions per HTTP method for `DOCTOR`, `CLERK`, and `ADMIN` roles
- **Patient Management** — register and look up patients by ΑΜΚΑ (Greek social security number)
- **ΑΜΚΑ Validation** — full Luhn algorithm implementation with birth-date extraction
- **Hospitalization Workflow** — admit patients, assign hospitals, discharge with date tracking
- **Medical Tests** — record and retrieve patient test history with cost tracking
- **Atomic Clerk Operations** — patient registration + hospitalization in a single transactional request
- **Structured Error Handling** — consistent JSON error responses across all failure types

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (jjwt) |
| Database | MySQL |
| DB Access | Spring JdbcTemplate |
| Connection Pool | HikariCP |
| Containerization | Docker |
| Build Tool | Maven |

---

## Architecture

```
com.example.demo
├── auth/
│   ├── JwtUtil.java               # Token generation & validation
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
- Java 17+
- Docker & Docker Compose (recommended)
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

### Run with Docker

```bash
docker build -t hospital-api .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/hospital_db \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  -e JWT_SECRET=your-secret-key-min-32-chars \
  hospital-api
```

### Run Locally

```bash
./mvnw spring-boot:run
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

## Author

**Dimitrios Dalaklidis**  
[LinkedIn](https://linkedin.com/in/dimitris-dalaklidis) · [GitHub](https://github.com/DimitriosDalaklidhs)
