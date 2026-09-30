// Форматирование дат для отображения

// '2026-09' → 'Сентябрь 2026'
export function formatPeriod(period: string): string {
  const [year, month] = period.split('-').map(Number);
  const name = new Date(year, month - 1, 1).toLocaleString('ru-RU', { month: 'long' });
  return `${name[0].toUpperCase()}${name.slice(1)} ${year}`;
}

// '2026-09-10' → '10.09.2026'
export function formatDate(date: string): string {
  return date.split('-').reverse().join('.');
}

// '2026-09-30T12:39:22Z' → '30.09.2026, 18:39' (по времени браузера)
export function formatDateTime(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  });
}

// 320 → '5 ч 20 мин'
export function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h} ч ${m} мин` : `${m} мин`;
}

const pad = (n: number) => String(n).padStart(2, '0');

// Сегодняшняя дата в формате '2026-09-30' (по времени браузера)
export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

// Текущий месяц в формате '2026-09'
export function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}
