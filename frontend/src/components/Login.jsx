import { motion, useAnimate } from 'framer-motion'
import { useState } from 'react'
import { loginOrCreate } from '../api.js'
import Mascot from './Mascot.jsx'

// Fără parolă: username-ul e contul. Fusul orar vine din browser (contează doar la conturile noi).
const detectZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
const VALID = /^[\p{L}\p{N}_.-]+$/u

const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08, delayChildren: 0.1 } },
}
const rise = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 260, damping: 22 } },
}

export default function Login({ onLoggedIn }) {
  const [username, setUsername] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [inputScope, animate] = useAnimate()
  const shake = () => animate(inputScope.current, { x: [0, -12, 10, -6, 4, 0] }, { duration: 0.4 })
  const zoneId = detectZone()

  const name = username.trim()
  const formatError = name && !VALID.test(name) ? "Doar litere, cifre, '_', '.' și '-'." : null
  const canSubmit = name.length >= 3 && !formatError && !busy

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canSubmit) {
      shake()
      return
    }
    setBusy(true)
    setError(null)
    try {
      onLoggedIn(await loginOrCreate(name, zoneId))
    } catch (e) {
      setError(e.message)
      shake()
      setBusy(false)
    }
  }

  return (
    <motion.div className="login" variants={stagger} initial="hidden" animate="show">
      <motion.div className="login-hero" variants={rise}>
        <Mascot size={110} mood={busy ? 'hype' : name.length >= 3 ? 'happy' : 'idle'} />
        <h1 className="login-title display">
          Side<span className="gradient-text">Quest</span>
        </h1>
        <p className="login-tagline muted">
          O mini-misiune pe zi. Fără presiune, doar puțin haos controlat. ✨
        </p>
      </motion.div>

      <motion.form className="card login-form" onSubmit={handleSubmit} variants={rise}>
        <label htmlFor="username" className="field-label">
          Cum te strigăm?
        </label>
        <div className="input-wrap" ref={inputScope}>
          <span className="at" aria-hidden="true">
            @
          </span>
          <input
            id="username"
            className="text-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="maria_explorer"
            minLength={3}
            maxLength={30}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            autoFocus
            required
          />
        </div>
        <p className="hint">
          Ai mai fost pe aici? Scrie același username și continui de unde ai rămas, cu tot cu XP și streak.
        </p>

        {(formatError || error) && (
          <p className="error" role="alert">
            {formatError || error}
          </p>
        )}

        <motion.button
          className="btn btn-primary btn-block"
          type="submit"
          disabled={busy}
          whileHover={canSubmit ? { scale: 1.03 } : undefined}
          whileTap={{ scale: 0.95 }}
        >
          {busy ? 'Se încarcă…' : 'Intră în joc →'}
        </motion.button>
        <p className="hint center">Fus orar detectat: {zoneId}</p>
      </motion.form>
    </motion.div>
  )
}
