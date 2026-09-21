import confetti from 'canvas-confetti'

// disableForReducedMotion: respectă setarea "reduce motion" din sistemul de operare.
const base = { disableForReducedMotion: true, zIndex: 1000 }

export function celebrate({ levelUp = false } = {}) {
  confetti({ ...base, particleCount: 100, spread: 75, origin: { y: 0.65 } })

  if (levelUp) {
    // Nivel nou: două tunuri de confetti din lateral.
    const cannon = (x, angle) =>
      confetti({ ...base, particleCount: 80, angle, spread: 60, startVelocity: 55, origin: { x, y: 0.75 } })
    setTimeout(() => cannon(0, 60), 250)
    setTimeout(() => cannon(1, 120), 250)
  }
}
