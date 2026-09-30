import { Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { RequireAuth, RequireDirector, RequireGuest } from './auth/guards';
import { SERVICES } from './services';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SubscribersPage from './pages/SubscribersPage';
import SubscriberPage from './pages/SubscriberPage';
import EmployeesPage from './pages/EmployeesPage';
import TariffsPage from './pages/TariffsPage';

// Страницы сайта (всё, кроме входа, — только для сотрудников муниципальной инспекции):
//   /login                              — вход по логину и паролю
//   /                                   — стартовая («выберите предприятие»)
//   /services/cold-water                — таблица абонентов предприятия
//   /services/cold-water/subscribers/5  — история начислений абонента
//   /employees                          — сотрудники и их время работы (только директор)
//   /tariffs                            — единые тарифы услуг (только директор)
export default function App() {
  const { session, isLoggedIn, isDirector, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = () => {
    logout();
    navigate('/login');
  };

  const menuClass = ({ isActive }: { isActive: boolean }) => 'menu-item' + (isActive ? ' active' : '');

  return (
    <div className={'app' + (isLoggedIn ? '' : ' no-sidebar')}>
      {/* Шапка Zetta Billing: логотип и название слева, справа — кто вошёл и кнопка «Выйти» */}
      <header className="topbar">
        <Link className="brand" to="/">
          <img className="brand-logo" src="/logo.png" alt="" />
          <span className="brand-title">Zetta <span>Billing</span></span>
        </Link>

        <div className="user">
          {session && (
            <>
              <div className="user-info">
                <div className="user-name">{session.name}</div>
                <div className="user-role">{session.role === 'DIRECTOR' ? '👔 Директор' : '🏛️ Инспектор'}</div>
              </div>
              <button type="button" className="logout" onClick={onLogout}>Выйти</button>
            </>
          )}
          <span className="version">v1.0 · React</span>
        </div>
      </header>

      {/* Меню слева после входа: коммунальные услуги (список — в services.ts), у директора ещё «Управление» */}
      {isLoggedIn && (
        <nav className="sidebar">
          <div className="menu-title">Коммунальные услуги</div>
          {SERVICES.map(service => (
            <NavLink key={service.id} className={menuClass} to={`/services/${service.id}`}>
              <span className="menu-icon">{service.icon}</span>
              {service.name}
            </NavLink>
          ))}

          {isDirector && (
            <>
              <div className="menu-title menu-section">Управление</div>
              <NavLink className={menuClass} to="/employees">
                <span className="menu-icon">👥</span>
                Сотрудники
              </NavLink>
              <NavLink className={menuClass} to="/tariffs">
                <span className="menu-icon">💰</span>
                Тарифы
              </NavLink>
            </>
          )}
        </nav>
      )}

      <main className="content">
        <Routes>
          <Route path="/login" element={<RequireGuest><LoginPage /></RequireGuest>} />
          <Route path="/" element={<RequireAuth><HomePage /></RequireAuth>} />
          <Route path="/services/:serviceId" element={<RequireAuth><SubscribersPage /></RequireAuth>} />
          <Route path="/services/:serviceId/subscribers/:id" element={<RequireAuth><SubscriberPage /></RequireAuth>} />
          <Route path="/employees" element={<RequireDirector><EmployeesPage /></RequireDirector>} />
          <Route path="/tariffs" element={<RequireDirector><TariffsPage /></RequireDirector>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
