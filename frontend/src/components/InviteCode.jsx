import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useState } from 'react'

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    // Fallback pentru browsere/contexte fără Clipboard API (ex. pagină servită pe http, nu localhost).
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    const ok = document.execCommand('copy')
    area.remove()
    return ok
  }
}

function useCopy() {
  const [copied, setCopied] = useState(false)
  useEffect(() => {
    if (!copied) return
    const timer = setTimeout(() => setCopied(false), 2000)
    return () => clearTimeout(timer)
  }, [copied])
  return [copied, async (text) => setCopied(await copyText(text))]
}

export function CopyButton({ code, compact = false }) {
  const [copied, copy] = useCopy()
  return (
    <motion.button
      type="button"
      className={`btn ${compact ? 'btn-ghost btn-sm' : 'btn-primary'}`}
      onClick={() => copy(code)}
      whileHover={{ scale: 1.04 }}
      whileTap={{ scale: 0.9 }}
      aria-live="polite"
    >
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={copied ? 'done' : 'copy'}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.15 }}
        >
          {copied ? 'Copiat! ✓' : compact ? '📋 Copiază' : '📋 Copiază codul'}
        </motion.span>
      </AnimatePresence>
    </motion.button>
  )
}

/** Codul de invitație mare, cu literele intrând pe rând, plus copiere și (pe telefon) share. */
export default function InviteCodeCard({ group, fresh = false, onDismiss }) {
  const code = group.inviteCode
  const canShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

  function share() {
    navigator
      .share({ title: 'SideQuest', text: `Hai în grupul meu „${group.name}” pe SideQuest! Codul de invitație: ${code}` })
      .catch(() => {}) // utilizatorul a închis fereastra de share
  }

  return (
    <motion.section
      className={`card invite-card ${fresh ? 'fresh' : ''}`}
      initial={{ opacity: 0, scale: 0.9, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.9 }}
      transition={{ type: 'spring', stiffness: 260, damping: 20 }}
    >
      {fresh && (
        <div className="invite-head">
          <h3 className="display">Grupul „{group.name}” e gata! 🎉</h3>
          {onDismiss && (
            <button className="icon-btn" onClick={onDismiss} aria-label="Închide">
              ✕
            </button>
          )}
        </div>
      )}
      <p className="muted small">Trimite codul ăsta prietenilor. Ei îl scriu la „Alătură-te unui grup”.</p>

      <div className="code-tiles" aria-label={`Cod de invitație: ${code.split('').join(' ')}`}>
        {code.split('').map((ch, i) => (
          <motion.span
            key={`${code}-${i}`}
            className="code-tile"
            aria-hidden="true"
            initial={{ rotateX: -90, opacity: 0 }}
            animate={{ rotateX: 0, opacity: 1 }}
            transition={{ type: 'spring', stiffness: 300, damping: 18, delay: 0.1 + i * 0.07 }}
          >
            {ch}
          </motion.span>
        ))}
      </div>

      <div className="invite-actions">
        <CopyButton code={code} />
        {canShare && (
          <motion.button type="button" className="btn btn-ghost" onClick={share} whileTap={{ scale: 0.9 }}>
            📤 Trimite
          </motion.button>
        )}
      </div>
    </motion.section>
  )
}
