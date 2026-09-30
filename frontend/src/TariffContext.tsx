import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from './api';
import { useAuth } from './auth/AuthContext';
import type { Tariff } from './models';

interface TariffValue {
  tariffs: Tariff[];
  currentPrice: (serviceType: string) => number | undefined;
  reload: () => void;
}

const TariffContext = createContext<TariffValue | null>(null);

// Единые тарифы, загруженные с бэкенда. Нужны таблицам абонентов (колонка «Тариф»)
// и странице тарифов директора. Загружаются после входа, после изменения — reload().
export function TariffProvider({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth();
  const [tariffs, setTariffs] = useState<Tariff[]>([]);

  const reload = useCallback(() => {
    api.getTariffs().then(setTariffs).catch(err => console.error(err));
  }, []);

  useEffect(() => {
    if (isLoggedIn) reload();
    else setTariffs([]);
  }, [isLoggedIn, reload]);

  const value = useMemo<TariffValue>(() => {
    // Действующая цена по каждой услуге: { 'gas': 13.5, ... }
    const prices: Record<string, number> = {};
    for (const t of tariffs) {
      if (t.status === 'CURRENT') prices[t.serviceType] = t.price;
    }
    return { tariffs, currentPrice: s => prices[s], reload };
  }, [tariffs, reload]);

  return <TariffContext.Provider value={value}>{children}</TariffContext.Provider>;
}

export function useTariffs(): TariffValue {
  const value = useContext(TariffContext);
  if (!value) throw new Error('useTariffs должен вызываться внутри <TariffProvider>');
  return value;
}
