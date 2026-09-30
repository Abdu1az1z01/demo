// Список коммунальных услуг в левом меню. Чтобы добавить/убрать/переименовать услугу — меняйте здесь.
// id должен совпадать с service_type в базе данных на бэкенде.
export interface UtilityService {
  id: string;
  name: string;
  icon: string;
  unit: string;
}

export const SERVICES: UtilityService[] = [
  { id: 'cold-water', name: 'Холодная вода/Стоки', icon: '💧', unit: 'м³' },
  { id: 'hot-water', name: 'Горячая вода', icon: '🚿', unit: 'м³' },
  { id: 'heating', name: 'Отопление', icon: '🔥', unit: 'Гкал' },
  { id: 'garbage', name: 'Вывоз ТБО (мусор)', icon: '🗑️', unit: 'чел.' },
  { id: 'gas', name: 'Газ', icon: '⛽', unit: 'м³' }
];

export function findService(id: string | null | undefined): UtilityService | undefined {
  return SERVICES.find(s => s.id === id);
}
