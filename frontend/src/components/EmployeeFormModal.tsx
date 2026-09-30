import { useState } from 'react';
import { api, describeError } from '../api';
import type { Employee, EmployeeForm, Role } from '../models';
import Modal from './Modal';
import Messages from './Messages';

// Окно «Новый сотрудник» / «Редактировать сотрудника» (только директор):
// ФИО, логин, роль, место работы, доступ и пароль.
export default function EmployeeFormModal({ employee, isSelf, onSaved, onClose }: {
  employee: Employee | null;
  isSelf: boolean;   // директор редактирует сам себя — роль и доступ менять нельзя
  onSaved: (employee: Employee) => void;
  onClose: () => void;
}) {
  const isEdit = employee !== null;
  const [form, setForm] = useState<EmployeeForm>(() => employee
    ? { login: employee.login, password: '', fullName: employee.fullName, role: employee.role, workplace: employee.workplace ?? '', active: employee.active }
    : { login: '', password: '', fullName: '', role: 'INSPECTOR', workplace: '', active: true });
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  const update = (patch: Partial<EmployeeForm>) => setForm(f => ({ ...f, ...patch }));

  const save = async () => {
    if (!form.fullName.trim() || (!isEdit && !form.login.trim())) {
      setError('Заполните ФИО и логин.');
      return;
    }
    if ((!isEdit || form.password) && form.password.length < 6) {
      setError('Пароль — не короче 6 символов.');
      return;
    }
    setIsSaving(true);
    setError('');
    try {
      onSaved(employee ? await api.updateEmployee(employee.id, form) : await api.createEmployee(form));
    } catch (err) {
      console.error(err);
      setError(describeError(err));
      setIsSaving(false);
    }
  };

  return (
    <Modal onClose={onClose} onSubmit={save}>
      <h3>{isEdit ? 'Редактировать сотрудника' : 'Новый сотрудник'}</h3>

      <label className="field">
        ФИО *
        <input className="input" value={form.fullName} onChange={e => update({ fullName: e.target.value })} placeholder="Фамилия Имя Отчество" />
      </label>

      <div className="row-2">
        <label className="field">
          Логин *
          <input className="input" value={form.login} onChange={e => update({ login: e.target.value })} disabled={isEdit} autoComplete="off" />
        </label>
        <label className="field">
          Роль *
          <select className="input" value={form.role} onChange={e => update({ role: e.target.value as Role })} disabled={isSelf}>
            <option value="INSPECTOR">🏛️ Инспектор</option>
            <option value="DIRECTOR">👔 Директор</option>
          </select>
        </label>
      </div>

      <label className="field">
        Место работы / участок
        <input className="input" value={form.workplace} onChange={e => update({ workplace: e.target.value })} placeholder="Например: Октябрьский район" />
      </label>

      <label className="field">
        {isEdit ? 'Новый пароль (оставьте пустым, чтобы не менять)' : 'Пароль *'}
        <input className="input" type="password" autoComplete="new-password" value={form.password} onChange={e => update({ password: e.target.value })} placeholder="Не короче 6 символов" />
      </label>

      {isEdit && (
        <>
          <label className="check">
            <input type="checkbox" checked={form.active} onChange={e => update({ active: e.target.checked })} disabled={isSelf} />
            Доступ к системе открыт
          </label>
          {!form.active && <p className="muted note">Сотрудник не сможет войти, а текущий вход будет сразу закрыт.</p>}
        </>
      )}

      <Messages error={error} />

      <div className="actions">
        <button type="button" className="btn btn-secondary" onClick={onClose}>Отмена</button>
        <button type="submit" className="btn btn-primary" disabled={isSaving}>
          {isSaving ? 'Сохранение...' : 'Сохранить'}
        </button>
      </div>
    </Modal>
  );
}
