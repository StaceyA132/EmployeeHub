# EmployeeHub

EmployeeHub is a full-stack employee management platform with a React + TypeScript frontend and a Java Spring Boot backend.

## Architecture

- `backend/`: Spring Boot REST API
- `frontend/`: React + TypeScript UI with Tailwind CSS
- `docker-compose.yml`: PostgreSQL, backend, and frontend services

## Getting Started

### Run locally with Docker

```bash
docker compose up --build
```

The backend will be available at `http://localhost:8080` and the frontend at `http://localhost:4173`.

### Default users

- `admin` / `admin123`
- `manager` / `manager123`

### Frontend development

```bash
cd frontend
npm install
npm run dev
```

### Verification

Confirm the backend and frontend build successfully:

```bash
cd backend
mvn -q -DskipTests package

cd ../frontend
npm install
npm run build
```

### Backend development

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

## Project structure

- Backend implements layered architecture with controllers, services, repositories, entities, DTOs, and JWT security.
- Frontend includes login, dashboard, and employee listing pages.

## Notes

- PostgreSQL credentials are defined in `docker-compose.yml` and can be customized.
- The backend uses JWT authentication and role-based authorization for protected employee operations.
