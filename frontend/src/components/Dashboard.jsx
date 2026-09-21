import { useCallback, useEffect, useState } from 'react'
import { ApiError, completeTodayQuest, getProfile, getTodayQuest } from '../api.js'
import { celebrate } from '../confetti.js'
import QuestCard from './QuestCard.jsx'
import Stats from './Stats.jsx'

export default function Dashboard({ userId, onForget }) {
  const [quest, setQuest] = useState(null)
  const [profile, setProfile] = useState(null)
  const [reward, setReward] = useState(null)
  const [error, setError] = useState(null)
  const [completing, setCompleting] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  useEffect(() => {
    let cancelled = false
    Promise.all([getTodayQuest(userId), getProfile(userId)]).then(
      ([q, p]) => {
        if (cancelled) return
        setQuest(q)
        setProfile(p)
        setError(null)
      },
      (e) => {
        if (cancelled) return
        // Utilizatorul salvat local nu mai există (ex. baza de date a fost ștearsă): reia onboarding-ul.
        if (e instanceof ApiError && e.status === 404) onForget()
        else setError(e.message)
      },
    )
    return () => {
      cancelled = true
    }
  }, [userId, onForget, reloadKey])

  // Dacă tab-ul a rămas deschis peste miezul nopții, la revenire luăm quest-ul zilei noi.
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === 'visible') reload()
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [reload])

  async function handleComplete() {
    setCompleting(true)
    setError(null)
    try {
      const result = await completeTodayQuest(userId)
      setQuest((q) => ({ ...q, completed: true, completedAt: result.completedAt, streak: result.streak }))
      setProfile((p) => ({
        ...p,
        totalXp: result.level.totalXp,
        completedQuests: p.completedQuests + 1,
        level: result.level,
        streak: result.streak,
      }))
      setReward(result)
      celebrate({ levelUp: result.leveledUp })
    } catch (e) {
      setError(e.message)
      if (e instanceof ApiError && e.status === 409) reload() // completat deja în alt tab
    } finally {
      setCompleting(false)
    }
  }

  if (!quest || !profile) {
    return error ? (
      <p className="error" role="alert">{error} <button onClick={reload}>Reîncearcă</button></p>
    ) : (
      <p className="muted center">Se caută misiunea zilei…</p>
    )
  }

  return (
    <>
      <p className="greeting">
        Salut, <strong>{profile.username}</strong>! Misiunea ta pentru {formatDate(quest.date)}:
      </p>

      <QuestCard quest={quest} completing={completing} onComplete={handleComplete} />

      {reward && quest.completed && (
        <div className="card reward-toast" role="status">
          <strong>+{reward.xpAwarded} XP</strong>
          <span className="muted small">
            {reward.xpBreakdown.map((line) => `${line.source} +${line.xp}`).join(' · ')}
          </span>
          {reward.leveledUp && (
            <div className="level-up">🎉 Nivel nou: {reward.level.level}, „{reward.level.title}”!</div>
          )}
        </div>
      )}

      {error && <p className="error" role="alert">{error}</p>}

      <Stats profile={profile} />

      <button className="link" onClick={onForget}>Schimbă utilizatorul</button>
    </>
  )
}

function formatDate(isoDate) {
  // "2026-06-10" e o dată calendaristică, nu un moment: o construim din părți ca să nu apară
  // deplasări de fus orar (new Date("2026-06-10") ar fi interpretat ca UTC).
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long' })
}
