import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

// Все страницы сайта — только для вошедших сотрудников инспекции
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth();
  return isLoggedIn ? children : <Navigate to="/login" replace />;
}

// Страница входа: если уже вошли — сразу на стартовую
export function RequireGuest({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth();
  return isLoggedIn ? <Navigate to="/" replace /> : children;
}

// Страницы директора (сотрудники, тарифы): инспектора отправляем на стартовую
export function RequireDirector({ children }: { children: ReactNode }) {
  const { isLoggedIn, isDirector } = useAuth();
  if (isDirector) return children;
  return <Navigate to={isLoggedIn ? '/' : '/login'} replace />;
}
