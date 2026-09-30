import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, setAuthToken, setUnauthorizedHandler } from '../api';
import type { AuthSession } from '../models';

const STORAGE_KEY = 'zetta-session';

interface AuthValue {
  session: AuthSession | null;
  isLoggedIn: boolean;
  isDirector: boolean;
  login: (login: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

// Вход/выход сотрудников. Сессия хранится в браузере, чтобы после обновления страницы не входить заново.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(readStored);

  // Токен добавляется к каждому запросу на бэкенд
  setAuthToken(session?.token ?? null);

  // Забыть сессию (например, когда бэкенд ответил 401 — токен устарел)
  const clear = useCallback(() => {
    setAuthToken(null);
    setSession(null);
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* браузер запретил хранилище */ }
  }, []);

  useEffect(() => setUnauthorizedHandler(clear), [clear]);

  const login = useCallback(async (login: string, password: string) => {
    const s = await api.login(login, password);
    setAuthToken(s.token);
    setSession(s);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(s)); } catch { /* браузер запретил хранилище */ }
  }, []);

  const logout = useCallback(() => {
    // Сообщаем бэкенду, что токен больше не нужен (ошибку игнорируем — выходим в любом случае)
    api.logout().catch(() => {});
    clear();
  }, [clear]);

  const value = useMemo<AuthValue>(() => ({
    session,
    isLoggedIn: session !== null,
    isDirector: session?.role === 'DIRECTOR',
    login,
    logout
  }), [session, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth должен вызываться внутри <AuthProvider>');
  return value;
}

function readStored(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const session = raw ? (JSON.parse(raw) as AuthSession) : null;
    // Сессия от старой версии сайта (без роли) не подходит — нужно войти заново
    return session?.token && session.role ? session : null;
  } catch {
    return null;
  }
}
