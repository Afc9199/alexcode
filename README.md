# Employee Management System (Spring Boot + MongoDB)
Final Year Project (FYP)
A modern Employee Management System built with Spring Boot 3.4, MongoDB

# Key Features
- Role-Based Access Control (RBAC): Distinct workflows and dashboards for ADMIN and EMPLOYEE roles.
- Automated HR Workflows: Real-time attendance tracking, leave application/approval, and dynamic payroll processing.

#Image
1. Admin - View Employee
  <img width="1901" height="942" alt="image" src="https://github.com/user-attachments/assets/c17f1964-19d8-4ddd-8336-e6b6af59fd83" />

2. Admin - View Employee Attendance List
<img width="1881" height="890" alt="image" src="https://github.com/user-attachments/assets/2250e54e-597e-4c05-bbb1-5561a6c7a5fe" />

3. Admin - View Employee Leave
<img width="1887" height="867" alt="image" src="https://github.com/user-attachments/assets/1a716949-8656-434e-9067-b36dec6481f3" />

4. Admin - Generate Employee Payslip
<img width="1876" height="580" alt="image" src="https://github.com/user-attachments/assets/a7b402ed-25af-40fa-9f25-d14d431542c7" />

5. Employee - Take Attendance
<img width="1882" height="854" alt="image" src="https://github.com/user-attachments/assets/3624fdd5-5921-4a3f-a396-3227d6b0f8b3" />

6. Employee - View Leave Balance
<img width="1886" height="909" alt="image" src="https://github.com/user-attachments/assets/0debe328-b2f3-40dd-b554-14a96c08203a" />
 

## Tech stack
- Backend Framework: Spring Boot 3.4.x (Java 21, Maven)
- Database: MongoDB
- Jakarta Bean Validation for DTO/entity validation
- Spring Boot Actuator for health/info endpoints

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
   - Execute the following commands in the project root directory
   - ./mvnw spring-boot:run

## Authentication quick start
- Default accounts are created automatically:
  - Admin: `admin` / `admin123`
  - Employee: `employee` / `employee123`
