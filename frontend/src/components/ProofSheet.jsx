import { AnimatePresence, motion, useDragControls } from 'framer-motion'
import { useEffect, useRef, useState } from 'react'

const MAX_TEXT = 500
const MAX_BYTES = 10 * 1024 * 1024
const ACCEPTED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

/**
 * Bottom sheet deschis la "Completed": atașezi opțional o poză și/sau un text, apoi confirmi.
 * Sheet-ul se montează doar cât e deschis, deci starea (poza, textul) pornește mereu de la zero.
 */
export default function ProofSheet({ open, ...props }) {
  return <AnimatePresence>{open && <Sheet {...props} />}</AnimatePresence>
}

function Sheet({ onClose, onSubmit, submitting, error }) {
  // { file, url }: url-ul de previzualizare e creat în handler și eliberat când poza e înlocuită/scoasă.
  const [photo, setPhoto] = useState(null)
  const photoRef = useRef(null)
  const [text, setText] = useState('')
  const [localError, setLocalError] = useState(null)
  const [dragging, setDragging] = useState(false)
  const inputRef = useRef(null)
  const drag = useDragControls()

  function replacePhoto(next) {
    if (photoRef.current) URL.revokeObjectURL(photoRef.current.url)
    photoRef.current = next
    setPhoto(next)
  }

  // eliberează previzualizarea când sheet-ul se închide
  useEffect(() => () => photoRef.current && URL.revokeObjectURL(photoRef.current.url), [])

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && !submitting && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, submitting])

  function pick(file) {
    if (!file) return
    if (!ACCEPTED.includes(file.type)) {
      setLocalError('Merge doar cu poze JPG, PNG, GIF sau WebP.')
      return
    }
    if (file.size > MAX_BYTES) {
      setLocalError('Poza e prea mare (maximum 10 MB).')
      return
    }
    setLocalError(null)
    replacePhoto({ file, url: URL.createObjectURL(file) })
  }

  function removePhoto() {
    replacePhoto(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit({ photo: photo?.file, text })
  }

  const hasProof = photo || text.trim()

  return (
    <motion.div
      className="backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={() => !submitting && onClose()}
    >
      <motion.form
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="proof-title"
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', stiffness: 320, damping: 32 }}
        drag="y"
        dragControls={drag}
        dragListener={false}
        dragConstraints={{ top: 0, bottom: 0 }}
        dragElastic={{ top: 0, bottom: 0.6 }}
        onDragEnd={(_, info) => info.offset.y > 120 && !submitting && onClose()}
      >
        {/* doar mânerul pornește drag-ul, ca textarea și scroll-ul să meargă normal */}
        <div className="sheet-grip" onPointerDown={(e) => drag.start(e)}>
          <div className="sheet-handle" />
        </div>
        <div>
          <h2 id="proof-title" className="sheet-title display">
            Pics or it didn't happen 📸
          </h2>
          <p className="muted small" style={{ marginTop: '0.35rem' }}>
            Dovada e opțională. Pune o poză, câteva cuvinte, amândouă sau nimic.
          </p>
        </div>

        <AnimatePresence mode="wait" initial={false}>
          {photo ? (
            <motion.div
              key="preview"
              className="preview"
              initial={{ opacity: 0, scale: 0.9, rotate: -2 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              exit={{ opacity: 0, scale: 0.9 }}
            >
              <img src={photo.url} alt="Previzualizare dovadă" />
              <motion.button
                type="button"
                className="icon-btn"
                onClick={removePhoto}
                whileTap={{ scale: 0.85 }}
                aria-label="Scoate poza"
              >
                ✕
              </motion.button>
            </motion.div>
          ) : (
            <motion.label
              key="drop"
              className={`dropzone ${dragging ? 'dragging' : ''}`}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.98 }}
              onDragOver={(e) => {
                e.preventDefault()
                setDragging(true)
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => {
                e.preventDefault()
                setDragging(false)
                pick(e.dataTransfer.files?.[0])
              }}
            >
              <input
                ref={inputRef}
                type="file"
                accept={ACCEPTED.join(',')}
                onChange={(e) => pick(e.target.files?.[0])}
              />
              <motion.span
                className="dropzone-emoji"
                animate={{ rotate: [0, -10, 10, 0] }}
                transition={{ duration: 1.6, repeat: Infinity, repeatDelay: 1 }}
                aria-hidden="true"
              >
                📷
              </motion.span>
              <strong>Adaugă o poză</strong>
              <span className="hint">Atinge sau trage aici · JPG, PNG, GIF, WebP · max 10 MB</span>
            </motion.label>
          )}
        </AnimatePresence>

        <div>
          <label htmlFor="proof-text" className="field-label">
            Cum a fost?
          </label>
          <textarea
            id="proof-text"
            className="textarea"
            style={{ marginTop: '0.5rem' }}
            value={text}
            onChange={(e) => setText(e.target.value.slice(0, MAX_TEXT))}
            placeholder="ex. Am urcat 12 etaje și am supraviețuit 💀"
            maxLength={MAX_TEXT}
          />
          <div className="counter">
            {text.length}/{MAX_TEXT}
          </div>
        </div>

        {(localError || error) && (
          <motion.p className="error" role="alert" initial={{ x: -8 }} animate={{ x: [8, -6, 4, 0] }}>
            {localError || error}
          </motion.p>
        )}

        <div className="sheet-actions">
          <motion.button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={submitting}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.95 }}
          >
            {submitting ? 'Se trimite…' : hasProof ? 'Trimite cu dovadă 🚀' : 'Completed, fără dovadă ✓'}
          </motion.button>
          <button type="button" className="btn btn-ghost btn-block" onClick={onClose} disabled={submitting}>
            Mai stai puțin
          </button>
        </div>
      </motion.form>
    </motion.div>
  )
}
