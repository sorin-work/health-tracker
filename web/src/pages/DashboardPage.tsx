import { useUser } from '../auth/AuthContext';

export function DashboardPage() {
  const user = useUser();
  return (
    <div className="page-header">
      <div>
        <h1>Hello, {user.displayName}</h1>
        <p className="muted">Your latest readings and today’s medications will show up here.</p>
      </div>
    </div>
  );
}
