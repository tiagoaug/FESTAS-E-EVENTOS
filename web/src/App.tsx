import { HashRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/authContextValue'
import BottomNav from './components/BottomNav'
import Login from './pages/Login'
import { usePartyStore } from './store/usePartyStore'
import Dashboard from './pages/Dashboard'
import EventSetup from './pages/EventSetup'
import Participants from './pages/Participants'
import Expenses from './pages/Expenses'
import Payments from './pages/Payments'
import Invitations from './pages/Invitations'

function AuthenticatedApp({ uid }: { uid: string }) {
  const store = usePartyStore(uid)

  return (
    <>
      <Routes>
        <Route path="/" element={<Dashboard store={store} />} />
        <Route path="/event-setup" element={<EventSetup store={store} />} />
        <Route path="/participants" element={<Participants store={store} />} />
        <Route path="/expenses" element={<Expenses store={store} />} />
        <Route path="/payments" element={<Payments store={store} />} />
        <Route path="/invitations" element={<Invitations store={store} />} />
      </Routes>
      <BottomNav />
    </>
  )
}

function AppShell() {
  const { user, loading } = useAuth()

  return (
    <div className="app-shell">
      {loading ? (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <p style={{ color: 'var(--on-surface-variant)' }}>Carregando...</p>
        </div>
      ) : user ? (
        <AuthenticatedApp uid={user.uid} />
      ) : (
        <Login />
      )}
    </div>
  )
}

function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <AppShell />
      </HashRouter>
    </AuthProvider>
  )
}

export default App
