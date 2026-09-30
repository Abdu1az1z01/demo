import { useCallback, useEffect, useState, type ChangeEvent } from 'react';
import { api, describeError } from '../api';
import type { Bill, Subscriber, SubscriberForm } from '../models';
import type { UtilityService } from '../services';
import { formatDate, formatPeriod } from '../format';
import Modal from './Modal';
import Messages from './Messages';
import BillStatusModal from './BillStatusModal';

// Окно «Добавить абонента» / «Редактировать абонента».
// Если передан subscriber — редактирование: данные абонента, статусы оплаты по месяцам
// (карандаш при наведении на месяц) и удаление абонента. Иначе — добавление в указанную услугу.
export default function SubscriberFormModal({ service, subscriber, onSaved, onStatusChanged, onDeleted, onClose }: {
  service: UtilityService;
  subscriber: Subscriber | null;
  onSaved: (subscriber: Subscriber) => void;
  onStatusChanged: (subscriber: Subscriber) => void;   // изменили статус оплаты какого-то месяца
  onDeleted: () => void;
  onClose: () => void;
}) {
  const isEdit = subscriber !== null;
  const [form, setForm] = useState<SubscriberForm>(() => subscriber
    ? { ownerName: subscriber.ownerName, accountNumber: subscriber.accountNumber, phone: subscriber.phone ?? '', address: subscriber.address ?? '' }
    : { ownerName: '', accountNumber: '', phone: '', address: '', currentReading: 0 });
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [bills, setBills] = useState<Bill[]>([]);
  const [statusBill, setStatusBill] = useState<Bill | null>(null);   // месяц, у которого меняем статус

  const loadBills = useCallback((id: number) => {
    api.getBills(id).then(setBills).catch(err => console.error(err));
  }, []);

  useEffect(() => {
    if (subscriber) loadBills(subscriber.id);
  }, [subscriber, loadBills]);

  const set = (field: keyof SubscriberForm) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [field]: field === 'currentReading' ? (e.target.value === '' ? null : Number(e.target.value)) : e.target.value }));

  const save = async () => {
    if (!form.ownerName.trim() || !form.accountNumber.trim()) {
      setError('Заполните ФИО и лицевой счёт.');
      return;
    }
    setIsSaving(true);
    setError('');
    try {
      const result = subscriber
        ? await api.updateSubscriber(subscriber.id, form)
        : await api.createSubscriber(service.id, form);
      onSaved(result);
    } catch (err) {
      console.error(err);
      setError(describeError(err));
      setIsSaving(false);
    }
  };

  const deleteSubscriber = async () => {
    if (!subscriber || !confirm(`Удалить абонента «${subscriber.ownerName}» (л/с ${subscriber.accountNumber}) вместе со всей историей начислений?`)) {
      return;
    }
    try {
      await api.deleteSubscriber(subscriber.id);
      onDeleted();
    } catch (err) {
      console.error(err);
      setError('Не удалось удалить абонента. ' + describeError(err));
    }
  };

  const onStatusSaved = (updated: Subscriber) => {
    setStatusBill(null);
    loadBills(updated.id);
    onStatusChanged(updated);
  };

  return (
    <>
      <Modal onClose={onClose} onSubmit={save}>
        <h3>{isEdit ? 'Редактировать абонента' : 'Новый абонент'}</h3>
        <div className="muted">{service.icon} {service.name}</div>

        <label className="field">
          ФИО *
          <input className="input" value={form.ownerName} onChange={set('ownerName')} placeholder="Фамилия Имя Отчество" />
        </label>

        <div className="row-2">
          <label className="field">
            Лицевой счёт *
            <input className="input" value={form.accountNumber} onChange={set('accountNumber')} placeholder="102030" />
          </label>
          <label className="field">
            Телефон
            <input className="input" value={form.phone} onChange={set('phone')} placeholder="+996 555 123456" />
          </label>
        </div>

        <label className="field">
          Адрес
          <input className="input" value={form.address} onChange={set('address')} placeholder="г. Бишкек, ул. ..., кв. ..." />
        </label>

        {!isEdit && (
          <label className="field">
            Начальные показания счётчика
            <input className="input" type="number" min={0} value={form.currentReading ?? ''} onChange={set('currentReading')} />
          </label>
        )}

        {/* Статусы оплаты по месяцам (только при редактировании).
            При наведении на месяц слева появляется карандаш — исправить статус. */}
        {isEdit && (
          <div className="form-section">
            <div className="form-section-title">Статусы оплаты</div>
            <div className="bills">
              {bills.map(bill => (
                <div key={bill.id} className={'hover-row bill-row ' + (bill.paid ? 'row-paid' : 'row-debt')}>
                  <button type="button" className="hover-edit" title="Изменить статус оплаты" onClick={() => setStatusBill(bill)}>✏️</button>
                  <span className="bill-period">{formatPeriod(bill.period)}</span>
                  <span className={bill.paid ? 'paid' : 'debt'}>{bill.amount} сом</span>
                  {bill.paid
                    ? <span className="badge badge-paid">Оплачено{bill.paidAt ? ' ' + formatDate(bill.paidAt) : ''}</span>
                    : <span className="badge badge-debt">Не оплачено</span>}
                </div>
              ))}
              {bills.length === 0 && <div className="muted">Начислений пока нет</div>}
            </div>
          </div>
        )}

        <Messages error={error} />

        <div className="actions">
          {isEdit && (
            <button type="button" className="btn btn-danger delete" onClick={deleteSubscriber}>🗑️ Удалить абонента</button>
          )}
          <button type="button" className="btn btn-secondary" onClick={onClose}>Отмена</button>
          <button type="submit" className="btn btn-primary" disabled={isSaving}>
            {isSaving ? 'Сохранение...' : 'Сохранить'}
          </button>
        </div>
      </Modal>

      {/* Окно «Изменить статус оплаты» поверх окна редактирования */}
      {statusBill && (
        <BillStatusModal
          bill={statusBill}
          periodLabel={formatPeriod(statusBill.period)}
          onSaved={onStatusSaved}
          onClose={() => setStatusBill(null)}
        />
      )}
    </>
  );
}
