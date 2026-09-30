import type { AuthSession, Bill, Employee, EmployeeForm, Subscriber, SubscriberForm, Tariff, WorkTime } from './models';

// Адрес бэкенда относительный: в готовом приложении сайт и API отдаёт один сервер (ZettaBilling),
// а при разработке (npm run dev) запросы /api перенаправляет vite.config.ts на http://localhost:8080
const API_URL = '/api';

// Ошибка запроса к бэкенду: код ответа (0 — бэкенд не отвечает) и текст от бэкенда
export class ApiError extends Error {
  status: number;
  url: string;

  constructor(status: number, message: string, url: string) {
    super(message);
    this.status = status;
    this.url = url;
  }
}

// Токен входа и что делать, когда бэкенд ответил 401 (задаёт AuthProvider)
let authToken: string | null = null;
let onUnauthorized: () => void = () => {};

export function setAuthToken(token: string | null): void {
  authToken = token;
}

export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const url = API_URL + path;
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

  let response: Response;
  try {
    response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, '', url);
  }

  if (!response.ok) {
    // Бэкенд присылает понятный текст ошибки в поле message (например «Лицевой счёт уже занят»)
    let message = '';
    try {
      message = (await response.json())?.message ?? '';
    } catch { /* ответ без JSON */ }
    // Токен устарел (например, после перезапуска бэкенда) — отправляем на страницу входа
    if (response.status === 401 && path !== '/auth/employee') {
      onUnauthorized();
    }
    throw new ApiError(response.status, message || response.statusText, url);
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

const query = (params: Record<string, string>) => '?' + new URLSearchParams(params).toString();

export const api = {
  // ===== Вход и выход =====
  login: (login: string, password: string) =>
    request<AuthSession>('POST', '/auth/employee', { login, password }),
  logout: () => request<void>('POST', '/auth/logout', {}),

  // ===== Абоненты =====
  // Список абонентов предприятия с поиском по ФИО, лицевому счёту или адресу
  getSubscribers: (serviceId: string, search: string) =>
    request<Subscriber[]>('GET', `/services/${serviceId}/subscribers` + query({ search })),
  // Поиск сразу во всех услугах
  searchAll: (search: string) => request<Subscriber[]>('GET', '/subscribers' + query({ search })),
  createSubscriber: (serviceId: string, form: SubscriberForm) =>
    request<Subscriber>('POST', `/services/${serviceId}/subscribers`, form),
  updateSubscriber: (id: number, form: SubscriberForm) => request<Subscriber>('PUT', `/subscribers/${id}`, form),
  deleteSubscriber: (id: number) => request<void>('DELETE', `/subscribers/${id}`),
  getSubscriber: (id: number) => request<Subscriber>('GET', `/subscribers/${id}`),
  // История начислений абонента (сначала новые)
  getBills: (subscriberId: number) => request<Bill[]>('GET', `/subscribers/${subscriberId}/bills`),
  submitReading: (subscriberId: number, reading: number) =>
    request<Subscriber>('POST', `/subscribers/${subscriberId}/readings`, { reading }),
  // Ручное изменение статуса оплаты (например, ошибка при оплате через банк), причина обязательна
  changeBillStatus: (billId: number, paid: boolean, note: string) =>
    request<Subscriber>('PUT', `/bills/${billId}/status`, { paid, note }),

  // ===== Тарифы (смотреть — все, менять — директор) =====
  getTariffs: () => request<Tariff[]>('GET', '/tariffs'),
  createTariff: (serviceType: string, price: number, effectiveFrom: string) =>
    request<Tariff>('POST', '/tariffs', { serviceType, price, effectiveFrom }),
  cancelTariff: (id: number) => request<void>('DELETE', `/tariffs/${id}`),

  // ===== Сотрудники (только директор) =====
  getEmployees: () => request<Employee[]>('GET', '/employees'),
  createEmployee: (form: EmployeeForm) => request<Employee>('POST', '/employees', form),
  updateEmployee: (id: number, form: EmployeeForm) => request<Employee>('PUT', `/employees/${id}`, form),
  getWorkTime: (employeeId: number, month: string) =>
    request<WorkTime>('GET', `/employees/${employeeId}/work-time` + query({ month }))
};

// Понятное описание ошибки запроса, чтобы сразу было видно, что не так с бэкендом
export function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 0) {
      return 'Бэкенд не отвечает. Запустите ZettaBilling (в IntelliJ или готовое приложение).';
    }
    if (err.status === 401 || err.status === 403) {
      return err.message || (err.status === 401 ? 'Требуется вход.' : 'Нет доступа.');
    }
    if (err.status === 404) {
      return err.message && err.message !== 'Not Found'
        ? err.message
        : `Бэкенд ответил 404 (${err.url}). Скорее всего, запущен старый бэкенд — перезапустите ZettaBilling.`;
    }
    // 400 / 409 и т.п.: бэкенд присылает понятный текст
    return err.message || `Ошибка бэкенда ${err.status}`;
  }
  return 'Неизвестная ошибка при обращении к бэкенду.';
}
