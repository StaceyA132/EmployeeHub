"""
Automated API test suite for EmployeeHub.

Setup:
    pip install pytest requests --break-system-packages

Run the app first:
    docker compose up --build
    (backend available at http://localhost:8080, per README)

Run tests:
    pytest test_employeehub_api.py -v

Notes on assumptions (verify against real behavior and adjust if needed):
- Seeded users from README: admin/admin123 (ROLE_ADMIN), manager/manager123 (ROLE_MANAGER)
- GET endpoints have no @PreAuthorize in the controller, so whether they require
  *any* authenticated user or are fully open depends on SecurityConfig (not seen).
  Tests below assume authentication is required for all /api/employees/** routes;
  if GET actually works without a token, that's worth a quick manual check.
- EmployeeDto has no visible @Valid/@NotNull annotations, so "missing field" tests
  assume the service layer may or may not reject them — these tests double as a way
  to discover whether that validation exists at all, which is itself useful QA info.
- getEmployeeById on a bad id likely calls .orElseThrow() somewhere with no custom
  @ExceptionHandler visible, so a 404 test may actually return 500. That mismatch,
  if found, is a real bug worth writing up rather than a broken test.
"""

import uuid
import pytest
import requests

BASE_URL = "http://localhost:8080"


# ---------- Fixtures ----------

@pytest.fixture(scope="session")
def admin_token():
    resp = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": "admin", "password": "admin123"
    })
    assert resp.status_code == 200, f"Admin login failed: {resp.status_code} {resp.text}"
    return resp.json()["token"]


@pytest.fixture(scope="session")
def auth_headers(admin_token):
    return {"Authorization": f"Bearer {admin_token}"}


@pytest.fixture
def new_employee_payload():
    unique = uuid.uuid4().hex[:8]
    return {
        "firstName": "Test",
        "lastName": f"User{unique}",
        "email": f"test.user.{unique}@example.com",
        "position": "QA Engineer",
        "salary": 65000,
        "hireDate": "2026-01-15",
        "status": "ACTIVE",
        "departmentName": "Engineering",
    }


@pytest.fixture
def created_employee(auth_headers, new_employee_payload):
    """Creates an employee, yields its response body, then cleans up."""
    resp = requests.post(f"{BASE_URL}/api/employees", json=new_employee_payload, headers=auth_headers)
    assert resp.status_code == 200, f"Setup create failed: {resp.status_code} {resp.text}"
    employee = resp.json()
    yield employee
    requests.delete(f"{BASE_URL}/api/employees/{employee['id']}", headers=auth_headers)


# ---------- Auth ----------

def test_login_success_admin():
    resp = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": "admin", "password": "admin123"
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["token"]
    assert body["username"] == "admin"


def test_login_invalid_password():
    resp = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": "admin", "password": "wrongpassword"
    })
    assert resp.status_code in (401, 403), f"Expected auth failure, got {resp.status_code}"


def test_login_nonexistent_user():
    resp = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": "no_such_user_xyz", "password": "whatever"
    })
    assert resp.status_code in (401, 403, 404)


def test_register_duplicate_username_fails():
    resp = requests.post(f"{BASE_URL}/api/auth/register", json={
        "username": "admin", "password": "irrelevant123"
    })
    assert resp.status_code == 400


def test_register_new_user_success():
    unique_username = f"qa_user_{uuid.uuid4().hex[:8]}"
    resp = requests.post(f"{BASE_URL}/api/auth/register", json={
        "username": unique_username, "password": "TestPass123"
    })
    assert resp.status_code == 200
    assert resp.json()["token"]


# ---------- GET ----------

def test_get_all_employees_with_auth(auth_headers):
    resp = requests.get(f"{BASE_URL}/api/employees", headers=auth_headers)
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


def test_get_all_employees_without_auth():
    resp = requests.get(f"{BASE_URL}/api/employees")
    assert resp.status_code in (401, 403), (
        f"Expected unauthenticated GET to be rejected, got {resp.status_code}. "
        "If this fails, the endpoint may be unintentionally public."
    )


def test_get_employee_metrics(auth_headers):
    resp = requests.get(f"{BASE_URL}/api/employees/metrics", headers=auth_headers)
    assert resp.status_code == 200


def test_get_employee_by_id_success(auth_headers, created_employee):
    resp = requests.get(f"{BASE_URL}/api/employees/{created_employee['id']}", headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["email"] == created_employee["email"]


def test_get_employee_by_id_not_found(auth_headers):
    resp = requests.get(f"{BASE_URL}/api/employees/999999999", headers=auth_headers)
    assert resp.status_code == 404, (
        f"Expected 404 for missing employee, got {resp.status_code}. "
        "A 500 here likely means there's no exception handler for orElseThrow()."
    )


# ---------- POST (create) ----------

def test_create_employee_success(auth_headers, new_employee_payload):
    resp = requests.post(f"{BASE_URL}/api/employees", json=new_employee_payload, headers=auth_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["email"] == new_employee_payload["email"]
    assert body["id"] is not None
    # cleanup
    requests.delete(f"{BASE_URL}/api/employees/{body['id']}", headers=auth_headers)


def test_create_employee_missing_email(auth_headers, new_employee_payload):
    payload = dict(new_employee_payload)
    del payload["email"]
    resp = requests.post(f"{BASE_URL}/api/employees", json=payload, headers=auth_headers)
    assert resp.status_code in (200, 400), f"Unexpected status: {resp.status_code}"
    if resp.status_code == 200:
        # No validation currently rejects a missing email — flag for review
        employee_id = resp.json()["id"]
        requests.delete(f"{BASE_URL}/api/employees/{employee_id}", headers=auth_headers)


def test_create_employee_without_auth(new_employee_payload):
    resp = requests.post(f"{BASE_URL}/api/employees", json=new_employee_payload)
    assert resp.status_code in (401, 403)


# ---------- PUT (update) ----------

def test_update_employee_success(auth_headers, created_employee):
    updated = dict(created_employee)
    updated["position"] = "Senior QA Engineer"
    resp = requests.put(f"{BASE_URL}/api/employees/{created_employee['id']}", json=updated, headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["position"] == "Senior QA Engineer"


def test_update_nonexistent_employee(auth_headers, new_employee_payload):
    resp = requests.put(f"{BASE_URL}/api/employees/999999999", json=new_employee_payload, headers=auth_headers)
    assert resp.status_code == 404, (
        f"Expected 404 for updating a nonexistent employee, got {resp.status_code}."
    )


# ---------- DELETE ----------

def test_delete_employee_success(auth_headers, new_employee_payload):
    create_resp = requests.post(f"{BASE_URL}/api/employees", json=new_employee_payload, headers=auth_headers)
    employee_id = create_resp.json()["id"]

    delete_resp = requests.delete(f"{BASE_URL}/api/employees/{employee_id}", headers=auth_headers)
    assert delete_resp.status_code == 204

    get_resp = requests.get(f"{BASE_URL}/api/employees/{employee_id}", headers=auth_headers)
    assert get_resp.status_code == 404


def test_delete_already_deleted_employee(auth_headers, new_employee_payload):
    create_resp = requests.post(f"{BASE_URL}/api/employees", json=new_employee_payload, headers=auth_headers)
    employee_id = create_resp.json()["id"]
    requests.delete(f"{BASE_URL}/api/employees/{employee_id}", headers=auth_headers)

    second_delete = requests.delete(f"{BASE_URL}/api/employees/{employee_id}", headers=auth_headers)
    assert second_delete.status_code == 404, (
        f"Expected 404 deleting an already-deleted employee, got {second_delete.status_code}."
    )