import { useCallback, useEffect, useState } from 'react';
import { api, describeError } from '../api';
import { useAuth } from '../auth/AuthContext';
import type { Employee } from '../models';
import { formatDateTime, formatMinutes } from '../format';
import Messages from '../components/Messages';
import EmployeeFormModal from '../components/EmployeeFormModal';
import WorkTimeModal from '../components/WorkTimeModal';

// Страница директора «Сотрудники»: список, роль, место работы, доступ и время работы.
// Карандаш слева при наведении — редактировать; нажатие на строку — время работы.
export default function EmployeesPage() {
  const { session } = useAuth();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  // Окно «Новый / Редактировать сотрудника»: null — закрыто, 'new' — новый
  const [formTarget, setFormTarget] = useState<Employee | 'new' | null>(null);
  // Окно «Время работы»
  const [workTimeOf, setWorkTimeOf] = useState<Employee | null>(null);

  const load = useCallback(() => {
    api.getEmployees().then(setEmployees).catch(err => {
      console.error(err);
      setError('Не удалось загрузить сотрудников. ' + describeError(err));
    });
  }, []);

  useEffect(load, [load]);

  const isSelf = (e: Employee | null) => e !== null && e.id === session?.employeeId;
  const editTarget = formTarget === 'new' ? null : formTarget;

  const onSaved = (e: Employee) => {
    const isNew = formTarget === 'new';
    setFormTarget(null);
    setSuccess(isNew ? `Сотрудник «${e.fullName}» добавлен.` : `Данные сотрудника «${e.fullName}» сохранены.`);
    load();
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h2 className="page-title">👥 Сотрудники</h2>
          <p className="subtitle">Роли, места работы, доступ к системе и время работы</p>
        </div>
        <button type="button" className="btn btn-primary add-btn" onClick={() => setFormTarget('new')}>+ Добавить сотрудника</button>
      </div>

      <Messages success={success} error={error} />

      {/* Нажатие на строку — время работы; карандаш слева (при наведении) — редактирование */}
      <section className="card table-wrap">
        <table className="table table-compact">
          <thead>
            <tr>
              <th className="edit-col"></th>
              <th>ФИО</th>
              <th>Логин</th>
              <th>Роль</th>
              <th>Место работы</th>
              <th>Статус</th>
              <th>Последний вход</th>
              <th className="text-right">За месяц</th>
            </tr>
          </thead>
          <tbody>
            {employees.map(e => (
              <tr key={e.id} className={'clickable hover-row' + (e.active ? '' : ' inactive')} onClick={() => setWorkTimeOf(e)} title="Показать время работы">
                <td className="edit-col" onClick={ev => ev.stopPropagation()}>
                  <button type="button" className="hover-edit" title="Редактировать сотрудника" onClick={() => setFormTarget(e)}>✏️</button>
                </td>
                <td className="name">
                  {e.fullName} {isSelf(e) && <span className="muted small">(это вы)</span>}
                </td>
                <td>{e.login}</td>
                <td className="nowrap">{e.role === 'DIRECTOR' ? '👔 Директор' : '🏛️ Инспектор'}</td>
                <td>{e.workplace || '—'}</td>
                <td className="nowrap">
                  {!e.active
                    ? <span className="badge badge-debt">🔒 Доступ закрыт</span>
                    : e.online
                      ? <span className="badge badge-paid">● В системе</span>
                      : <span className="muted">Не в системе</span>}
                </td>
                <td className="nowrap">{formatDateTime(e.lastLoginAt)}</td>
                <td className="text-right nowrap"><strong>{formatMinutes(e.minutesThisMonth)}</strong></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {formTarget && (
        <EmployeeFormModal
          employee={editTarget}
          isSelf={isSelf(editTarget)}
          onSaved={onSaved}
          onClose={() => setFormTarget(null)}
        />
      )}

      {workTimeOf && <WorkTimeModal employee={workTimeOf} onClose={() => setWorkTimeOf(null)} />}
    </>
  );
}
