import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { Employee } from '../types';
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
