import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../stores/AuthContext';
import { Icon, type IconName } from './Icon';

const navItems: { to: string; label: string; icon: IconName }[] = [
  { to: '/', label: 'Dashboard', icon: 'dashboard' },
  { to: '/employees', label: 'Employees', icon: 'users' },
];

function roleLabel(isAdmin: boolean, isManager: boolean) {
  if (isAdmin) return 'Administrator';
  if (isManager) return 'Manager';
  return 'Viewer';
}

function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { username, isAdmin, isManager, logout } = useAuth();

  return (
    <div className="flex h-full flex-col bg-slate-900 px-4 py-6 text-slate-300">
      <div className="flex items-center gap-3 px-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-500 text-lg font-bold text-white">E</div>
        <span className="text-lg font-semibold text-white">EmployeeHub</span>
      </div>

      <nav className="mt-10 flex-1 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                isActive ? 'bg-white/10 text-white' : 'hover:bg-white/5 hover:text-white'
              }`
            }
          >
            <Icon name={item.icon} />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-white/10 pt-4">
        <div className="flex items-center gap-3 px-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-sm font-semibold uppercase text-white">
            {username.slice(0, 2)}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-white">{username}</p>
            <p className="text-xs text-slate-400">{roleLabel(isAdmin, isManager)}</p>
          </div>
          <button onClick={logout} className="rounded-lg p-2 hover:bg-white/10 hover:text-white" title="Sign out" aria-label="Sign out">
            <Icon name="logout" />
          </button>
        </div>
      </div>
    </div>
  );
}

function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="min-h-screen">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 lg:block">
        <Sidebar />
      </aside>

      {/* Mobile sidebar drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 animate-fade-in bg-slate-900/50" onClick={() => setMobileOpen(false)} />
          <aside className="relative h-full w-64 animate-pop-in">
            <Sidebar onNavigate={() => setMobileOpen(false)} />
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-slate-200 bg-white/80 px-4 py-3 backdrop-blur lg:hidden">
          <button onClick={() => setMobileOpen(true)} className="rounded-lg p-2 text-slate-600 hover:bg-slate-100" aria-label="Open menu">
            <Icon name="menu" />
          </button>
          <span className="font-semibold">EmployeeHub</span>
        </header>
        <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
