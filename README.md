# Employee Management System (Spring Boot + MongoDB)

This project skeleton was generated from Spring Initializr with the correct dependencies to build a reactive-ready Employee Management System backed by MongoDB.

## Tech stack
- Spring Boot 3.4.x (Java 21, Maven)
- Spring Web for REST APIs
- Spring Data MongoDB with automatic index creation enabled
- Jakarta Bean Validation for DTO/entity validation
- Spring Boot Actuator for health/info endpoints
- Lombok (optional, disable it if your toolchain lacks support)

## Getting started

1. **Install prerequisites**
   - JDK 21 (Temurin, Oracle, or your preferred distribution)
   - Maven Wrapper already included; alternatively install Maven 3.9+
   - Local MongoDB instance (`mongodb://localhost:27017`) or a managed cluster such as MongoDB Atlas.

2. **Configure MongoDB**
   - Update `src/main/resources/application.yml`:
     ```yaml
     spring:
       data:
         mongodb:
           uri: mongodb://<user>:<password>@<host>:<port>/<database>?authSource=admin
     ```
   - For MongoDB Atlas, enable your IP on the network access list and copy the connection string into `spring.data.mongodb.uri`.

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   or
   ```bash
   ./mvnw clean package
   java -jar target/employee-management-0.0.1-SNAPSHOT.jar
   ```

4. **Health checks**
   - Actuator endpoints are exposed at `http://localhost:8080/actuator/health` and `/actuator/info`.

## Authentication quick start
- Default accounts are created automatically:
  - Admin: `admin` / `admin123`
  - Employee: `employee` / `employee123`
- Use `POST http://localhost:8080/api/auth/login` with body:
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- Successful logins return the user id, username, role (`ADMIN` or `EMPLOYEE`) and a message; failed logins return HTTP 401.

## Dashboard & APIs
- `http://localhost:8080/login.html` → quick login helper (JSON-based)
- `http://localhost:8080/dashboard.html` → lightweight dashboard for employees/admins:
  - Employee mode: update profile, submit attendance/leave, view payroll, read announcements.
  - Admin mode: approve attendance/leave, manage payroll, publish announcements, list users.
- REST endpoints (selected):
  - Employee: `/api/employee/profile/{userId}`, `/api/employee/attendance`, `/api/employee/leave`, `/api/employee/payroll/{userId}`, `/api/employee/announcements`
  - Admin: `/api/admin/attendance`, `/api/admin/leave`, `/api/admin/payroll`, `/api/admin/announcements`, `/api/admin/users`

## AI Copilot (Google Gemini 3 Pro Preview)
- Backend endpoint: `POST /api/employee/chatbot` (requires an authenticated employee session).  
- Frontend workspace: `employee-chatbot.html` with real-time streaming from the same endpoint.
- Configuration:
  ```yaml
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: gemini-3.0-pro-preview-001
  ```
- Set the API key as an environment variable before starting Spring Boot, e.g. on PowerShell:
  ```powershell
  setx GEMINI_API_KEY "AIzaSy..."
  ```
- The assistant only returns answers grounded in MongoDB data (profile, attendance, leave, payroll, KPIs, benefits). If the key is missing the endpoint returns HTTP 503.

## Next steps
- Define domain models under `src/main/java/com/example/employeemanagement`.
- Create repositories via `Spring Data MongoRepository`.
- Build REST controllers and DTOs with validation annotations.
- Add integration tests using the built-in `spring-boot-starter-test` (consider embedding MongoDB via Testcontainers for realism).

