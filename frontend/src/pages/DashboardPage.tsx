import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { Icon, type IconName } from '../components/Icon';
import { Alert, Avatar, Skeleton, StatusBadge, formatDate } from '../components/ui';
import { useAuth } from '../stores/AuthContext';
import type { Employee, EmployeeMetrics } from '../types';

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function StatCard({ label, value, icon, tint }: { label: string; value: number; icon: IconName; tint: string }) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span className={`flex h-9 w-9 items-center justify-center rounded-lg ${tint}`}>
          <Icon name={icon} className="h-[18px] w-[18px]" />
        </span>
      </div>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function DashboardPage() {
  const { username } = useAuth();
  const [metrics, setMetrics] = useState<EmployeeMetrics | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.get<EmployeeMetrics>('/employees/metrics'), api.get<Employee[]>('/employees')])
      .then(([metricsRes, employeesRes]) => {
        setMetrics(metricsRes.data);
        setEmployees(employeesRes.data);
      })
      .catch(() => setError('Unable to load dashboard data. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  const departments = useMemo(() => {
    const counts = new Map<string, number>();
    employees.forEach((e) => {
      const name = e.departmentName || 'Unassigned';
      counts.set(name, (counts.get(name) ?? 0) + 1);
    });
    return [...counts.entries()].sort((a, b) => b[1] - a[1]);
  }, [employees]);

  const recentHires = useMemo(
    () => [...employees].sort((a, b) => b.hireDate.localeCompare(a.hireDate)).slice(0, 5),
    [employees]
  );

  const maxDeptCount = departments[0]?.[1] ?? 1;

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm text-slate-500">{new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</p>
          <h1 className="mt-1 text-2xl font-semibold text-slate-900 sm:text-3xl">
            {greeting()}, <span className="capitalize">{username}</span>
          </h1>
        </div>
        <Link to="/employees" className="btn-primary self-start sm:self-auto">
          View employees <Icon name="arrowRight" className="h-4 w-4" />
        </Link>
      </div>

      {error && <Alert>{error}</Alert>}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {loading || !metrics ? (
          [0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-[116px] rounded-2xl" />)
        ) : (
          <>
            <StatCard label="Total employees" value={metrics.totalEmployees} icon="users" tint="bg-brand-50 text-brand-600" />
            <StatCard label="Active employees" value={metrics.activeEmployees} icon="userCheck" tint="bg-emerald-50 text-emerald-600" />
            <StatCard label="Departments" value={metrics.departments} icon="building" tint="bg-amber-50 text-amber-600" />
            <StatCard label="Hired in last 30 days" value={metrics.recentHires} icon="sparkles" tint="bg-violet-50 text-violet-600" />
          </>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <section className="card p-6 lg:col-span-3">
          <h2 className="font-semibold text-slate-900">Headcount by department</h2>
          <p className="text-sm text-slate-500">How your team is distributed.</p>
          <div className="mt-6 space-y-4">
            {loading ? (
              [0, 1, 2].map((i) => <Skeleton key={i} className="h-8" />)
            ) : departments.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-500">No employees yet.</p>
            ) : (
              departments.map(([name, count]) => (
                <div key={name}>
                  <div className="mb-1.5 flex justify-between text-sm">
                    <span className="font-medium text-slate-700">{name}</span>
                    <span className="text-slate-500">{count}</span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                    <div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${(count / maxDeptCount) * 100}%` }} />
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="card p-6 lg:col-span-2">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-slate-900">Recent hires</h2>
              <p className="text-sm text-slate-500">Newest team members.</p>
            </div>
            <Link to="/employees" className="text-sm font-medium text-brand-600 hover:text-brand-700">See all</Link>
          </div>
          <ul className="mt-4 divide-y divide-slate-100">
            {loading ? (
              [0, 1, 2].map((i) => <li key={i} className="py-3"><Skeleton className="h-10" /></li>)
            ) : recentHires.length === 0 ? (
              <li className="py-8 text-center text-sm text-slate-500">No employees yet.</li>
            ) : (
              recentHires.map((e) => (
                <li key={e.id} className="flex items-center gap-3 py-3">
                  <Avatar firstName={e.firstName} lastName={e.lastName} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-slate-900">{e.firstName} {e.lastName}</p>
                    <p className="truncate text-xs text-slate-500">{e.position} · {formatDate(e.hireDate)}</p>
                  </div>
                  <StatusBadge status={e.status} />
                </li>
              ))
            )}
          </ul>
        </section>
      </div>
    </div>
  );
}

export default DashboardPage;
