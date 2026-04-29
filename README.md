# Greek Hospital Management REST API

A secure, production structured REST API for managing hospital patients, hospitalizations, and medical tests. Built with **Java 24**, **Spring Boot 3**, and **MySQL**, deployed on Railway with automatic CI/CD from GitHub.

---

## Highlights

- **Atomic clerk workflow** : patient registration + hospitalization in a single transactional request
- **ΑΜΚΑ validation** : full Luhn implementation with birth-date extraction and century pivot logic
- **Per-method role-based access control** : fine-grained authorization (DOCTOR / CLERK / ADMIN) at the HTTP-method level
- **Stateless JWT authentication** with role extraction at the filter layer
- **Strict Controller → Service → DAO layering** with transactions owned by the service tier
- **Live deployment** on Railway with automated redeploys on every push to `main`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (jjwt) |
| Database | MySQL 8 |
| DB Access | Spring JdbcTemplate |
| Connection Pool | HikariCP |
| Build Tool | Maven |
| Hosting | Railway (app + MySQL) |

---

## Domain Context

The schema models the Greek public healthcare system and was built against a university-provided specification. Table and column names are in Greek (e.g. `ασθενεισ`, `νοσηλειεσ_ασθενων`), and patient identity is keyed on **ΑΜΚΑ**, the 11-digit Greek social security number that encodes the holder's birth date.

This domain context shapes two of the more interesting parts of the codebase: the ΑΜΚΑ validator and the clerk admission workflow, both described below.

---

## Architecture
```
The project follows a strict **Controller → Service → DAO** separation. Controllers handle HTTP concerns only, services own transactions and business rules, and DAOs encapsulate SQL.
com.example.demo
├── auth/
│   ├── JwtUtil.java                    → token generation & validation
│   ├── JwtAuthFilter.java              → per-request JWT filter
│   ├── UserDetailsServiceImpl.java
│   └── AuthController.java             → /api/auth/login, /api/auth/register
├── config/
│   └── SecurityConfig.java             → filter chain, role rules, CORS
├── error/
│   └── GlobalExceptionHandler.java     → consistent JSON error envelope
├── patient/
│   ├── PatientController.java
│   └── PatientDao.java
├── hospitalization/
│   ├── HospitalizationController.java
│   └── HospitalizationDao.java
├── test/
│   ├── PatientTestController.java
│   └── PatientTestDao.java
├── doctor/
│   └── DoctorService.java              → core medical-workflow logic
├── clerk/
│   ├── ClerkController.java
│   └── ClerkService.java               → atomic registration + admission
├── validation/
│   └── AmkaValidator.java              → Luhn + birth-date extraction
└── Db.java                             → DataSource config + parsing utils
src/test/java/com.example.demo
└── auth/
├── AuthControllerTest.java         → 8 tests, login + register flows
├── JwtUtilTest.java                → 6 tests, generate/parse/validate/expiry
└── UserDetailsServiceImplTest.java → 2 tests, found + not found
```
---

## Security Model

Authentication is stateless and JWT-based. The `JwtAuthFilter` runs once per request, extracts and validates the token, and populates the `SecurityContext` with the authenticated principal and authorities. Role checks are then enforced declaratively in `SecurityConfig` at the **HTTP-method level** — the same URL can be readable by a CLERK but writable only by a DOCTOR.

| Role | Capabilities |
|---|---|
| `ADMIN` | User account creation |
| `DOCTOR` | Full clinical access — record tests, discharge patients |
| `CLERK` | Front-desk operations — register patients, admit |

This matters because in a hospital setting, a clerk should be able to admit a patient but never record a medical test, and a doctor should be able to discharge a patient but should not be creating user accounts. The matrix below reflects that separation.

---

## API Reference

### Auth
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Obtain JWT token |
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

## Notable Implementation Details

### Atomic clerk admission

In a real hospital, a clerk receiving a new patient performs two logically inseparable actions: create the patient record and admit them. Splitting these into two API calls would leave room for a half-completed admission if the second call failed.

`ClerkService` wraps both operations in a single `@Transactional` boundary. If admission fails, the patient insert is rolled back. The controller exposes this as a single endpoint so the client cannot accidentally produce a partial state.

### ΑΜΚΑ validation

ΑΜΚΑ (Αριθμός Μητρώου Κοινωνικής Ασφάλισης) is an 11-digit identifier: `DDMMYY` + sequential number + Luhn check digit. The validator performs:

- Length and digit-only checks
- Full Luhn algorithm, doubling even position digits from the left, summing digits of the doubled values, comparing against the check digit
- Birth-date extraction with **century pivot logic** : the two digit year is disambiguated by comparing against the current date, since a `01015012345` could mean 1950 or 2050

### Consistent error envelope

`GlobalExceptionHandler` translates every exception path that is, validation failures, missing entities, auth errors, constraint violations. All into a single JSON shape:

```json
{
  "code": "NOT_FOUND",
  "message": "Δεν βρέθηκε ασθενής με ΑΜΚΑ: 01015012345",
  "status": 404,
  "timestamp": "2024-03-15T10:30:00Z"
}
```

Clients can rely on `code` and `status` without parsing free-form messages.

---

## Getting Started

### Prerequisites
- Java 24+
- MySQL 8+
- Maven (or use the bundled wrapper)

### Configuration

Set the following in `application.properties` or as environment variables:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_db
spring.datasource.username=your_user
spring.datasource.password=your_password

jwt.secret=your-secret-key-at-least-32-characters-long
jwt.expiration-ms=86400000
```

### Run locally

```bash
./mvnw spring-boot:run
```

### Run tests

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

**Register a patient and admit (atomic clerk workflow)**
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

## Deployment

The API is deployed on **Railway** with GitHub integration. Every push to `main` triggers an automatic build and redeploy.
GitHub (main) → Railway Build → Cloud Deployment → Public HTTPS Endpoint
**Live base URL**
https://spring-boot-greek-hospital-system-production.up.railway.app
Example:
POST https://spring-boot-greek-hospital-system-production.up.railway.app/api/auth/login
### Production environment variables

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL for the managed MySQL instance |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `JWT_SECRET` | Signing key for issued tokens |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds |

### Note on the API surface

This project ships a secured REST API and does not include a browser-based UI. Most endpoints require JWT authentication and are intended to be consumed from Postman, curl, or a separate frontend.

Typical flow:
1. Authenticate via `POST /api/auth/login`
2. Receive a JWT token
3. Include `Authorization: Bearer <token>` on subsequent requests

---

## Author

**Dimitrios Dalaklidis**
[LinkedIn](https://www.linkedin.com/in/dimitris-dalaklidis-a72838397/) · [GitHub](https://github.com/DimitriosDalaklidhs)
