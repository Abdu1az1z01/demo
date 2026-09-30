import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, describeError } from '../api';
import type { Bill, Subscriber } from '../models';
import { findService } from '../services';
import { useTariffs } from '../TariffContext';
import { formatDate, formatPeriod } from '../format';
import Messages from '../components/Messages';
import BillStatusModal from '../components/BillStatusModal';
import SubscriberFormModal from '../components/SubscriberFormModal';

// Страница абонента (/services/:serviceId/subscribers/:id): данные, история начислений по месяцам,
// исправление статуса оплаты (карандаш при наведении на месяц) и передача показаний.
export default function SubscriberPage() {
  const params = useParams();
  const id = Number(params.id);
  const service = findService(params.serviceId);
  const navigate = useNavigate();
  const { currentPrice } = useTariffs();

  const [subscriber, setSubscriber] = useState<Subscriber | null>(null);
  const [bills, setBills] = useState<Bill[]>([]);
  const [newReading, setNewReading] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isEditing, setIsEditing] = useState(false);            // открыто окно «Редактировать абонента»
  const [statusBill, setStatusBill] = useState<Bill | null>(null);   // окно «Изменить статус оплаты»

  const loadBills = useCallback((subscriberId: number) => {
    api.getBills(subscriberId).then(setBills).catch(err => console.error(err));
  }, []);

  useEffect(() => {
    setSubscriber(null);
    setError('');
    setSuccess('');
    api.getSubscriber(id).then(setSubscriber).catch(err => {
      console.error(err);
      setError('Не удалось загрузить абонента. ' + describeError(err));
    });
    loadBills(id);
  }, [id, loadBills]);

  const unpaidCount = bills.filter(b => !b.paid).length;

  const onStatusChanged = (updated: Subscriber) => {
    setSubscriber(updated);
    loadBills(updated.id);
    setError('');
  };

  // Статус оплаты изменён вручную через карандаш у месяца
  const onStatusSaved = (updated: Subscriber) => {
    const bill = statusBill;
    setStatusBill(null);
    onStatusChanged(updated);
    if (bill) setSuccess(`Статус начисления за ${formatPeriod(bill.period)} изменён.`);
  };

  const onSaved = (updated: Subscriber) => {
    setSubscriber(updated);
    setIsEditing(false);
    setError('');
    setSuccess('Данные абонента сохранены.');
  };

  const onSubmitReading = async () => {
    if (newReading.trim() === '' || subscriber === null) return;
    const reading = Number(newReading);
    if (reading < subscriber.currentReading) {
      setError('Новые показания не могут быть меньше последних!');
      return;
    }
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      const updated = await api.submitReading(subscriber.id, reading);
      setSubscriber(updated);
      loadBills(updated.id);
      setNewReading('');
      setSuccess('Показания приняты, начисление добавлено в историю.');
    } catch (err) {
      console.error(err);
      setError('Ошибка передачи показаний: ' + describeError(err));
    }
    setIsLoading(false);
  };

  const s = subscriber;

  return (
    <>
      <Link className="back" to={`/services/${service?.id ?? ''}`}>← К списку абонентов</Link>

      {!s ? <Messages error={error} /> : (
        <>
          {/* Данные абонента и общий долг */}
          <section className="card info">
            <div className="info-main">
              <div className="muted">{service?.icon} {service?.name} · лицевой счёт № {s.accountNumber}</div>
              <h2 className="page-title">{s.ownerName}</h2>
              <div className="info-actions">
                <button type="button" className="btn btn-secondary btn-small" onClick={() => setIsEditing(true)}>✏️ Редактировать</button>
              </div>
              <div className="info-grid">
                <div><span className="muted">Адрес:</span> {s.address}</div>
                <div><span className="muted">Телефон:</span> {s.phone || '—'}</div>
                <div><span className="muted">Тариф (единый):</span> {currentPrice(s.serviceType) ?? '—'} сом/{service?.unit}</div>
                <div><span className="muted">Последние показания:</span> {s.currentReading}</div>
              </div>
            </div>

            <div className={'total ' + (s.debt > 0 ? 'total-debt' : 'total-paid')}>
              {s.debt > 0 ? (
                <>
                  <div className="total-label">Общий долг</div>
                  <div className="total-value">{s.debt} <span>сом</span></div>
                  <div className="total-note">Не оплачено месяцев: {unpaidCount}</div>
                </>
              ) : (
                <>
                  <div className="total-label">Долгов нет</div>
                  <div className="total-value">✓ Оплачено</div>
                </>
              )}
            </div>
          </section>

          <Messages success={success} error={error} />

          {/* История начислений: красные — долг, зелёные — оплачено.
              При наведении на месяц слева появляется карандаш — исправить статус оплаты. */}
          <h3 className="section-title">История начислений</h3>
          <section className="card table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th className="edit-col"></th>
                  <th>Месяц</th>
                  <th>Показания</th>
                  <th>Расход</th>
                  <th>Тариф</th>
                  <th className="text-right">Сумма</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                {bills.map(bill => (
                  <tr key={bill.id} className={'hover-row ' + (bill.paid ? 'row-paid' : 'row-debt')}>
                    <td className="edit-col">
                      <button type="button" className="hover-edit" title="Изменить статус оплаты" onClick={() => setStatusBill(bill)}>✏️</button>
                    </td>
                    <td className="period">{formatPeriod(bill.period)}</td>
                    <td>{bill.previousReading} → {bill.currentReading}</td>
                    <td>{bill.consumption} {service?.unit}</td>
                    <td className="muted">{bill.tariff !== null ? `${bill.tariff} сом` : '—'}</td>
                    <td className={'text-right ' + (bill.paid ? 'paid' : 'debt')}>{bill.amount} сом</td>
                    <td>
                      {bill.paid ? (
                        <>
                          <span className="badge badge-paid">Оплачено {bill.paidAt ? formatDate(bill.paidAt) : ''}</span>
                          {bill.paidVia && <div className="muted small">через {bill.paidVia}</div>}
                        </>
                      ) : (
                        <span className="badge badge-debt">Не оплачено</span>
                      )}
                      {bill.statusNote && (
                        <div className="status-note" title={bill.statusNote}>✎ Изменено инспекцией: {bill.statusNote}</div>
                      )}
                    </td>
                  </tr>
                ))}
                {bills.length === 0 && <tr><td colSpan={7} className="muted empty-row">Начислений пока нет</td></tr>}
              </tbody>
            </table>
          </section>

          {/* Передать новые показания: создаёт начисление за текущий месяц */}
          <section className="card reading">
            <h3>Передать новые показания счётчика</h3>
            <form className="input-row" onSubmit={e => { e.preventDefault(); onSubmitReading(); }}>
              <input
                type="number"
                className="input"
                value={newReading}
                onChange={e => setNewReading(e.target.value)}
                placeholder={`Больше или равно ${s.currentReading}`}
              />
              <button type="submit" className="btn btn-success" disabled={isLoading}>Отправить</button>
            </form>
          </section>

          {/* Окно «Изменить статус оплаты» */}
          {statusBill && (
            <BillStatusModal
              bill={statusBill}
              periodLabel={formatPeriod(statusBill.period)}
              onSaved={onStatusSaved}
              onClose={() => setStatusBill(null)}
            />
          )}

          {/* Окно «Редактировать абонента» */}
          {isEditing && service && (
            <SubscriberFormModal
              service={service}
              subscriber={s}
              onSaved={onSaved}
              onStatusChanged={onStatusChanged}
              onDeleted={() => navigate(`/services/${service.id}`)}
              onClose={() => setIsEditing(false)}
            />
          )}
        </>
      )}
    </>
  );
}
