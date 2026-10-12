# EmployeeHub

EmployeeHub is a full-stack employee management app with a React + TypeScript frontend and a Spring Boot backend.

It supports CRUD employee records, department management, and JWT-protected API access. The app uses PostgreSQL for data storage and provides a dashboard, employee roster, and secure login.

## Architecture

- `backend/`: Spring Boot REST API
- `frontend/`: React + TypeScript UI with Tailwind CSS
- `docker-compose.yml`: PostgreSQL, backend, and frontend services

## Getting Started

### Configure secrets

The database password and JWT signing secret are read from environment variables, not stored in the code.
Copy the example file and fill in your own values (`.env` is git-ignored):

```bash
cp .env.example .env
# then edit .env, e.g. set JWT_SECRET to the output of: openssl rand -base64 48
```

### Run locally with Docker

```bash
docker compose up --build
```

The backend will be available at `http://localhost:8080` and the frontend at `http://localhost:4173`.

### Default users

- `admin` / `admin123` (full access; can create users)
- `manager` / `manager123` (can create, edit, and delete employees)

New accounts can only be created by an admin via `POST /api/auth/register`, and are read-only (`ROLE_USER`).

### Frontend development

```bash
cd frontend
npm install
npm run dev
```

The dev server runs at `http://localhost:3000`.

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
set -a; source ../.env; set +a   # load DB_PASSWORD and JWT_SECRET
mvn spring-boot:run
```

This expects PostgreSQL on `localhost:5432` (e.g. `docker compose up db`).

### API tests

With the app running:

```bash
pip install pytest requests
pytest tests/ -v
```

## Project structure

- Backend implements layered architecture with controllers, services, repositories, entities, DTOs, and JWT security.
- Frontend includes login, dashboard, and employee listing pages.

## Notes

- PostgreSQL credentials are defined in `docker-compose.yml` and can be customized.
- The backend uses JWT authentication and role-based authorization for protected employee operations.
