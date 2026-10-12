import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { Icon } from '../components/Icon';
import { Alert } from '../components/ui';
import { useAuth } from '../stores/AuthContext';

const highlights = [
  'Live workforce metrics at a glance',
  'Role-based access for admins, managers, and viewers',
  'Salary data visible only to the people who need it',
];

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const response = await api.post('/auth/login', { username, password });
      login(response.data.token);
      navigate('/', { replace: true });
    } catch {
      setError('Incorrect username or password.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Brand panel */}
      <div className="relative hidden w-1/2 overflow-hidden bg-slate-900 lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-brand-600/30 blur-3xl" />
        <div className="absolute -bottom-40 right-0 h-[28rem] w-[28rem] rounded-full bg-violet-600/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-500 text-xl font-bold text-white">E</div>
          <span className="text-xl font-semibold text-white">EmployeeHub</span>
        </div>

        <div className="relative">
          <h1 className="text-4xl font-semibold leading-tight text-white">
            Your whole team,
            <br />
            in one place.
          </h1>
          <ul className="mt-8 space-y-4">
            {highlights.map((text) => (
              <li key={text} className="flex items-center gap-3 text-slate-300">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-500/20 text-brand-300">
                  <Icon name="userCheck" className="h-3.5 w-3.5" />
                </span>
                {text}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-sm text-slate-500">Spring Boot · React · PostgreSQL</p>
      </div>

      {/* Form */}
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm animate-pop-in">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-600 text-xl font-bold text-white">E</div>
            <span className="text-xl font-semibold">EmployeeHub</span>
          </div>

          <h2 className="text-2xl font-semibold text-slate-900">Welcome back</h2>
          <p className="mt-1 text-sm text-slate-500">Sign in to manage your workforce.</p>

          <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="username" className="label">Username</label>
              <input
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="input"
                autoComplete="username"
                autoFocus
                required
              />
            </div>
            <div>
              <label htmlFor="password" className="label">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input"
                autoComplete="current-password"
                required
              />
            </div>

            {error && <Alert>{error}</Alert>}

            <button type="submit" className="btn-primary w-full py-2.5" disabled={submitting}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="mt-8 rounded-xl bg-slate-100 p-4 text-xs text-slate-600">
            <p className="flex items-center gap-1.5 font-medium text-slate-700">
              <Icon name="lock" className="h-3.5 w-3.5" /> Demo accounts
            </p>
            <p className="mt-1.5">
              <code className="font-mono">admin / admin123</code> · <code className="font-mono">manager / manager123</code>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
