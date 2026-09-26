import { AnimatePresence, MotionConfig, motion } from 'framer-motion'
import { useCallback, useState } from 'react'
import Home from './components/Home.jsx'
import Login from './components/Login.jsx'

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

function welcomeFor(login) {
  if (login.created) return { emoji: '✨', text: `Bun venit, ${login.username}! Prima misiune te așteaptă.` }
  const streak = login.streak.current
  return streak > 0
    ? { emoji: '🔥', text: `Bine ai revenit, ${login.username}! ${streak} ${streak === 1 ? 'zi' : 'zile'} la rând.` }
    : { emoji: '👋', text: `Bine ai revenit, ${login.username}!` }
}

export default function App() {
  // Sesiunea: id-ul salvat local + (după login) profilul primit direct de la POST /users.
  const [session, setSession] = useState(() => {
    const id = readStoredUserId()
    return id ? { userId: id } : null
  })

  const handleLoggedIn = useCallback((login) => {
    storeUserId(login.id)
    setSession({ userId: String(login.id), profile: login, welcome: welcomeFor(login) })
  }, [])

  const handleLogout = useCallback(() => {
    storeUserId(null)
    setSession(null)
  }, [])

  return (
    // reducedMotion="user": animațiile se reduc dacă sistemul cere "reduce motion".
    <MotionConfig reducedMotion="user">
      <main className="app">
        <AnimatePresence mode="wait">
          <motion.div
            key={session ? `home-${session.userId}` : 'login'}
            // fără filter/transform rămase după animație: ar strica position: fixed al overlay-urilor
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -24 }}
            transition={{ duration: 0.35, ease: 'easeOut' }}
          >
            {session ? (
              <Home
                userId={session.userId}
                initialProfile={session.profile}
                welcome={session.welcome}
                onLogout={handleLogout}
              />
            ) : (
              <Login onLoggedIn={handleLoggedIn} />
            )}
          </motion.div>
        </AnimatePresence>
      </main>
    </MotionConfig>
  )
}
