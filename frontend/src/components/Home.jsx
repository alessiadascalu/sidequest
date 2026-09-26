import { AnimatePresence, motion } from 'framer-motion'
import { useCallback, useEffect, useState } from 'react'
import { ApiError, completeTodayQuest, getProfile, getTodayQuest } from '../api.js'
import { celebrateQuest } from '../confetti.js'
import { LevelUpOverlay, RewardPop } from './Celebrations.jsx'
import HistoryTab from './HistoryTab.jsx'
import Lightbox from './Lightbox.jsx'
import Mascot from './Mascot.jsx'
import ProofSheet from './ProofSheet.jsx'
import QuestCard from './QuestCard.jsx'
import Stats from './Stats.jsx'

const TABS = [
  { id: 'today', label: '⚡ Azi' },
  { id: 'history', label: '📜 Istoric' },
]

export default function Home({ userId, initialProfile, welcome, onLogout }) {
  const [tab, setTab] = useState('today')
  const [profile, setProfile] = useState(initialProfile ?? null)
  const [today, setToday] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  const [sheetOpen, setSheetOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [reward, setReward] = useState(null)
  const [levelUp, setLevelUp] = useState(null)
  const [image, setImage] = useState(null)
  const [toast, setToast] = useState(welcome ?? null)

  useEffect(() => {
    let cancelled = false
    Promise.all([getTodayQuest(userId), getProfile(userId)]).then(
      ([q, p]) => {
        if (cancelled) return
        setToday(q)
        setProfile(p)
        setError(null)
      },
      (e) => {
        if (cancelled) return
        // Utilizatorul salvat local nu mai există (ex. baza de date a fost ștearsă): înapoi la login.
        if (e instanceof ApiError && e.status === 404) onLogout()
        else setError(e.message)
      },
    )
    return () => {
      cancelled = true
    }
  }, [userId, onLogout, reloadKey])

  // Dacă tab-ul a rămas deschis peste miezul nopții, la revenire luăm quest-ul zilei noi.
  useEffect(() => {
    const onVisible = () => document.visibilityState === 'visible' && reload()
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [reload])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 3500)
    return () => clearTimeout(timer)
  }, [toast])

  const closeSheet = useCallback(() => {
    setSheetOpen(false)
    setSubmitError(null)
  }, [])
  const clearReward = useCallback(() => setReward(null), [])
  const closeLevelUp = useCallback(() => setLevelUp(null), [])
  const closeImage = useCallback(() => setImage(null), [])

  async function handleComplete(proof) {
    setSubmitting(true)
    setSubmitError(null)
    try {
      const result = await completeTodayQuest(userId, proof)
      setSheetOpen(false)
      setToday((q) => ({ ...q, completed: true, completedAt: result.completedAt, streak: result.streak, proof: result.proof }))
      setProfile((p) => ({
        ...p,
        totalXp: result.level.totalXp,
        completedQuests: p.completedQuests + 1,
        level: result.level,
        streak: result.streak,
      }))
      celebrateQuest()
      setReward(result)
      if (result.leveledUp) setTimeout(() => setLevelUp(result.level), 1400)
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        // completat deja în alt tab
        setSheetOpen(false)
        setToast({ emoji: '👀', text: e.message })
        reload()
      } else {
        setSubmitError(e.message)
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!today || !profile) {
    return error ? (
      <div className="error-banner" role="alert">
        {error}
        <button className="btn btn-ghost" onClick={reload}>
          Reîncearcă
        </button>
      </div>
    ) : (
      <div className="loading">
        <Mascot size={80} mood="idle" />
        <p className="muted">Se caută misiunea zilei…</p>
      </div>
    )
  }

  const mood = reward ? 'hype' : today.completed ? 'happy' : profile.streak.current === 0 ? 'sleepy' : 'idle'

  return (
    <motion.div className="screen" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <header className="topbar">
        <Mascot size={56} mood={mood} />
        <div className="topbar-hello">
          <div className="small muted">Salut,</div>
          <div className="display">@{profile.username}</div>
        </div>
        <motion.button
          className="icon-btn"
          onClick={onLogout}
          whileHover={{ rotate: 15 }}
          whileTap={{ scale: 0.85 }}
          aria-label="Schimbă utilizatorul"
          title="Schimbă utilizatorul"
        >
          ⏏
        </motion.button>
      </header>

      <nav className="tabs" role="tablist" aria-label="Secțiuni">
        {TABS.map((t) => (
          <motion.button
            key={t.id}
            className="tab"
            role="tab"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            whileTap={{ scale: 0.94 }}
          >
            {tab === t.id && (
              <motion.span layoutId="tab-pill" className="tab-pill" transition={{ type: 'spring', stiffness: 400, damping: 32 }} />
            )}
            <span className="tab-label">{t.label}</span>
          </motion.button>
        ))}
      </nav>

      <AnimatePresence mode="wait" initial={false}>
        <motion.div
          key={tab}
          className="screen"
          initial={{ opacity: 0, x: tab === 'history' ? 40 : -40 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: tab === 'history' ? -40 : 40 }}
          transition={{ duration: 0.25, ease: 'easeOut' }}
        >
          {tab === 'today' ? (
            <>
              <QuestCard today={today} onComplete={() => setSheetOpen(true)} onOpenImage={setImage} />
              <Stats profile={profile} />
            </>
          ) : (
            <HistoryTab userId={userId} onOpenImage={setImage} />
          )}
        </motion.div>
      </AnimatePresence>

      <ProofSheet
        open={sheetOpen}
        onClose={closeSheet}
        onSubmit={handleComplete}
        submitting={submitting}
        error={submitError}
      />
      <RewardPop reward={reward} onDone={clearReward} />
      <LevelUpOverlay level={levelUp} onClose={closeLevelUp} />
      <Lightbox image={image} onClose={closeImage} />

      <AnimatePresence>
        {toast && (
          <motion.div
            className="toast"
            role="status"
            initial={{ opacity: 0, y: 40, x: '-50%', scale: 0.9 }}
            animate={{ opacity: 1, y: 0, x: '-50%', scale: 1 }}
            exit={{ opacity: 0, y: 40, x: '-50%' }}
            transition={{ type: 'spring', stiffness: 400, damping: 26 }}
          >
            <span aria-hidden="true">{toast.emoji}</span>
            {toast.text}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}
