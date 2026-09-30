import { useCallback, useEffect, useRef, useState, type UIEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, describeError } from '../api';
import type { Subscriber } from '../models';
import { findService } from '../services';
import { useTariffs } from '../TariffContext';
import Messages from '../components/Messages';
import SubscriberFormModal from '../components/SubscriberFormModal';

// Сколько строк показывать сразу и сколько добавлять при прокрутке вниз
const CHUNK_SIZE = 30;

// Страница «Абоненты предприятия»: таблица с поиском и прокруткой (строки подгружаются по мере прокрутки).
// Карандаш слева при наведении на абонента открывает окно редактирования
// (данные, статусы оплаты по месяцам, удаление).
export default function SubscribersPage() {
  const { serviceId } = useParams();
  const service = findService(serviceId);
  // При переключении услуги в меню страница создаётся заново (key) — поиск и сообщения сбрасываются
  return service
    ? <SubscribersTable key={service.id} serviceId={service.id} />
    : (
      <section className="card empty-card">
        <div className="empty-title">Такой услуги нет</div>
        <div className="muted">Выберите услугу в меню слева</div>
      </section>
    );
}

function SubscribersTable({ serviceId }: { serviceId: string }) {
  const service = findService(serviceId)!;
  const navigate = useNavigate();
  const { currentPrice } = useTariffs();

  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [searchAllServices, setSearchAllServices] = useState(false);   // галочка «Искать во всех услугах»
  const [visibleCount, setVisibleCount] = useState(CHUNK_SIZE);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  // Окно добавления/редактирования: null — закрыто, 'new' — новый абонент, иначе — редактируемый
  const [formTarget, setFormTarget] = useState<Subscriber | 'new' | null>(null);
  const requestId = useRef(0);

  // Поиск запускается сам через 300 мс после того, как пользователь перестал печатать
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(searchQuery.trim()), 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const load = useCallback(async () => {
    const id = ++requestId.current;   // ответ на устаревший запрос (поиск уже изменился) игнорируем
    setIsLoading(true);
    setError('');
    try {
      const list = searchAllServices
        ? await api.searchAll(debouncedQuery)
        : await api.getSubscribers(serviceId, debouncedQuery);
      if (id !== requestId.current) return;
      setSubscribers(list);
      setVisibleCount(CHUNK_SIZE);
    } catch (err) {
      if (id !== requestId.current) return;
      console.error(err);
      setSubscribers([]);
      setError('Не удалось загрузить абонентов. ' + describeError(err));
    }
    setIsLoading(false);
  }, [serviceId, debouncedQuery, searchAllServices]);

  useEffect(() => { load(); }, [load]);

  const visibleRows = subscribers.slice(0, visibleCount);
  const hasMore = visibleCount < subscribers.length;

  // Прокрутили таблицу почти до конца — показываем следующие строки
  const onTableScroll = (e: UIEvent<HTMLElement>) => {
    const el = e.currentTarget;
    if (hasMore && el.scrollTop + el.clientHeight >= el.scrollHeight - 200) {
      setVisibleCount(n => n + CHUNK_SIZE);
    }
  };

  const editTarget = formTarget === 'new' ? null : formTarget;
  // Услуга абонента (нужна, когда ищем во всех услугах)
  const formService = (editTarget && findService(editTarget.serviceType)) || service;

  const onSaved = (s: Subscriber) => {
    const isNew = formTarget === 'new';
    setFormTarget(null);
    setSuccess(isNew ? `Абонент «${s.ownerName}» добавлен.` : `Данные абонента «${s.ownerName}» сохранены.`);
    load();
  };

  // Статус оплаты изменили в окне редактирования — обновляем долг в таблице
  const onStatusChanged = (updated: Subscriber) =>
    setSubscribers(list => list.map(s => (s.id === updated.id ? updated : s)));

  const onDeleted = () => {
    if (editTarget) setSuccess(`Абонент «${editTarget.ownerName}» удалён.`);
    setFormTarget(null);
    load();
  };

  const columns = searchAllServices ? 9 : 8;

  return (
    <>
      <div className="page-head">
        <div>
          <h2 className="page-title">{service.icon} {service.name}</h2>
          <p className="subtitle">
            {searchAllServices
              ? `Поиск во всех услугах · найдено: ${subscribers.length}`
              : `Абоненты предприятия · всего: ${subscribers.length}`}
          </p>
        </div>

        <div className="tools">
          <div className="search-box">
            <input
              type="search"
              className="input"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="🔍 Поиск по ФИО, лицевому счёту, телефону или адресу"
            />
            <label className="check">
              <input type="checkbox" checked={searchAllServices} onChange={e => setSearchAllServices(e.target.checked)} />
              Искать во всех услугах
            </label>
          </div>
          <button type="button" className="btn btn-primary add-btn" onClick={() => setFormTarget('new')}>+ Добавить абонента</button>
        </div>
      </div>

      <Messages success={success} error={error} />

      {/* Таблица абонентов: прокручивается внутри карточки, заголовок остаётся на месте.
          Нажатие на строку — история начислений; карандаш слева (при наведении) — редактирование. */}
      <section className="card table-scroll" onScroll={onTableScroll}>
        <table className="table table-compact">
          <thead>
            <tr>
              <th className="edit-col"></th>
              <th>№</th>
              {searchAllServices && <th>Услуга</th>}
              <th>ФИО</th>
              <th>Лицевой счёт</th>
              <th>Телефон</th>
              <th>Адрес</th>
              <th>Тариф</th>
              <th className="text-right">Долг</th>
            </tr>
          </thead>
          <tbody>
            {visibleRows.map((sub, i) => {
              const subService = findService(sub.serviceType);
              return (
                <tr
                  key={sub.id}
                  className="clickable hover-row"
                  onClick={() => navigate(`/services/${sub.serviceType}/subscribers/${sub.id}`)}
                  title="Открыть историю начислений"
                >
                  <td className="edit-col" onClick={e => e.stopPropagation()}>
                    <button type="button" className="hover-edit" title="Редактировать абонента" onClick={() => setFormTarget(sub)}>✏️</button>
                  </td>
                  <td className="muted">{i + 1}</td>
                  {searchAllServices && <td className="service-cell" title={subService?.name ?? ''}>{subService?.icon}</td>}
                  <td className="name">👤 {sub.ownerName}</td>
                  <td className="nowrap">{sub.accountNumber}</td>
                  <td className="nowrap">{sub.phone || '—'}</td>
                  <td className="address">{sub.address}</td>
                  <td className="nowrap">{currentPrice(sub.serviceType) ?? '—'} сом/{subService?.unit}</td>
                  <td className="text-right nowrap">
                    {sub.debt > 0 ? <span className="debt">{sub.debt} сом</span> : <span className="paid">Оплачено</span>}
                  </td>
                </tr>
              );
            })}
            {subscribers.length === 0 && !isLoading && !error && (
              <tr><td colSpan={columns} className="muted empty-row">Никого не найдено</td></tr>
            )}
          </tbody>
        </table>
        {hasMore && <div className="more muted">Прокрутите вниз, чтобы увидеть ещё…</div>}
      </section>

      {/* Окно «Добавить / Редактировать абонента» */}
      {formTarget && (
        <SubscriberFormModal
          service={formService}
          subscriber={editTarget}
          onSaved={onSaved}
          onStatusChanged={onStatusChanged}
          onDeleted={onDeleted}
          onClose={() => setFormTarget(null)}
        />
      )}
    </>
  );
}
