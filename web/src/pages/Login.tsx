import { PartyPopper } from 'lucide-react'
import { useAuth } from '../context/authContextValue'

export default function Login() {
  const { signInWithGoogle, error } = useAuth()

  return (
    <div
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
        textAlign: 'center',
        gap: 16,
      }}
    >
      <div
        style={{
          width: 72,
          height: 72,
          borderRadius: 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'linear-gradient(155deg, #a98cf5 0%, #7657dd 100%)',
          color: 'white',
          boxShadow: '0 10px 24px -6px rgba(139,108,240,0.55)',
        }}
      >
        <PartyPopper size={34} strokeWidth={2} />
      </div>

      <div>
        <h1 style={{ margin: '0 0 6px', fontSize: '1.3rem', fontWeight: 800 }}>Festas & Eventos</h1>
        <p style={{ margin: 0, color: 'var(--on-surface-variant)', fontSize: '0.88rem' }}>
          Entre com sua conta Google para organizar seus eventos e sincronizar entre dispositivos.
        </p>
      </div>

      <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={signInWithGoogle}>
        <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
          <path
            fill="#fff"
            d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.9c1.7-1.57 2.7-3.88 2.7-6.62Z"
          />
          <path
            fill="#fff"
            d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.9-2.26c-.8.54-1.83.86-3.06.86-2.35 0-4.34-1.59-5.05-3.72H.9v2.33A9 9 0 0 0 9 18Z"
          />
          <path
            fill="#fff"
            d="M3.95 10.7A5.4 5.4 0 0 1 3.67 9c0-.59.1-1.17.28-1.7V4.97H.9A9 9 0 0 0 0 9c0 1.45.35 2.83.9 4.03l3.05-2.33Z"
          />
          <path
            fill="#fff"
            d="M9 3.58c1.32 0 2.51.46 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .9 4.97L3.95 7.3C4.66 5.17 6.65 3.58 9 3.58Z"
          />
        </svg>
        Entrar com Google
      </button>

      {error && (
        <p style={{ color: 'var(--danger)', fontSize: '0.8rem', maxWidth: 320 }}>{error}</p>
      )}
    </div>
  )
}
