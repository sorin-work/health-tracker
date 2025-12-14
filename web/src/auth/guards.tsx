import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './AuthContext';

function FullPageSpinner() {
  return (
    <div className="center-page" role="status">
      <span className="spinner" aria-hidden />
      <span className="sr-only">Loading…</span>
    </div>
  );
}

export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <FullPageSpinner />;
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

export function RedirectIfAuthenticated() {
  const { status } = useAuth();
  if (status === 'loading') return <FullPageSpinner />;
  if (status === 'authenticated') return <Navigate to="/" replace />;
  return <Outlet />;
}
