import { useState, type ComponentType, type SVGProps } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { useAuth, useUser } from '../auth/AuthContext';
import { Disclaimer } from './Disclaimer';
import {
  CloseIcon,
  HomeIcon,
  LogoutIcon,
  MenuIcon,
  PulseIcon,
} from './icons';

interface NavItem {
  to: string;
  label: string;
  icon: ComponentType<SVGProps<SVGSVGElement>>;
}

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: HomeIcon },
  { to: '/vitals', label: 'Vitals', icon: PulseIcon },
];

export function Layout() {
  const user = useUser();
  const { logout } = useAuth();
  const location = useLocation();
  // On small screens the sidebar is a drop-down that closes on navigation.
  const [menuFor, setMenuFor] = useState<string | null>(null);
  const menuOpen = menuFor === location.pathname;

  return (
    <div className="shell">
      <header className="topbar">
        <Brand />
        <button
          className="icon-button"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          aria-controls="sidebar"
          onClick={() => setMenuFor(menuOpen ? null : location.pathname)}
        >
          {menuOpen ? <CloseIcon /> : <MenuIcon />}
        </button>
      </header>

      <aside id="sidebar" className={`sidebar${menuOpen ? ' sidebar--open' : ''}`}>
        <div className="sidebar__brand-row">
          <Brand />
        </div>
        <nav className="sidebar__nav" aria-label="Main">
          {NAV.map(({ to, label, icon: IconComponent }) => (
            <NavLink key={to} to={to} end={to === '/'} className="nav-link">
              <IconComponent />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__footer">
          <div className="sidebar__user">
            <span className="avatar" aria-hidden>
              {user.displayName.slice(0, 1).toUpperCase()}
            </span>
            <span className="sidebar__user-name" title={user.email}>
              {user.displayName}
            </span>
          </div>
          <button className="nav-link nav-link--button" onClick={logout}>
            <LogoutIcon />
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
        <Disclaimer />
      </main>
    </div>
  );
}

function Brand() {
  return (
    <div className="brand">
      <img src="/favicon.svg" alt="" width={28} height={28} />
      <span>Health Tracker</span>
    </div>
  );
}
