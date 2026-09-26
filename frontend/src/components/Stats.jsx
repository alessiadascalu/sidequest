import { AnimatePresence, motion, useAnimate } from 'framer-motion'
import { useEffect, useRef, useState } from 'react'
import AnimatedNumber from './AnimatedNumber.jsx'

const EASE = [0.22, 1, 0.36, 1]

export default function Stats({ profile }) {
  return (
    <section className="stats">
      <StreakCard streak={profile.streak} />
      <XpCard level={profile.level} />
    </section>
  )
}

function StreakCard({ streak }) {
  const alive = streak.current > 0
  return (
    <motion.div
      className={`card streak-card ${alive ? 'alive' : ''}`}
      title={`Cel mai lung streak: ${streak.longest} zile`}
      whileHover={{ y: -3 }}
    >
      <motion.span
        key={streak.current} // la fiecare zi nouă de streak, flacăra face "pop"
        className={`flame ${alive ? '' : 'dead'}`}
        initial={{ scale: 0.4, rotate: -20 }}
        animate={
          alive
            ? { scale: [1.35, 1, 1.08, 1], rotate: [0, -4, 4, 0], transition: { duration: 0.8 } }
            : { scale: 1, rotate: 0 }
        }
        aria-hidden="true"
      >
        🔥
      </motion.span>
      <div>
        <div className="big-number">
          <AnimatedNumber value={streak.current} duration={0.6} />
        </div>
        <div className="stat-label">{streak.current === 1 ? 'zi la rând' : 'zile la rând'}</div>
        <div className="small" style={{ color: 'var(--faint)' }}>record: {streak.longest}</div>
      </div>
    </motion.div>
  )
}

function XpCard({ level }) {
  const [scope, animate] = useAnimate()
  const previous = useRef(null)
  const [gain, setGain] = useState(null)

  useEffect(() => {
    const fill = scope.current
    const from = previous.current
    previous.current = level
    const target = `${(level.progress * 100).toFixed(2)}%`

    if (!from) {
      animate(fill, { width: ['0%', target] }, { duration: 1.2, ease: EASE, delay: 0.25 })
      return
    }
    if (level.totalXp > from.totalXp) {
      setGain({ key: level.totalXp, amount: level.totalXp - from.totalXp })
    }
    if (level.level > from.level) {
      // Level up: bara se umple până la capăt, se golește, apoi urcă la progresul din nivelul nou.
      animate([
        [fill, { width: '100%' }, { duration: 0.7, ease: 'easeIn' }],
        [fill, { width: '0%' }, { duration: 0.01, at: '+0.25' }],
        [fill, { width: target }, { duration: 0.9, ease: EASE }],
      ])
    } else {
      animate(fill, { width: target }, { duration: 1, ease: EASE })
    }
  }, [level, animate, scope])

  return (
    <motion.div className="card xp-card" whileHover={{ y: -3 }}>
      <div className="xp-head">
        <span className="level-badge">
          LVL <span className="gradient-text">{level.level}</span>
        </span>
        <span className="level-title">{level.title}</span>
      </div>

      <div
        className="xp-bar"
        role="progressbar"
        aria-label="Progres către nivelul următor"
        aria-valuemin={0}
        aria-valuemax={level.xpForNextLevel}
        aria-valuenow={level.xpIntoLevel}
      >
        <div ref={scope} className="xp-fill" style={{ width: 0 }} />
      </div>

      <div className="xp-foot" style={{ position: 'relative' }}>
        <span>
          <AnimatedNumber value={level.xpIntoLevel} /> / {level.xpForNextLevel} XP
        </span>
        <span>
          total <AnimatedNumber value={level.totalXp} />
        </span>
        <AnimatePresence>
          {gain && (
            <motion.strong
              key={gain.key}
              initial={{ opacity: 0, y: 0, scale: 0.6 }}
              animate={{ opacity: [0, 1, 1, 0], y: -46, scale: 1.1 }}
              transition={{ duration: 1.8, ease: 'easeOut' }}
              onAnimationComplete={() => setGain(null)}
              style={{
                position: 'absolute',
                right: 0,
                top: 0,
                color: 'var(--lime)',
                fontFamily: 'var(--font-display)',
                fontSize: '1.1rem',
                pointerEvents: 'none',
              }}
            >
              +{gain.amount} XP
            </motion.strong>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  )
}
