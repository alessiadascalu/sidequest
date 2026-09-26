import { motion } from 'framer-motion'
import AnimatedNumber from './AnimatedNumber.jsx'

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #8b5cff, #ff4fd8)',
  'linear-gradient(135deg, #22c3ff, #3dffb0)',
  'linear-gradient(135deg, #ff8a3d, #ff4f9a)',
  'linear-gradient(135deg, #3dffb0, #fff36b)',
  'linear-gradient(135deg, #b14dff, #22c3ff)',
  'linear-gradient(135deg, #ff4fd8, #fff36b)',
]

/** Aceeași culoare pentru același username, de fiecare dată. */
function avatarGradient(username) {
  let hash = 0
  for (const ch of username) hash = (hash * 31 + ch.codePointAt(0)) | 0
  return AVATAR_GRADIENTS[Math.abs(hash) % AVATAR_GRADIENTS.length]
}

const MEDAL = { 2: '🥈', 3: '🥉' }

const list = { hidden: {}, show: { transition: { staggerChildren: 0.07, delayChildren: 0.05 } } }
const row = {
  hidden: { opacity: 0, y: 24, scale: 0.95 },
  show: { opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 320, damping: 24 } },
}

export default function Leaderboard({ entries, currentUserId }) {
  return (
    <motion.ol className="leaderboard" variants={list} initial="hidden" animate="show">
      {entries.map((entry) => (
        <LeaderboardRow key={entry.userId} entry={entry} isMe={String(entry.userId) === String(currentUserId)} />
      ))}
    </motion.ol>
  )
}

function LeaderboardRow({ entry, isMe }) {
  const first = entry.rank === 1
  const classes = ['lb-row', 'card', first && 'lb-first', isMe && 'lb-me'].filter(Boolean).join(' ')

  return (
    <motion.li className={classes} variants={row} layout whileHover={{ scale: 1.015 }}>
      <div className="lb-rank" aria-label={`Locul ${entry.rank}`}>
        {first ? (
          <motion.span
            className="lb-trophy"
            animate={{ rotate: [0, -12, 12, -6, 0], scale: [1, 1.15, 1] }}
            transition={{ duration: 1.6, repeat: Infinity, repeatDelay: 1.4 }}
            aria-hidden="true"
          >
            🏆
          </motion.span>
        ) : (
          (MEDAL[entry.rank] ?? <span className="lb-rank-num">{entry.rank}</span>)
        )}
      </div>

      <div className="lb-avatar-wrap">
        {first && (
          <motion.span
            className="lb-crown"
            aria-hidden="true"
            initial={{ y: -14, opacity: 0, rotate: -30 }}
            animate={{ y: [0, -4, 0], opacity: 1, rotate: -12 }}
            transition={{ y: { duration: 2, repeat: Infinity, ease: 'easeInOut' }, opacity: { delay: 0.4 } }}
          >
            👑
          </motion.span>
        )}
        <span className="lb-avatar" style={{ background: avatarGradient(entry.username) }} aria-hidden="true">
          {entry.username.charAt(0).toUpperCase()}
        </span>
      </div>

      <div className="lb-who">
        <div className="lb-name">
          <span className="lb-username">@{entry.username}</span>
          {isMe && <span className="lb-you">tu</span>}
        </div>
        <div className="lb-level">
          LVL {entry.level} · {entry.title}
        </div>
      </div>

      <div className="lb-stats">
        <div className="lb-xp">
          <AnimatedNumber value={entry.totalXp} /> <small>XP</small>
        </div>
        <div className={`lb-streak ${entry.streak > 0 ? '' : 'off'}`} title={entry.completedToday ? 'A completat quest-ul de azi' : undefined}>
          🔥 {entry.streak}
          {entry.completedToday && <span className="lb-today"> ✓</span>}
        </div>
      </div>
    </motion.li>
  )
}
