import { useEffect, useState } from 'react';
import { api, describeError } from '../api';
import type { Employee, WorkTime } from '../models';
import { currentMonth, formatDateTime, formatMinutes } from '../format';
import Modal from './Modal';
import Messages from './Messages';

// Окно «Время работы» сотрудника за выбранный месяц: все входы и выходы и сумма часов
export default function WorkTimeModal({ employee, onClose }: { employee: Employee; onClose: () => void }) {
  const [month, setMonth] = useState(currentMonth);
  const [workTime, setWorkTime] = useState<WorkTime | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!month) return;
    let cancelled = false;
    setError('');
    api.getWorkTime(employee.id, month)
      .then(wt => { if (!cancelled) setWorkTime(wt); })
      .catch(err => {
        console.error(err);
        if (!cancelled) setError(describeError(err));
      });
    return () => { cancelled = true; };
  }, [employee.id, month]);

  return (
    <Modal onClose={onClose}>
      <h3>Время работы</h3>
      <div className="muted">{employee.fullName} · {employee.workplace || 'место работы не указано'}</div>

      <label className="field month">
        Месяц
        <input className="input" type="month" value={month} onChange={e => setMonth(e.target.value)} />
      </label>

      {workTime && (
        <>
          <div className="worktime-total">
            Всего за месяц: <strong>{formatMinutes(workTime.totalMinutes)}</strong> · входов: {workTime.sessions.length}
          </div>
          <div className="sessions">
            {workTime.sessions.map(s => (
              <div key={s.startedAt} className="session">
                <span>🟢 {formatDateTime(s.startedAt)}</span>
                <span>
                  {s.online ? (
                    <span className="online">сейчас в системе</span>
                  ) : (
                    <>
                      → {formatDateTime(s.finishedAt)}
                      {!s.ended && (
                        <span className="muted small" title="Сотрудник не нажал «Выйти» — учтено до последнего действия"> (без выхода)</span>
                      )}
                    </>
                  )}
                </span>
                <span className="minutes">{formatMinutes(s.minutes)}</span>
              </div>
            ))}
            {workTime.sessions.length === 0 && <div className="muted">В этом месяце сотрудник не входил в систему</div>}
          </div>
        </>
      )}

      <Messages error={error} />

      <div className="actions">
        <button type="button" className="btn btn-secondary" onClick={onClose}>Закрыть</button>
      </div>
    </Modal>
  );
}
