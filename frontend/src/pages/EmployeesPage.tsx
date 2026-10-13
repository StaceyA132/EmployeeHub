import { FormEvent, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import api from '../api';
import { Icon } from '../components/Icon';
import { Alert, Avatar, Modal, STATUS_OPTIONS, Skeleton, StatusBadge, formatDate, formatSalary } from '../components/ui';
import { useAuth } from '../stores/AuthContext';
import type { Employee } from '../types';

const emptyEmployee: Employee = {
  firstName: '',
  lastName: '',
  email: '',
  position: '',
  salary: null,
  hireDate: new Date().toISOString().slice(0, 10),
  status: 'Active',
  departmentName: '',
};

// Show the backend's message (e.g. "email must not be blank") when there is one
const describeError = (err: unknown, fallback: string) =>
  (axios.isAxiosError(err) && err.response?.data?.message) || fallback;

function EmployeeForm({
  initial,
  departments,
  onSave,
  onCancel,
}: {
  initial: Employee;
  departments: string[];
  onSave: (employee: Employee) => Promise<void>;
  onCancel: () => void;
}) {
  const [form, setForm] = useState<Employee>(initial);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const set = <K extends keyof Employee>(key: K, value: Employee[K]) => setForm((f) => ({ ...f, [key]: value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await onSave(form);
    } catch (err) {
      setError(describeError(err, 'Unable to save employee.'));
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit}>
      <div className="grid gap-4 px-6 py-5 sm:grid-cols-2">
        <div>
          <label htmlFor="firstName" className="label">First name</label>
          <input id="firstName" className="input" value={form.firstName} onChange={(e) => set('firstName', e.target.value)} required autoFocus />
        </div>
        <div>
          <label htmlFor="lastName" className="label">Last name</label>
          <input id="lastName" className="input" value={form.lastName} onChange={(e) => set('lastName', e.target.value)} required />
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="email" className="label">Email</label>
          <input id="email" type="email" className="input" value={form.email} onChange={(e) => set('email', e.target.value)} required />
        </div>
        <div>
          <label htmlFor="position" className="label">Position</label>
          <input id="position" className="input" value={form.position} onChange={(e) => set('position', e.target.value)} required />
        </div>
        <div>
          <label htmlFor="department" className="label">Department</label>
          <input id="department" className="input" list="department-options" value={form.departmentName} onChange={(e) => set('departmentName', e.target.value)} />
          <datalist id="department-options">
            {departments.map((d) => <option key={d} value={d} />)}
          </datalist>
        </div>
        <div>
          <label htmlFor="salary" className="label">Annual salary (USD)</label>
          <input
            id="salary"
            type="number"
            min={0}
            step="any"
            className="input"
            value={form.salary ?? ''}
            onChange={(e) => set('salary', e.target.value === '' ? null : Number(e.target.value))}
            required
          />
        </div>
        <div>
          <label htmlFor="hireDate" className="label">Hire date</label>
          <input id="hireDate" type="date" className="input" value={form.hireDate} onChange={(e) => set('hireDate', e.target.value)} required />
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="status" className="label">Status</label>
          <select id="status" className="input" value={form.status} onChange={(e) => set('status', e.target.value)}>
            {/* Keep any legacy value (e.g. "ACTIVE") selectable so editing doesn't silently change it */}
            {!STATUS_OPTIONS.includes(form.status as (typeof STATUS_OPTIONS)[number]) && <option value={form.status}>{form.status}</option>}
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
        {error && <div className="sm:col-span-2"><Alert>{error}</Alert></div>}
      </div>
      <div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50/60 px-6 py-4">
        <button type="button" onClick={onCancel} className="btn-secondary">Cancel</button>
        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? 'Saving…' : initial.id ? 'Save changes' : 'Add employee'}
        </button>
      </div>
    </form>
  );
}

function EmployeesPage() {
  const { isAdmin, isManager } = useAuth();
  const canEdit = isAdmin || isManager;

  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Employee | null>(null);
  const [deleting, setDeleting] = useState<Employee | null>(null);
  const [deleteError, setDeleteError] = useState('');

  const [search, setSearch] = useState('');
  const [department, setDepartment] = useState('');
  const [status, setStatus] = useState('');

  const fetchEmployees = () => {
    setError('');
    return api.get<Employee[]>('/employees')
      .then((response) => setEmployees(response.data))
      .catch(() => setError('Unable to load employee data.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  const departments = useMemo(
    () => [...new Set(employees.map((e) => e.departmentName).filter(Boolean))].sort(),
    [employees]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return employees
      .filter((e) => !department || e.departmentName === department)
      .filter((e) => !status || e.status?.toLowerCase() === status.toLowerCase())
      .filter((e) => !q || `${e.firstName} ${e.lastName} ${e.email} ${e.position}`.toLowerCase().includes(q))
      .sort((a, b) => `${a.lastName}${a.firstName}`.localeCompare(`${b.lastName}${b.firstName}`));
  }, [employees, search, department, status]);

  const hasFilters = search || department || status;

  const save = async (employee: Employee) => {
    if (employee.id) {
      await api.put(`/employees/${employee.id}`, employee);
    } else {
      await api.post('/employees', employee);
    }
    setEditing(null);
    await fetchEmployees();
  };

  const confirmDelete = async () => {
    if (!deleting?.id) return;
    try {
      await api.delete(`/employees/${deleting.id}`);
      setDeleting(null);
      await fetchEmployees();
    } catch (err) {
      setDeleteError(describeError(err, 'Unable to delete employee.'));
    }
  };

  // Less important columns drop off on smaller screens so the table never needs sideways scrolling on phones
  const columns = [
    { label: 'Employee', className: '' },
    { label: 'Department', className: 'hidden md:table-cell' },
    { label: 'Position', className: 'hidden xl:table-cell' },
    { label: 'Status', className: 'hidden sm:table-cell' },
    { label: 'Hire date', className: 'hidden 2xl:table-cell' },
    ...(canEdit ? [{ label: 'Salary', className: 'hidden md:table-cell' }, { label: '', className: '' }] : []),
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900 sm:text-3xl">Employees</h1>
          <p className="mt-1 text-sm text-slate-500">
            {loading ? 'Loading…' : `${employees.length} ${employees.length === 1 ? 'person' : 'people'} across ${departments.length} ${departments.length === 1 ? 'department' : 'departments'}`}
          </p>
        </div>
        {canEdit && (
          <button onClick={() => setEditing(emptyEmployee)} className="btn-primary self-start sm:self-auto">
            <Icon name="plus" className="h-4 w-4" /> Add employee
          </button>
        )}
      </div>

      {error && <Alert>{error}</Alert>}

      <div className="card overflow-hidden">
        {/* Toolbar */}
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 md:flex-row md:items-center">
          <div className="relative flex-1">
            <Icon name="search" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              className="input pl-9"
              placeholder="Search by name, email, or position"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search employees"
            />
          </div>
          <div className="flex gap-3">
            <select className="input md:w-44" value={department} onChange={(e) => setDepartment(e.target.value)} aria-label="Filter by department">
              <option value="">All departments</option>
              {departments.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
            <select className="input md:w-36" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filter by status">
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/60">
                {columns.map((c, i) => (
                  <th key={i} className={`whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 first:pl-6 last:pr-6 ${c.className}`}>
                    {c.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                [0, 1, 2, 3].map((i) => (
                  <tr key={i}>
                    <td colSpan={columns.length} className="px-6 py-3"><Skeleton className="h-10" /></td>
                  </tr>
                ))
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={columns.length} className="px-6 py-16 text-center">
                    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                      <Icon name={hasFilters ? 'search' : 'users'} />
                    </div>
                    <p className="mt-3 font-medium text-slate-900">{hasFilters ? 'No matching employees' : 'No employees yet'}</p>
                    <p className="mt-1 text-slate-500">
                      {hasFilters ? 'Try a different search or clear the filters.' : canEdit ? 'Add your first employee to get started.' : 'Employees will appear here once added.'}
                    </p>
                    {hasFilters && (
                      <button className="btn-secondary mt-4" onClick={() => { setSearch(''); setDepartment(''); setStatus(''); }}>
                        Clear filters
                      </button>
                    )}
                  </td>
                </tr>
              ) : (
                filtered.map((e) => (
                  <tr key={e.id} className="group transition hover:bg-slate-50/80">
                    <td className="py-3 pl-6 pr-4">
                      <div className="flex items-center gap-3">
                        <Avatar firstName={e.firstName} lastName={e.lastName} />
                        <div className="min-w-0 max-w-[11rem] sm:max-w-[14rem] lg:max-w-[11rem] xl:max-w-[14rem] 2xl:max-w-[18rem]">
                          <p className="truncate font-medium text-slate-900">{e.firstName} {e.lastName}</p>
                          <p className="truncate text-slate-500 xl:hidden">{e.position}</p>
                          <div className="mt-1 sm:hidden"><StatusBadge status={e.status} /></div>
                          <p className="hidden truncate text-slate-500 xl:block">{e.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="hidden px-4 py-3 text-slate-700 md:table-cell">{e.departmentName || '—'}</td>
                    <td className="hidden px-4 py-3 text-slate-700 xl:table-cell">{e.position}</td>
                    <td className="hidden whitespace-nowrap px-4 py-3 sm:table-cell"><StatusBadge status={e.status} /></td>
                    <td className="hidden whitespace-nowrap px-4 py-3 text-slate-700 2xl:table-cell">{formatDate(e.hireDate)}</td>
                    {canEdit && (
                      <>
                        <td className="hidden whitespace-nowrap px-4 py-3 tabular-nums text-slate-700 md:table-cell">{formatSalary(e.salary)}</td>
                        <td className="whitespace-nowrap py-3 pl-2 pr-4 text-right sm:pr-6">
                          <div className="flex justify-end gap-1 opacity-100 transition md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100">
                            <button onClick={() => setEditing(e)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900" title="Edit" aria-label={`Edit ${e.firstName} ${e.lastName}`}>
                              <Icon name="pencil" className="h-4 w-4" />
                            </button>
                            <button onClick={() => { setDeleteError(''); setDeleting(e); }} className="rounded-lg p-2 text-slate-500 hover:bg-red-50 hover:text-red-600" title="Delete" aria-label={`Delete ${e.firstName} ${e.lastName}`}>
                              <Icon name="trash" className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && filtered.length > 0 && hasFilters && (
          <div className="border-t border-slate-100 px-6 py-3 text-xs text-slate-500">
            Showing {filtered.length} of {employees.length}
          </div>
        )}
      </div>

      {editing && (
        <Modal title={editing.id ? 'Edit employee' : 'Add employee'} onClose={() => setEditing(null)}>
          <EmployeeForm initial={editing} departments={departments} onSave={save} onCancel={() => setEditing(null)} />
        </Modal>
      )}

      {deleting && (
        <Modal title="Delete employee" onClose={() => setDeleting(null)}>
          <div className="space-y-4 px-6 py-5">
            <p className="text-sm text-slate-600">
              Are you sure you want to delete <span className="font-medium text-slate-900">{deleting.firstName} {deleting.lastName}</span>? This can't be undone.
            </p>
            {deleteError && <Alert>{deleteError}</Alert>}
          </div>
          <div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50/60 px-6 py-4">
            <button onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
            <button onClick={confirmDelete} className="btn-danger">Delete</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

export default EmployeesPage;
