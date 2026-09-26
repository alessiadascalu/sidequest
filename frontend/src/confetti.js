import confetti from 'canvas-confetti'

// disableForReducedMotion: respectă setarea "reduce motion" din sistemul de operare.
const base = { disableForReducedMotion: true, zIndex: 1000 }
const NEON = ['#8b5cff', '#ff4fd8', '#ff8a3d', '#3dffb0', '#22c3ff', '#fff36b']

export function celebrateQuest() {
  confetti({ ...base, colors: NEON, particleCount: 120, spread: 80, startVelocity: 45, origin: { y: 0.6 } })
  setTimeout(
    () => confetti({ ...base, colors: NEON, particleCount: 60, spread: 120, scalar: 0.8, origin: { y: 0.5 } }),
    180,
  )
}

export function celebrateLevelUp() {
  // Două tunuri din lateral + o ploaie de stele din mijloc.
  const cannon = (x, angle) =>
    confetti({ ...base, colors: NEON, particleCount: 90, angle, spread: 65, startVelocity: 60, origin: { x, y: 0.8 } })
  cannon(0, 60)
  cannon(1, 120)
  setTimeout(() => {
    confetti({ ...base, colors: NEON, shapes: ['star'], particleCount: 70, spread: 360, startVelocity: 30, scalar: 1.3, origin: { y: 0.4 } })
  }, 350)
}
