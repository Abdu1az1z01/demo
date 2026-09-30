import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { api, describeError } from '../api';
import type { Tariff } from '../models';
import { SERVICES, type UtilityService } from '../services';
import { useTariffs } from '../TariffContext';
import { formatDate, todayIso } from '../format';
import Messages from '../components/Messages';

// Страница директора «Тарифы»: единый тариф для каждой услуги.
// Новый тариф начинает действовать с выбранной даты; начисления, созданные раньше, не пересчитываются.
export default function TariffsPage() {
  const { tariffs, reload } = useTariffs();
  const today = todayIso();

  // Форма «Новый тариф» открыта для этой услуги
  const [editingService, setEditingService] = useState<string | null>(null);
  const [newPrice, setNewPrice] = useState('');
  const [newDate, setNewDate] = useState(today);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // При открытии страницы берём свежие тарифы
  useEffect(reload, [reload]);

  // Тарифы по услугам: { 'gas': [планируемые..., действующий, прошлые...] }
  const byService = useMemo(() => {
    const map: Record<string, Tariff[]> = {};
    for (const t of tariffs) (map[t.serviceType] ??= []).push(t);
    return map;
  }, [tariffs]);

  const current = (serviceId: string) => byService[serviceId]?.find(t => t.status === 'CURRENT');

  const openForm = (service: UtilityService) => {
    setEditingService(service.id);
    setNewPrice(String(current(service.id)?.price ?? ''));
    setNewDate(today);
    setError('');
    setSuccess('');
  };

  const save = async (e: FormEvent, service: UtilityService) => {
    e.preventDefault();
    const price = Number(newPrice);
    if (newPrice.trim() === '' || !(price > 0)) {
      setError('Введите тариф больше нуля.');
      return;
    }
    if (!newDate || newDate < today) {
      setError('Дата начала — сегодня или позже.');
      return;
    }
    setIsSaving(true);
    setError('');
    try {
      const t = await api.createTariff(service.id, price, newDate);
      setEditingService(null);
      setSuccess(`${service.name}: тариф ${t.price} сом/${service.unit} с ${formatDate(t.effectiveFrom)}.`);
      reload();
    } catch (err) {
      console.error(err);
      setError(describeError(err));
    }
    setIsSaving(false);
  };

  const cancel = async (tariff: Tariff, service: UtilityService) => {
    if (!confirm(`Отменить тариф ${tariff.price} сом/${service.unit} с ${formatDate(tariff.effectiveFrom)}?`)) return;
    try {
      await api.cancelTariff(tariff.id);
      setSuccess('Запланированный тариф отменён.');
      reload();
    } catch (err) {
      console.error(err);
      setError(describeError(err));
    }
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h2 className="page-title">💰 Тарифы</h2>
          <p className="subtitle tariffs-subtitle">
            Единый тариф для всех абонентов услуги. Новый тариф применяется к начислениям с выбранной даты,
            уже созданные начисления не пересчитываются.
          </p>
        </div>
      </div>

      <Messages success={success} error={editingService ? '' : error} />

      <div className="tariff-grid">
        {SERVICES.map(service => {
          const t = current(service.id);
          return (
            <section key={service.id} className="card tariff-card">
              <div className="tariff-head">
                <div className="tariff-service">{service.icon} {service.name}</div>
                <button type="button" className="btn btn-primary btn-small" onClick={() => openForm(service)}>Изменить тариф</button>
              </div>

              <div>
                {t ? (
                  <>
                    <div className="price">{t.price} <span>сом/{service.unit}</span></div>
                    <div className="muted">действует с {formatDate(t.effectiveFrom)}</div>
                  </>
                ) : (
                  <div className="muted">Тариф не установлен</div>
                )}
              </div>

              {/* Форма нового тарифа */}
              {editingService === service.id && (
                <form className="new-form" onSubmit={e => save(e, service)}>
                  <label className="field">
                    Новый тариф, сом/{service.unit}
                    <input className="input" type="number" step="0.01" min="0.01" value={newPrice} onChange={e => setNewPrice(e.target.value)} />
                  </label>
                  <label className="field">
                    Действует с
                    <input className="input" type="date" min={today} value={newDate} onChange={e => setNewDate(e.target.value)} />
                  </label>
                  <Messages error={error} />
                  <div className="actions">
                    <button type="button" className="btn btn-secondary btn-small" onClick={() => setEditingService(null)}>Отмена</button>
                    <button type="submit" className="btn btn-success btn-small" disabled={isSaving}>Сохранить</button>
                  </div>
                </form>
              )}

              {/* История: запланированные, действующий, прошлые */}
              <div className="history">
                {(byService[service.id] ?? []).map(t => (
                  <div key={t.id} className={'history-row' + (t.status === 'PLANNED' ? ' planned' : t.status === 'CURRENT' ? ' current-row' : '')}>
                    <span className="history-date">с {formatDate(t.effectiveFrom)}</span>
                    <span className="value">{t.price} сом</span>
                    <span className="history-status">
                      {t.status === 'PLANNED' && <span className="badge badge-planned">Запланирован</span>}
                      {t.status === 'CURRENT' && <span className="badge badge-paid">Действует</span>}
                      {t.status === 'PAST' && <span className="muted">Прошлый</span>}
                    </span>
                    {t.status === 'PLANNED' && (
                      <button type="button" className="link-danger" onClick={() => cancel(t, service)}>Отменить</button>
                    )}
                  </div>
                ))}
              </div>
            </section>
          );
        })}
      </div>
    </>
  );
}
