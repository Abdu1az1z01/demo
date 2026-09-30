import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { describeError } from '../api';
import { useAuth } from '../auth/AuthContext';
import Messages from '../components/Messages';

// Страница входа для сотрудников муниципальной инспекции
export default function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!login.trim() || !password) {
      setError('Введите логин и пароль.');
      return;
    }
    setIsLoading(true);
    setError('');
    try {
      await auth.login(login.trim(), password);
      navigate('/');
    } catch (err) {
      console.error(err);
      setError(describeError(err));
      setIsLoading(false);
    }
  };

  return (
    <div className="login">
      <form className="card login-form" onSubmit={submit}>
        <img className="login-logo" src="/logo.png" alt="Zetta Billing" />
        <h2 className="page-title">Вход в Zetta Billing</h2>
        <p className="subtitle">Система муниципальной инспекции коммунальных услуг Бишкека</p>

        <label className="field">
          Логин
          <input className="input" autoComplete="username" value={login} onChange={e => setLogin(e.target.value)} />
        </label>
        <label className="field">
          Пароль
          <input className="input" type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} />
        </label>

        <Messages error={error} />

        <button type="submit" className="btn btn-primary submit" disabled={isLoading}>
          {isLoading ? 'Вход...' : 'Войти'}
        </button>
      </form>
    </div>
  );
}
