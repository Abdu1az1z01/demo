// Стартовая страница: пока предприятие не выбрано
export default function HomePage() {
  return (
    <section className="card empty-card">
      <div className="empty-icon">👈</div>
      <div className="empty-title">Выберите коммунальное предприятие</div>
      <div className="muted">Нажмите на услугу в меню слева, чтобы увидеть список абонентов</div>
    </section>
  );
}
