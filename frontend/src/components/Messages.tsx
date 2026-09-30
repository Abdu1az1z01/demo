// Сообщения об успехе (зелёное) и ошибке (красное) под заголовком страницы
export default function Messages({ success, error }: { success?: string; error?: string }) {
  return (
    <>
      {success && <p className="msg msg-success">✅ {success}</p>}
      {error && <p className="msg msg-error">⚠️ {error}</p>}
    </>
  );
}
