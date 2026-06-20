import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import {
  BarChart3,
  Calendar,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Shield,
  Sun,
  Users,
  X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

const allNav = [
  { to: '/', label: 'Inicio', icon: LayoutDashboard, end: true, roles: ['ADMIN', 'TRAINER', 'USER'] },
  { to: '/equipos', label: 'Equipos', icon: Shield, roles: ['ADMIN', 'TRAINER'] },
  { to: '/jugadores', label: 'Jugadores', icon: Users, roles: ['ADMIN', 'TRAINER'] },
  { to: '/partidos', label: 'Partidos', icon: Calendar, roles: ['ADMIN', 'TRAINER', 'USER'] },
  { to: '/estadisticas', label: 'Stats', icon: BarChart3, roles: ['ADMIN', 'TRAINER', 'USER'] },
];

export function Layout() {
  const { user, logout, isUser } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const baseUrl = import.meta.env.BASE_URL.replace(/\/$/, '');
  const logoUrl = `${baseUrl}/${theme === 'light' ? 'logo-mono-dark.webp' : 'logo-primary.webp'}`;
  const compactLogoUrl = `${baseUrl}/logo-symbol.webp`;

  const role = user?.roleName?.toUpperCase() ?? '';
  const nav = allNav.filter((item) => item.roles.includes(role));

  const pageTitle =
    nav.find((item) =>
      item.end ? location.pathname === item.to : location.pathname.startsWith(item.to),
    )?.label ?? 'FMPRO';

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  return (
    <div className="app-shell app-shell--top">
      <header className="top-nav top-nav--desktop" aria-label="Navegación principal">
        <div className="top-nav__brand">
          <div className="top-nav__logo" aria-hidden>
            <img className="app-logo__img" src={logoUrl} alt="" />
          </div>
          <strong>FMPRO</strong>
        </div>

        <nav className="top-nav__links">
          {nav.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `top-nav__link${isActive ? ' top-nav__link--active' : ''}`
              }
            >
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="top-nav__user">
          {user && (
            <>
              <button
                type="button"
                className="top-nav__action"
                onClick={toggleTheme}
                aria-label={theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
              >
                {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <div className="top-nav__avatar" title={user.username}>{user.username[0]?.toUpperCase()}</div>
              <button
                type="button"
                className="top-nav__action top-nav__action--danger"
                onClick={() => void logout()}
                title="Cerrar sesión"
              >
                <LogOut size={18} />
              </button>
            </>
          )}
        </div>
      </header>

      <div className="app-body">
        <main className="main">
          <header className="topbar topbar--desktop">
            <h1>{pageTitle}</h1>
            <p>
              {isUser
                ? 'Consulta partidos y rendimiento en vivo'
                : 'Gestión integral de tu club'}
            </p>
          </header>
          <div className="main__content">
            <Outlet />
          </div>
        </main>
      </div>

      <nav className="bottom-nav" aria-label="Navegación móvil">
        {nav.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `bottom-nav__link${isActive ? ' bottom-nav__link--active' : ''}`
            }
          >
            <Icon size={20} />
            <span>{label}</span>
          </NavLink>
        ))}
        {user && (
          <button
            type="button"
            className={`bottom-nav__link bottom-nav__button${mobileMenuOpen ? ' bottom-nav__link--active' : ''}`}
            onClick={() => setMobileMenuOpen((v) => !v)}
            aria-label={mobileMenuOpen ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-menu"
          >
            {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            <span>Menú</span>
          </button>
        )}
      </nav>

      {user && mobileMenuOpen && (
        <div
          className="mobile-sheet__backdrop"
          role="presentation"
          onClick={() => setMobileMenuOpen(false)}
        >
          <section
            id="mobile-menu"
            className="mobile-sheet"
            role="dialog"
            aria-label="Menú"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mobile-sheet__handle" aria-hidden />
            <div className="mobile-sheet__header">
              <div className="mobile-sheet__brand">
                <span className="sidebar__logo" aria-hidden>
                    <img className="app-logo__img" src={compactLogoUrl} alt="" />
                </span>
                <div>
                  <strong>{user.username}</strong>
                  <span>{user.roleName}</span>
                </div>
              </div>
              <button
                type="button"
                className="mobile-sheet__close"
                onClick={() => setMobileMenuOpen(false)}
                aria-label="Cerrar menú"
              >
                <X size={18} />
              </button>
            </div>
            <div className="mobile-sheet__actions">
              <button
                type="button"
                className="mobile-sheet__action"
                onClick={toggleTheme}
                aria-label={theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
              >
                {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
                {theme === 'dark' ? 'Modo claro' : 'Modo oscuro'}
              </button>
              <button
                type="button"
                className="mobile-sheet__action mobile-sheet__action--danger"
                onClick={() => void logout()}
              >
                <LogOut size={18} />
                Cerrar sesión
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
