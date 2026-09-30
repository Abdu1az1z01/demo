import type { FormEvent, ReactNode } from 'react';

// Всплывающее окно поверх страницы. Нажатие на затемнённый фон закрывает окно.
// Если передан onSubmit — окно становится формой (Enter = «Сохранить»).
export default function Modal({ onClose, onSubmit, className = '', children }: {
  onClose: () => void;
  onSubmit?: () => void;
  className?: string;
  children: ReactNode;
}) {
  const stop = (e: { stopPropagation: () => void }) => e.stopPropagation();
  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit?.();
  };

  return (
    <div className="backdrop" onClick={onClose}>
      {onSubmit ? (
        <form className={`card modal ${className}`} onClick={stop} onSubmit={submit}>{children}</form>
      ) : (
        <div className={`card modal ${className}`} onClick={stop}>{children}</div>
      )}
    </div>
  );
}
