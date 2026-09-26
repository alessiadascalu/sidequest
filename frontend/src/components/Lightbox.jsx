import { AnimatePresence, motion } from 'framer-motion'
import { useEffect } from 'react'

/** Poza mărită; `layoutId` o leagă de miniatură, deci "zboară" din card până în centru. */
export default function Lightbox({ image, onClose }) {
  useEffect(() => {
    if (!image) return
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [image, onClose])

  return (
    <AnimatePresence>
      {image && (
        <motion.div
          className="lightbox"
          onClick={onClose}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          role="dialog"
          aria-modal="true"
          aria-label="Poza-dovadă"
        >
          <motion.img layoutId={image.layoutId} src={image.src} alt="Dovada quest-ului, mărită" />
        </motion.div>
      )}
    </AnimatePresence>
  )
}
