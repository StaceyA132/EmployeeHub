import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../stores/AuthContext';

const stats = [
  { label: 'Total employees', value: '42' },
  { label: 'Departments', value: '5' },
  { label: 'Active positions', value: '8' },
  { label: 'New hires', value: '4' },
];

function DashboardPage() {
  const { logout } = useAuth();

  const quickStats = useMemo(() => stats, []);

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <div>
            <p className="text-sm text-slate-500">Welcome back</p>
            <h1 className="text-2xl font-semibold text-slate-900">EmployeeHub Dashboard</h1>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/employees" className="rounded-xl bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700">
              View Employees
            </Link>
            <button onClick={logout} className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-slate-700 hover:bg-slate-50">
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-6 py-8">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {quickStats.map((item) => (
            <div key={item.label} className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
              <p className="text-sm text-slate-500">{item.label}</p>
              <p className="mt-4 text-3xl font-semibold text-slate-900">{item.value}</p>
            </div>
          ))}
        </div>
        <section className="mt-8 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Workforce insights</h2>
              <p className="text-slate-500">Quick overview of employee activity and personnel trends.</p>
            </div>
            <Link to="/employees" className="rounded-xl bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700">
              Open employee roster
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}

export default DashboardPage;
