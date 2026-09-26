import { motion } from 'framer-motion'
import { useState } from 'react'
import { imageSrc } from '../api.js'

/** Dovada unui quest: miniatura pozei (se deschide mare la click) și/sau textul, ca un balon de chat. */
export default function ProofView({ proof, layoutKey, onOpenImage }) {
  const src = imageSrc(proof.imageUrl)
  // Pe Render (plan gratuit) discul e efemer: poza poate dispărea după un redeploy sau restart.
  const [missing, setMissing] = useState(false)

  return (
    <div className="proof">
      {src && !missing && (
        <motion.button
          className="proof-thumb"
          onClick={() => onOpenImage?.({ src, layoutId: layoutKey })}
          whileHover={{ scale: 1.06, rotate: -2 }}
          whileTap={{ scale: 0.94 }}
          aria-label="Vezi poza-dovadă mărită"
        >
          <motion.img
            layoutId={layoutKey}
            src={src}
            alt="Dovada quest-ului"
            loading="lazy"
            onError={() => setMissing(true)}
          />
        </motion.button>
      )}
      {src && missing && (
        <div className="proof-thumb proof-missing" title="Poza nu mai e disponibilă pe server">
          <span aria-hidden="true">📷</span>
          <span>poză pierdută</span>
        </div>
      )}
      {proof.text && <p className="proof-text">„{proof.text}”</p>}
    </div>
  )
}
