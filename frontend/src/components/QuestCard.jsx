import { AnimatePresence, motion, useMotionValue, useSpring, useTransform } from 'framer-motion'
import { DIFFICULTY, categoryMeta, formatDay } from '../quests.js'
import ProofView from './ProofView.jsx'

export function CategoryChip({ quest }) {
  const meta = categoryMeta(quest.category)
  return (
    <span className="chip chip-cat" style={{ '--chip-gradient': meta.gradient }}>
      {meta.icon} {quest.categoryLabel}
    </span>
  )
}

export function DifficultyChip({ difficulty }) {
  const meta = DIFFICULTY[difficulty] ?? { label: difficulty, dots: 1 }
  return (
    <span className={`chip diff-${difficulty}`}>
      <span className="dots" aria-hidden="true">
        {[1, 2, 3].map((n) => (
          <i key={n} className={n <= meta.dots ? 'on' : ''} />
        ))}
      </span>
      {meta.label}
    </span>
  )
}

export default function QuestCard({ today, onComplete, onOpenImage }) {
  const { completed, xpReward, proof, date } = today
  const quest = today.quest
  const meta = categoryMeta(quest.category)

  // Tilt 3D discret după poziția pointerului.
  const px = useMotionValue(0.5)
  const py = useMotionValue(0.5)
  const rotateX = useSpring(useTransform(py, [0, 1], [6, -6]), { stiffness: 200, damping: 20 })
  const rotateY = useSpring(useTransform(px, [0, 1], [-6, 6]), { stiffness: 200, damping: 20 })

  function handlePointerMove(e) {
    if (e.pointerType !== 'mouse') return
    const rect = e.currentTarget.getBoundingClientRect()
    px.set((e.clientX - rect.left) / rect.width)
    py.set((e.clientY - rect.top) / rect.height)
  }

  function resetTilt() {
    px.set(0.5)
    py.set(0.5)
  }

  return (
    <motion.section
      className={`card quest ${completed ? 'is-done' : ''}`}
      style={{ '--quest-glow': meta.glow, rotateX, rotateY, transformPerspective: 900 }}
      onPointerMove={handlePointerMove}
      onPointerLeave={resetTilt}
      initial={{ opacity: 0, y: 30, scale: 0.96 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ type: 'spring', stiffness: 220, damping: 22 }}
      aria-live="polite"
    >
      <div className="quest-top">
        <span className="quest-date">Misiunea de {formatDay(date, { relative: true }).toLowerCase()}</span>
        <DifficultyChip difficulty={quest.difficulty} />
      </div>

      <div className="chips">
        <CategoryChip quest={quest} />
      </div>

      <p className="quest-text">{quest.text}</p>

      <AnimatePresence mode="wait" initial={false}>
        {completed ? (
          <motion.div
            key="done"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ type: 'spring', stiffness: 400, damping: 18 }}
            style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}
          >
            <div className="quest-done">
              <motion.span
                className="check-badge"
                initial={{ rotate: -180, scale: 0 }}
                animate={{ rotate: 0, scale: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 14, delay: 0.1 }}
              >
                ✓
              </motion.span>
              <span>Misiune îndeplinită! Revino mâine pentru următoarea.</span>
            </div>
            {proof && <ProofView proof={proof} layoutKey={`today-${today.assignmentId}`} onOpenImage={onOpenImage} />}
          </motion.div>
        ) : (
          <motion.div key="todo" className="quest-footer" exit={{ opacity: 0, y: 10 }}>
            <span className="xp-reward gradient-text">+{xpReward} XP</span>
            <motion.button
              className="btn btn-primary"
              onClick={onComplete}
              whileHover={{ scale: 1.05, rotate: -1 }}
              whileTap={{ scale: 0.9 }}
              animate={{ boxShadow: ['0 10px 30px -8px rgba(255,79,216,.5)', '0 10px 44px -4px rgba(255,79,216,.85)', '0 10px 30px -8px rgba(255,79,216,.5)'] }}
              transition={{ boxShadow: { duration: 2, repeat: Infinity } }}
            >
              ✓ Completed
            </motion.button>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.section>
  )
}
