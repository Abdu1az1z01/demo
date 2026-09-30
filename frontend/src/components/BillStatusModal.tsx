import { useState } from 'react';
import { api, describeError } from '../api';
import type { Bill, Subscriber } from '../models';
import Modal from './Modal';
import Messages from './Messages';

// Окно «Изменить статус оплаты» для инспекции: если при оплате была ошибка
// (деньги списались, но оплата не отметилась, или платёж отменён банком).
// Причина обязательна — она сохраняется и видна в истории начислений.
export default function BillStatusModal({ bill, periodLabel, onSaved, onClose }: {
  bill: Bill;
  periodLabel: string;
  onSaved: (subscriber: Subscriber) => void;
  onClose: () => void;
}) {
  // По умолчанию предлагаем противоположный статус — обычно его и нужно исправить
  const [paid, setPaid] = useState(!bill.paid);
  const [note, setNote] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  const save = async () => {
    if (!note.trim()) {
      setError('Укажите причину изменения статуса.');
      return;
    }
    setIsSaving(true);
    setError('');
    try {
      onSaved(await api.changeBillStatus(bill.id, paid, note.trim()));
    } catch (err) {
      console.error(err);
      setError(describeError(err));
      setIsSaving(false);
    }
  };

  return (
    <Modal onClose={onClose} onSubmit={save}>
      <h3>Изменить статус оплаты</h3>
      <div className="muted">
        {periodLabel} · {bill.amount} сом · сейчас:{' '}
        <strong className={bill.paid ? 'paid' : 'debt'}>{bill.paid ? 'оплачено' : 'не оплачено'}</strong>
      </div>

      <div className="options">
        <label className={'option' + (paid ? ' selected' : '')}>
          <input type="radio" name="paid" checked={paid} onChange={() => setPaid(true)} />
          <span className="paid">✓ Оплачено</span>
        </label>
        <label className={'option' + (!paid ? ' selected' : '')}>
          <input type="radio" name="paid" checked={!paid} onChange={() => setPaid(false)} />
          <span className="debt">✗ Не оплачено</span>
        </label>
      </div>

      <label className="field">
        Причина изменения *
        <textarea
          className="input"
          rows={3}
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="Например: деньги списаны, банк подтвердил оплату, чек № 12345"
        />
      </label>

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
