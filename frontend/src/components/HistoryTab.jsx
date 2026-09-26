import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { getHistory } from '../api.js'
import { categoryMeta, formatDay, formatTime } from '../quests.js'
import Mascot from './Mascot.jsx'
import ProofView from './ProofView.jsx'
import { CategoryChip, DifficultyChip } from './QuestCard.jsx'

/** Istoricul vine sortat descrescător; îl grupăm pe zile păstrând ordinea. */
function groupByDay(entries) {
  const groups = []
  for (const entry of entries) {
    const last = groups.at(-1)
    if (last?.date === entry.date) last.entries.push(entry)
    else groups.push({ date: entry.date, entries: [entry] })
  }
  return groups
}

const list = { hidden: {}, show: { transition: { staggerChildren: 0.06 } } }
const item = {
  hidden: { opacity: 0, x: -24, scale: 0.97 },
  show: { opacity: 1, x: 0, scale: 1, transition: { type: 'spring', stiffness: 300, damping: 26 } },
}

export default function HistoryTab({ userId, onOpenImage }) {
  const [entries, setEntries] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    getHistory(userId).then(
      (h) => !cancelled && setEntries(h),
      (e) => !cancelled && setError(e.message),
    )
    return () => {
      cancelled = true
    }
  }, [userId])

  if (error) return <p className="error-banner" role="alert">{error}</p>

  if (!entries) {
    return (
      <div className="screen" aria-busy="true">
        {[0, 1, 2].map((i) => (
          <div key={i} className="skeleton" />
        ))}
      </div>
    )
  }

  if (entries.length === 0) {
    return (
      <motion.div className="card empty" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <Mascot size={90} mood="sleepy" />
        <h2 className="display" style={{ fontSize: '1.3rem' }}>
          Nimic aici încă
        </h2>
        <p className="muted">Completează primul quest și începe-ți legenda. Istoricul apare aici, zi cu zi.</p>
      </motion.div>
    )
  }

  const totalXp = entries.reduce((sum, e) => sum + (e.xpAwarded ?? 0), 0)
  const withProof = entries.filter((e) => e.proof).length

  return (
    <div className="screen">
      <motion.div className="history-summary" variants={list} initial="hidden" animate="show">
        <MiniStat value={entries.length} label="quest-uri" />
        <MiniStat value={totalXp} label="XP câștigat" />
        <MiniStat value={withProof} label="cu dovadă" />
      </motion.div>

      {groupByDay(entries).map((group) => (
        <section key={group.date} className="day-group">
          <h3 className="day-label">{formatDay(group.date, { relative: true })}</h3>
          <motion.ol className="timeline" variants={list} initial="hidden" animate="show" style={{ margin: 0, listStyle: 'none' }}>
            {group.entries.map((entry) => (
              <HistoryEntry key={entry.assignmentId} entry={entry} onOpenImage={onOpenImage} />
            ))}
          </motion.ol>
        </section>
      ))}
    </div>
  )
}

function MiniStat({ value, label }) {
  return (
    <motion.div className="mini-stat" variants={item} whileHover={{ y: -3, scale: 1.03 }}>
      <div className="display gradient-text">{value}</div>
      <div className="small muted">{label}</div>
    </motion.div>
  )
}

function HistoryEntry({ entry, onOpenImage }) {
  const meta = categoryMeta(entry.quest.category)
  return (
    <motion.li
      className="card entry"
      variants={item}
      whileHover={{ x: 4 }}
      style={{ '--dot': meta.glow, '--chip-gradient': meta.gradient }}
    >
      <span className="entry-dot" aria-hidden="true" />
      <div className="entry-head">
        <motion.span className="cat-icon" whileHover={{ rotate: [0, -12, 12, 0], scale: 1.1 }} aria-hidden="true">
          {meta.icon}
        </motion.span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p className="entry-text">{entry.quest.text}</p>
          <div className="entry-meta">
            <CategoryChip quest={entry.quest} />
            <DifficultyChip difficulty={entry.quest.difficulty} />
            <span className="small muted">{formatTime(entry.completedAt)}</span>
            {entry.xpAwarded != null && <span className="entry-xp">+{entry.xpAwarded} XP</span>}
          </div>
        </div>
      </div>
      {entry.proof && (
        <ProofView proof={entry.proof} layoutKey={`history-${entry.assignmentId}`} onOpenImage={onOpenImage} />
      )}
    </motion.li>
  )
}
