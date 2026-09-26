import { AnimatePresence, motion } from 'framer-motion'
import { useEffect } from 'react'
import { celebrateLevelUp } from '../confetti.js'
import Mascot from './Mascot.jsx'

/** "+35 XP" care sare pe ecran după un quest completat, cu detaliul recompensei. */
export function RewardPop({ reward, onDone }) {
  useEffect(() => {
    if (!reward) return
    const timer = setTimeout(onDone, 2600)
    return () => clearTimeout(timer)
  }, [reward, onDone])

  return (
    <AnimatePresence>
      {reward && (
        <motion.div
          key={reward.assignmentId}
          className="reward-pop"
          role="status"
          initial={{ opacity: 0, scale: 0.3, x: '-50%', y: 40 }}
          animate={{ opacity: 1, scale: 1, x: '-50%', y: 0 }}
          exit={{ opacity: 0, scale: 0.8, x: '-50%', y: -60 }}
          transition={{ type: 'spring', stiffness: 380, damping: 16 }}
        >
          <span className="display gradient-text">+{reward.xpAwarded} XP</span>
          <span className="muted small">
            {reward.xpBreakdown.map((line) => `${line.source} +${line.xp}`).join(' · ')}
          </span>
          {reward.streak.current > 1 && (
            <strong style={{ color: 'var(--orange)' }}>🔥 {reward.streak.current} zile la rând!</strong>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  )
}

const WORD = 'LEVEL UP!'.split('')

/** Ecranul de level-up: raze care se rotesc, literele sar pe rând, nivelul nou intră cu spin. */
export function LevelUpOverlay({ level, onClose }) {
  useEffect(() => {
    if (!level) return
    celebrateLevelUp()
    const onKey = (e) => (e.key === 'Escape' || e.key === 'Enter') && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [level, onClose])

  return (
    <AnimatePresence>
      {level && (
        <motion.div
          className="levelup"
          role="dialog"
          aria-modal="true"
          aria-label={`Nivel nou: ${level.level}, ${level.title}`}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0, transition: { duration: 0.3 } }}
          onClick={onClose}
        >
          <motion.div
            className="levelup-rays"
            animate={{ rotate: 360 }}
            transition={{ duration: 30, repeat: Infinity, ease: 'linear' }}
          />

          <div className="levelup-content">
            <Mascot size={96} mood="hype" />

            <div className="levelup-word gradient-text" aria-hidden="true">
              {WORD.map((ch, i) => (
                <motion.span
                  key={i}
                  initial={{ y: 80, opacity: 0, rotate: -30 }}
                  animate={{ y: 0, opacity: 1, rotate: 0 }}
                  transition={{ type: 'spring', stiffness: 500, damping: 14, delay: 0.15 + i * 0.05 }}
                  style={{ whiteSpace: 'pre' }}
                >
                  {ch}
                </motion.span>
              ))}
            </div>

            <motion.div
              className="levelup-level"
              initial={{ scale: 0, rotate: -270 }}
              animate={{ scale: 1, rotate: 0 }}
              transition={{ type: 'spring', stiffness: 200, damping: 12, delay: 0.6 }}
            >
              {level.level}
            </motion.div>

            <motion.p
              className="levelup-title"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 1 }}
            >
              Acum ești „{level.title}”
            </motion.p>

            <motion.button
              className="btn btn-primary"
              onClick={onClose}
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 1.3, type: 'spring' }}
              whileHover={{ scale: 1.08 }}
              whileTap={{ scale: 0.9 }}
              autoFocus
            >
              Let&apos;s gooo 🚀
            </motion.button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
