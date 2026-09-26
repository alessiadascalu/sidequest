// Metadate vizuale pentru categorii și dificultăți (etichetele vin și de la backend).

export const CATEGORY = {
  SOCIAL: { icon: '💬', gradient: 'linear-gradient(135deg, #ff4fd8, #ff8a3d)', glow: '#ff4fd8' },
  FITNESS_EXPLORE: { icon: '🧭', gradient: 'linear-gradient(135deg, #3dffb0, #22c3ff)', glow: '#22e0c8' },
  FOCUS: { icon: '🎯', gradient: 'linear-gradient(135deg, #22c3ff, #8b5cff)', glow: '#5b8cff' },
  CREATIV: { icon: '🎨', gradient: 'linear-gradient(135deg, #b14dff, #ff4f9a)', glow: '#c04dff' },
}

export const categoryMeta = (category) => CATEGORY[category] ?? { icon: '✨', gradient: 'var(--grad-main)', glow: '#8b5cff' }

export const DIFFICULTY = {
  EASY: { label: 'Ușor', dots: 1 },
  MEDIUM: { label: 'Mediu', dots: 2 },
  HARD: { label: 'Greu', dots: 3 },
}

/** "2026-06-10" e o dată calendaristică, nu un moment: o construim din părți, fără deplasări de fus orar. */
export function parseLocalDate(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function formatDay(isoDate, { relative = false } = {}) {
  const date = parseLocalDate(isoDate)
  if (relative) {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const diff = Math.round((today - date) / 86_400_000)
    if (diff === 0) return 'Azi'
    if (diff === 1) return 'Ieri'
  }
  return date.toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long' })
}

export const formatTime = (instant) =>
  new Date(instant).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })
