import { AnimatePresence, motion } from 'framer-motion'
import { useCallback, useEffect, useRef, useState } from 'react'
import { getLeaderboard, getUserGroups } from '../api.js'
import { celebrateQuest, celebrateTopSpot } from '../confetti.js'
import { CreateGroupForm, JoinGroupForm } from './GroupForms.jsx'
import InviteCodeCard, { CopyButton } from './InviteCode.jsx'
import Leaderboard from './Leaderboard.jsx'
import Mascot from './Mascot.jsx'

export default function GroupsTab({ userId, onToast }) {
  const [groups, setGroups] = useState(null)
  const [error, setError] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [adding, setAdding] = useState(false)
  const [fresh, setFresh] = useState(null) // grupul tocmai creat, cu codul afișat mare

  useEffect(() => {
    let cancelled = false
    getUserGroups(userId).then(
      (list) => {
        if (cancelled) return
        setGroups(list)
        setSelectedId((id) => id ?? list[0]?.id ?? null)
      },
      (e) => !cancelled && setError(e.message),
    )
    return () => {
      cancelled = true
    }
  }, [userId])

  const addGroup = useCallback((group) => {
    setGroups((list) => [...(list ?? []).filter((g) => g.id !== group.id), group])
    setSelectedId(group.id)
    setAdding(false)
  }, [])

  function handleCreated(group) {
    addGroup(group)
    setFresh(group)
    celebrateQuest()
  }

  function handleJoined(group) {
    addGroup(group)
    setFresh(null)
    onToast({ emoji: '🎟️', text: `Ai intrat în „${group.name}”! Vezi cum stai în clasament.` })
  }

  if (error) return <p className="error-banner" role="alert">{error}</p>
  if (!groups) {
    return (
      <div className="screen" aria-busy="true">
        <div className="skeleton" style={{ height: 48 }} />
        {[0, 1, 2].map((i) => (
          <div key={i} className="skeleton" style={{ height: 76 }} />
        ))}
      </div>
    )
  }

  if (groups.length === 0) {
    return (
      <div className="screen">
        <motion.div className="groups-hero" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
          <Mascot size={84} mood="happy" />
          <h2 className="display">
            Side-quest-urile sunt mai bune <span className="gradient-text">în gașcă</span>
          </h2>
          <p className="muted">Fă un grup cu prietenii și vedeți cine strânge mai mult XP.</p>
        </motion.div>
        <GroupForms userId={userId} onCreated={handleCreated} onJoined={handleJoined} />
      </div>
    )
  }

  const selected = groups.find((g) => g.id === selectedId) ?? groups[0]

  return (
    <div className="screen">
      <div className="group-chips" role="tablist" aria-label="Grupurile tale">
        {groups.map((g) => (
          <motion.button
            key={g.id}
            role="tab"
            aria-selected={g.id === selected.id}
            className="group-chip"
            onClick={() => {
              setSelectedId(g.id)
              setAdding(false)
            }}
            whileTap={{ scale: 0.92 }}
          >
            {g.id === selected.id && (
              <motion.span layoutId="group-chip-pill" className="group-chip-pill" transition={{ type: 'spring', stiffness: 400, damping: 32 }} />
            )}
            <span className="tab-label">
              {g.name} <small>· {g.memberCount}</small>
            </span>
          </motion.button>
        ))}
        <motion.button
          className={`group-chip add ${adding ? 'open' : ''}`}
          onClick={() => setAdding((a) => !a)}
          whileTap={{ scale: 0.9 }}
          animate={{ rotate: adding ? 45 : 0 }}
          aria-expanded={adding}
          aria-label={adding ? 'Închide' : 'Creează sau alătură-te unui grup'}
        >
          +
        </motion.button>
      </div>

      <AnimatePresence initial={false}>
        {adding && (
          <motion.div
            key="add"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            style={{ overflow: 'hidden' }}
          >
            <GroupForms userId={userId} onCreated={handleCreated} onJoined={handleJoined} />
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {fresh && fresh.id === selected.id && (
          <InviteCodeCard key={fresh.id} group={fresh} fresh onDismiss={() => setFresh(null)} />
        )}
      </AnimatePresence>

      <GroupBoard key={selected.id} group={selected} userId={userId} showCode={!fresh || fresh.id !== selected.id} />
    </div>
  )
}

function GroupForms({ userId, onCreated, onJoined }) {
  return (
    <div className="group-forms">
      <CreateGroupForm userId={userId} onCreated={onCreated} />
      <div className="or" aria-hidden="true">
        sau
      </div>
      <JoinGroupForm userId={userId} onJoined={onJoined} />
    </div>
  )
}

function GroupBoard({ group, userId, showCode }) {
  const [entries, setEntries] = useState(null)
  const [error, setError] = useState(null)
  const celebrated = useRef(false)

  useEffect(() => {
    let cancelled = false
    getLeaderboard(group.id).then(
      (list) => !cancelled && setEntries(list),
      (e) => !cancelled && setError(e.message),
    )
    return () => {
      cancelled = true
    }
  }, [group.id])

  // Momentul de glorie: dacă ești pe locul 1 într-un grup cu concurență, ploaie aurie.
  useEffect(() => {
    if (!entries || celebrated.current || entries.length < 2) return
    const me = entries.find((e) => String(e.userId) === String(userId))
    if (me?.rank === 1 && me.totalXp > 0) {
      celebrated.current = true
      const timer = setTimeout(celebrateTopSpot, 500)
      return () => clearTimeout(timer)
    }
  }, [entries, userId])

  const me = entries?.find((e) => String(e.userId) === String(userId))

  return (
    <section className="screen">
      <div className="card group-header">
        <div style={{ minWidth: 0 }}>
          <h2 className="display group-title">{group.name}</h2>
          <p className="muted small">
            {group.memberCount} {group.memberCount === 1 ? 'membru' : 'membri'}
            {me && ` · ești pe locul ${me.rank}`}
          </p>
        </div>
        {showCode && (
          <div className="group-code">
            <span className="code-mini">{group.inviteCode}</span>
            <CopyButton code={group.inviteCode} compact />
          </div>
        )}
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}
      {!entries && !error && [0, 1, 2].map((i) => <div key={i} className="skeleton" style={{ height: 76 }} />)}
      {entries && <Leaderboard entries={entries} currentUserId={userId} />}
      {entries?.length === 1 && (
        <p className="muted small center">Ești singur(ă) aici deocamdată. Trimite codul prietenilor ca să aveți concurență. 👀</p>
      )}
    </section>
  )
}
