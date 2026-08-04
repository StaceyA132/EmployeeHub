import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import type { Employee } from '../types';
import { useAuth } from '../stores/AuthContext';

function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Employee | null>(null);
  const { logout, isAdmin, isManager } = useAuth();

  const fetchEmployees = () => {
    setLoading(true);
    api.get('/employees')
      .then((response) => setEmployees(response.data))
      .catch(() => setError('Unable to load employee data.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  const startCreate = () => setEditing({ firstName: '', lastName: '', email: '', position: '', salary: 0, hireDate: '', status: 'Active', departmentName: '' });
  const startEdit = (e: Employee) => setEditing(e);
  const cancelEdit = () => setEditing(null);

  const save = async (e: Employee) => {
    try {
      if (e.id) {
        await api.put(`/employees/${e.id}`, e);
      } else {
        await api.post('/employees', e);
      }
      cancelEdit();
      fetchEmployees();
    } catch {
      setError('Unable to save employee.');
    }
  };

  const remove = async (id?: number) => {
    if (!id) return;
    if (!confirm('Delete this employee?')) return;
    try {
      await api.delete(`/employees/${id}`);
      fetchEmployees();
    } catch {
      setError('Unable to delete employee.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <div>
            <p className="text-sm text-slate-500">Employee management</p>
            <h1 className="text-2xl font-semibold text-slate-900">Employee roster</h1>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/" className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-slate-700 hover:bg-slate-50">
              Dashboard
            </Link>
            <button onClick={logout} className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-slate-700 hover:bg-slate-50">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">
        {loading ? (
          <div className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">Loading...</div>
        ) : error ? (
          <div className="rounded-3xl bg-white p-6 text-red-600 shadow-sm ring-1 ring-slate-200">{error}</div>
        ) : (
          <div className="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200">
            <div className="p-6 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">Employee list</h2>
                <p className="mt-1 text-sm text-slate-500">Browse and manage employee profiles.</p>
              </div>
              {(isAdmin || isManager) && (
                <div>
                  <button onClick={startCreate} className="rounded-xl bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700">New Employee</button>
                </div>
              )}
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Name</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Department</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Position</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Hire date</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white">
                  {employees.map((employee) => (
                    <tr key={employee.id}>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-900">{employee.firstName} {employee.lastName}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.departmentName}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.position}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.status}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.hireDate}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        <div className="flex gap-2">
                          {(isAdmin || isManager) && (
                            <>
                              <button onClick={() => startEdit(employee)} className="rounded-md border px-2 py-1 text-sm">Edit</button>
                              <button onClick={() => remove(employee.id)} className="rounded-md border px-2 py-1 text-sm text-red-600">Delete</button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {editing && (
          <div className="mt-6 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <h3 className="text-lg font-semibold">{editing.id ? 'Edit Employee' : 'New Employee'}</h3>
            <div className="grid gap-4 md:grid-cols-2 mt-4">
              <input placeholder="First name" value={editing.firstName} onChange={(e) => setEditing({ ...editing, firstName: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Last name" value={editing.lastName} onChange={(e) => setEditing({ ...editing, lastName: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Email" value={editing.email} onChange={(e) => setEditing({ ...editing, email: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Position" value={editing.position} onChange={(e) => setEditing({ ...editing, position: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Salary" type="number" value={editing.salary} onChange={(e) => setEditing({ ...editing, salary: Number(e.target.value) })} className="border p-2 rounded" />
              <input placeholder="Hire date (YYYY-MM-DD)" value={editing.hireDate} onChange={(e) => setEditing({ ...editing, hireDate: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Status" value={editing.status} onChange={(e) => setEditing({ ...editing, status: e.target.value })} className="border p-2 rounded" />
              <input placeholder="Department" value={editing.departmentName} onChange={(e) => setEditing({ ...editing, departmentName: e.target.value })} className="border p-2 rounded" />
            </div>
            <div className="mt-4 flex gap-2">
              <button onClick={() => save(editing)} className="rounded-xl bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700">Save</button>
              <button onClick={cancelEdit} className="rounded-xl border px-4 py-2">Cancel</button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default EmployeesPage;
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import type { Employee } from '../types.ts';
import { useAuth } from '../stores/AuthContext';

function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { logout } = useAuth();

  useEffect(() => {
    api.get('/employees')
      .then((response) => setEmployees(response.data))
      .catch(() => setError('Unable to load employee data.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <div>
            <p className="text-sm text-slate-500">Employee management</p>
            <h1 className="text-2xl font-semibold text-slate-900">Employee roster</h1>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/" className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-slate-700 hover:bg-slate-50">
              Dashboard
            </Link>
            <button onClick={logout} className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-slate-700 hover:bg-slate-50">
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-6 py-8">
        {loading ? (
          <div className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">Loading...</div>
        ) : error ? (
          <div className="rounded-3xl bg-white p-6 text-red-600 shadow-sm ring-1 ring-slate-200">{error}</div>
        ) : (
          <div className="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200">
            <div className="p-6">
              <h2 className="text-xl font-semibold text-slate-900">Employee list</h2>
              <p className="mt-1 text-sm text-slate-500">Browse and manage employee profiles.</p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Name</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Department</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Position</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Hire date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white">
                  {employees.map((employee) => (
                    <tr key={employee.id}>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-900">
                        {employee.firstName} {employee.lastName}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.departmentName}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.position}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.status}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">{employee.hireDate}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default EmployeesPage;
