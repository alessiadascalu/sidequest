import { motion, useAnimate } from 'framer-motion'
import { useState } from 'react'
import { createGroup, joinGroup } from '../api.js'

function useSubmit(action, onSuccess) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [scope, animate] = useAnimate()

  async function run(...args) {
    setBusy(true)
    setError(null)
    try {
      onSuccess(await action(...args))
    } catch (e) {
      setError(e.message)
      animate(scope.current, { x: [0, -10, 8, -5, 3, 0] }, { duration: 0.4 })
    } finally {
      setBusy(false)
    }
  }
  return { busy, error, scope, run }
}

export function CreateGroupForm({ userId, onCreated }) {
  const [name, setName] = useState('')
  const { busy, error, scope, run } = useSubmit(createGroup, onCreated)
  const valid = name.trim().length > 0

  return (
    <motion.form
      ref={scope}
      className="card group-form"
      onSubmit={(e) => {
        e.preventDefault()
        if (valid) run(name.trim(), userId)
      }}
      whileHover={{ y: -3 }}
    >
      <span className="form-emoji" aria-hidden="true">
        🏰
      </span>
      <h3 className="display">Creează un grup</h3>
      <p className="muted small">Faci grupul, primești un cod, îl trimiți prietenilor.</p>
      <input
        className="text-input plain"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="ex. Gașca de la cămin"
        maxLength={40}
        aria-label="Numele grupului"
      />
      {error && (
        <p className="error small" role="alert">
          {error}
        </p>
      )}
      <motion.button className="btn btn-primary btn-block" disabled={!valid || busy} whileTap={{ scale: 0.94 }}>
        {busy ? 'Se creează…' : 'Creează grupul ✨'}
      </motion.button>
    </motion.form>
  )
}

export function JoinGroupForm({ userId, onJoined }) {
  const [code, setCode] = useState('')
  const { busy, error, scope, run } = useSubmit(joinGroup, onJoined)
  const clean = code.replace(/[\s-]/g, '')
  const valid = clean.length >= 4

  return (
    <motion.form
      ref={scope}
      className="card group-form"
      onSubmit={(e) => {
        e.preventDefault()
        if (valid) run(userId, clean)
      }}
      whileHover={{ y: -3 }}
    >
      <span className="form-emoji" aria-hidden="true">
        🎟️
      </span>
      <h3 className="display">Alătură-te unui grup</h3>
      <p className="muted small">Ai primit un cod de la cineva? Scrie-l aici.</p>
      <input
        className="text-input plain code-input"
        value={code}
        onChange={(e) => setCode(e.target.value.toUpperCase())}
        placeholder="K7QX2M"
        maxLength={12}
        autoCapitalize="characters"
        autoComplete="off"
        spellCheck={false}
        aria-label="Codul de invitație"
      />
      {error && (
        <p className="error small" role="alert">
          {error}
        </p>
      )}
      <motion.button className="btn btn-primary btn-block" disabled={!valid || busy} whileTap={{ scale: 0.94 }}>
        {busy ? 'Se verifică…' : 'Intră în grup →'}
      </motion.button>
    </motion.form>
  )
}
