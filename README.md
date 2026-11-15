# Student Management System

A simple Spring Boot REST API with role-based access, a plain HTML/JavaScript frontend, and a PostgreSQL database, all run with Docker.

## Roles

| Role    | View students | View teachers | Edit students & teachers | Add / remove students & teachers |
|---------|:-------------:|:-------------:|:------------------------:|:--------------------------------:|
| STUDENT | ✅            |               |                          |                                  |
| TEACHER | ✅            | ✅            | ✅                       |                                  |
| ADMIN   | ✅            | ✅            | ✅                       | ✅                               |

Students and teachers each have an `id` (assigned by the database), a `name` and a `department`.

## Tech stack

- Java 17, Spring Boot 3.5
- Spring Web (REST API), Spring Data JPA, Spring Security (HTTP Basic auth, BCrypt passwords), Bean Validation
- PostgreSQL 16
- Lombok and ModelMapper
- Frontend: one HTML page with vanilla JavaScript, served by Spring Boot
- Docker and Docker Compose

## Run with Docker

Requires [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up --build
```

Then open **http://localhost:8081** and log in with one of the demo accounts:

| Username | Password     | Role    |
|----------|--------------|---------|
| admin    | admin123     | ADMIN   |
| teacher  | teacher123   | TEACHER |
| student  | student123   | STUDENT |

These accounts and a few sample students and teachers are created the first time the app starts with an empty database (see `DataInitializer.java`). The data is kept in the `pgdata` Docker volume. To start again from an empty database:

```bash
docker compose down -v
```

To change the database username or password, copy `.env.example` to `.env` and edit it.

## REST API

Every `/api` request needs HTTP Basic authentication (`Authorization: Basic base64(username:password)`).

| Method | Endpoint             | Allowed roles            |
|--------|----------------------|--------------------------|
| GET    | `/api/auth/me`       | any logged-in user       |
| GET    | `/api/students`      | STUDENT, TEACHER, ADMIN  |
| GET    | `/api/students/{id}` | STUDENT, TEACHER, ADMIN  |
| PUT    | `/api/students/{id}` | TEACHER, ADMIN           |
| POST   | `/api/students`      | ADMIN                    |
| DELETE | `/api/students/{id}` | ADMIN                    |
| GET    | `/api/teachers`      | TEACHER, ADMIN           |
| GET    | `/api/teachers/{id}` | TEACHER, ADMIN           |
| PUT    | `/api/teachers/{id}` | TEACHER, ADMIN           |
| POST   | `/api/teachers`      | ADMIN                    |
| DELETE | `/api/teachers/{id}` | ADMIN                    |

Example:

```bash
curl -u admin:admin123 -X POST http://localhost:8081/api/students \
     -H "Content-Type: application/json" \
     -d '{"name": "Sadia Islam", "department": "CSE"}'
```

Responses: `401` when not logged in or the password is wrong, `403` when the role is not allowed, `400` when a field is blank, and `404` when the id doesn't exist.

## Project structure

```
src/main/java/com/example/sms
├── StudentManagementSystemApplication.java
├── config/       AppConfig (ModelMapper), SecurityConfig (role rules), DataInitializer (demo data)
├── controller/   StudentController, TeacherController, AuthController
├── dto/          StudentDTO, TeacherDTO, UserDTO
├── entity/       Student, Teacher, AppUser, Role
├── exception/    ResourceNotFoundException, GlobalExceptionHandler
├── repository/   StudentRepository, TeacherRepository, UserRepository
└── service/      StudentService, TeacherService, CustomUserDetailsService
src/main/resources
├── application.yml
└── static/       index.html, app.js, style.css (the frontend)
src/test/java/com/example/sms
├── service/      StudentServiceTest, TeacherServiceTest (unit tests)
└── integration/  RoleAccessIntegrationTest (integration tests)
src/test/resources
└── application.yml   (H2 in-memory database for tests)
```

## Tests

| Type | Class | Tests | What it checks |
|---|---|:-:|---|
| Unit | `StudentServiceTest` | 4 | get by id, create ignores client id, update, delete unknown id |
| Unit | `TeacherServiceTest` | 2 | list all, get unknown id |
| Integration | `RoleAccessIntegrationTest` | 3 | 401 without login, student can't see teachers, only admin can add students |

Unit tests mock the repository with Mockito. Integration tests start the whole app against an in-memory H2 database, so PostgreSQL isn't needed:

```bash
./mvnw test        # Windows: mvnw.cmd test
```

GitHub Actions (`.github/workflows/ci.yml`) runs all tests on every push to `main` or `dev` and on every pull request to `main`, then builds the Docker image. Results appear in the **Actions** tab, with a summary table on each run.

## Run without Docker

To run the app outside Docker, start a PostgreSQL server and set `DB_URL`, `DB_USER` and `DB_PASS`. The defaults are `jdbc:postgresql://localhost:5432/smsdb`, `admin` and `admin`. Then run:

```bash
./mvnw spring-boot:run
```

## Notes

- The demo passwords are hard-coded in `DataInitializer.java`. Change them before using the app anywhere other than your own machine.
- HTTP Basic sends the password with every request, so use HTTPS in any real deployment. The frontend keeps the credentials in `sessionStorage` until you log out or close the tab.
- New students and teachers are records only. They don't get login accounts automatically.
