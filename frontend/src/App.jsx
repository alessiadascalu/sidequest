import { useCallback, useState } from 'react'
import Dashboard from './components/Dashboard.jsx'
import Onboarding from './components/Onboarding.jsx'

const STORAGE_KEY = 'sidequest.userId'

function readStoredUserId() {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null // localStorage indisponibil (mod privat strict etc.)
  }
}

function storeUserId(id) {
  try {
    if (id == null) localStorage.removeItem(STORAGE_KEY)
    else localStorage.setItem(STORAGE_KEY, String(id))
  } catch {
    // ignorat: aplicația merge și fără persistență
  }
}

export default function App() {
  const [userId, setUserId] = useState(readStoredUserId)

  const handleCreated = useCallback((id) => {
    storeUserId(id)
    setUserId(String(id))
  }, [])

  const handleForget = useCallback(() => {
    storeUserId(null)
    setUserId(null)
  }, [])

  return (
    <main className="app">
      <header className="app-header">
        <h1>SideQuest</h1>
        <p className="muted">O mini-misiune pe zi.</p>
      </header>

      {userId ? (
        <Dashboard userId={userId} onForget={handleForget} />
      ) : (
        <Onboarding onCreated={handleCreated} />
      )}
    </main>
  )
}
