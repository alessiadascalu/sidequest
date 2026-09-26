import { motion } from 'framer-motion'
import { useId } from 'react'

// Questy, mascota SideQuest: un blob cu antenă-stea. Starea îi schimbă fața și mișcarea.
// Toate gurile au aceeași structură de path (M Q Q Z), ca Framer Motion să le poată interpola.
const MOUTHS = {
  idle: { d: 'M41 63 Q50 70 59 63 Q50 70 41 63 Z', fill: 0 },
  happy: { d: 'M39 61 Q50 77 61 61 Q50 66 39 61 Z', fill: 1 },
  hype: { d: 'M37 59 Q50 83 63 59 Q50 64 37 59 Z', fill: 1 },
  sleepy: { d: 'M44 66 Q50 63 56 66 Q50 63 44 66 Z', fill: 0 },
}

const BODY_MOTION = {
  idle: { y: [0, -4, 0], rotate: 0, transition: { duration: 2.6, repeat: Infinity, ease: 'easeInOut' } },
  happy: { y: [0, -16, 0, -8, 0], rotate: [0, -6, 6, 0], transition: { duration: 0.9, repeat: Infinity, repeatDelay: 1.2 } },
  hype: { y: [0, -22, 0], rotate: [0, 360], transition: { duration: 0.8, repeat: Infinity, repeatDelay: 0.6 } },
  sleepy: { y: [0, 2, 0], rotate: [-3, 3, -3], transition: { duration: 4, repeat: Infinity, ease: 'easeInOut' } },
}

export default function Mascot({ size = 72, mood = 'idle', className = '' }) {
  const id = useId()
  const mouth = MOUTHS[mood] ?? MOUTHS.idle
  const eyeScale = mood === 'sleepy' ? 0.35 : 1

  return (
    <motion.div
      className={className}
      style={{ width: size, height: size, display: 'inline-block', cursor: 'grab' }}
      animate={BODY_MOTION[mood] ?? BODY_MOTION.idle}
      whileHover={{ scale: 1.08, rotate: [0, -8, 8, -4, 0], transition: { duration: 0.5 } }}
      whileTap={{ scale: 0.85, rotate: 0 }}
      aria-hidden="true"
    >
      <svg viewBox="0 0 100 100" width={size} height={size} style={{ overflow: 'visible' }}>
        <defs>
          <linearGradient id={`${id}-body`} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#8b5cff" />
            <stop offset="55%" stopColor="#ff4fd8" />
            <stop offset="100%" stopColor="#ff8a3d" />
          </linearGradient>
          <filter id={`${id}-glow`} x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="5" />
          </filter>
        </defs>

        {/* antena cu stea */}
        <motion.g
          style={{ originX: '50px', originY: '26px' }}
          animate={{ rotate: [-10, 10, -10] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: 'easeInOut' }}
        >
          <path d="M50 26 Q48 16 52 10" stroke="#ff8a3d" strokeWidth="3" fill="none" strokeLinecap="round" />
          <path d="M52 1 L54.5 7.5 L61 10 L54.5 12.5 L52 19 L49.5 12.5 L43 10 L49.5 7.5 Z" fill="#fff36b" />
        </motion.g>

        {/* umbră colorată + corp */}
        <ellipse cx="50" cy="62" rx="36" ry="30" fill="#ff4fd8" opacity="0.45" filter={`url(#${id}-glow)`} />
        <path
          d="M50 24 C74 24 88 40 88 60 C88 80 72 92 50 92 C28 92 12 80 12 60 C12 40 26 24 50 24 Z"
          fill={`url(#${id}-body)`}
        />
        <ellipse cx="34" cy="38" rx="10" ry="6" fill="#fff" opacity="0.28" transform="rotate(-25 34 38)" />

        {/* obraji */}
        <circle cx="27" cy="62" r="6" fill="#ff4f9a" opacity="0.55" />
        <circle cx="73" cy="62" r="6" fill="#ff4f9a" opacity="0.55" />

        {/* ochi care clipesc */}
        <motion.g
          style={{ originX: '50px', originY: '49px' }}
          initial={false}
          animate={{ scaleY: mood === 'sleepy' ? eyeScale : [1, 1, 0.1, 1] }}
          transition={
            mood === 'sleepy'
              ? { duration: 0.3 }
              : { duration: 3.4, times: [0, 0.9, 0.95, 1], repeat: Infinity }
          }
        >
          <ellipse cx="38" cy="49" rx="8" ry="10" fill="#fff" />
          <ellipse cx="62" cy="49" rx="8" ry="10" fill="#fff" />
          <circle cx="40" cy="51" r="4.5" fill="#1b0b2e" />
          <circle cx="64" cy="51" r="4.5" fill="#1b0b2e" />
          <circle cx="41.5" cy="49" r="1.5" fill="#fff" />
          <circle cx="65.5" cy="49" r="1.5" fill="#fff" />
        </motion.g>

        <motion.path
          initial={false}
          animate={{ d: mouth.d, fillOpacity: mouth.fill }}
          transition={{ type: 'spring', stiffness: 300, damping: 20 }}
          fill="#2a0f3d"
          stroke="#2a0f3d"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </motion.div>
  )
}
