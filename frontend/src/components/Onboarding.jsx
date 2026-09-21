import { useState } from 'react'
import { createUser } from '../api.js'

// Fără auth real: alegi un username, iar fusul orar vine din browser.
const detectZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'

export default function Onboarding({ onCreated }) {
  const [username, setUsername] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const zoneId = detectZone()

  async function handleSubmit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const user = await createUser(username.trim(), zoneId)
      onCreated(user.id)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <form className="card onboarding" onSubmit={handleSubmit}>
      <h2>Bun venit la SideQuest 🧭</h2>
      <p className="muted">O mini-misiune pe zi. Fără presiune. Doar puțin haos controlat.</p>

      <label htmlFor="username">Cum să te strigăm?</label>
      <input
        id="username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="ex. maria_explorer"
        minLength={3}
        maxLength={30}
        autoComplete="off"
        autoFocus
        required
      />
      <p className="hint">Fus orar detectat: {zoneId}. Ziua ta începe și se termină după el.</p>

      {error && <p className="error" role="alert">{error}</p>}
      <button className="primary" type="submit" disabled={busy || username.trim().length < 3}>
        {busy ? 'Se creează…' : 'Începe aventura'}
      </button>
    </form>
  )
}
