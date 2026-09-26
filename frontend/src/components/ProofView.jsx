import { motion } from 'framer-motion'
import { imageSrc } from '../api.js'

/** Dovada unui quest: miniatura pozei (se deschide mare la click) și/sau textul, ca un balon de chat. */
export default function ProofView({ proof, layoutKey, onOpenImage }) {
  const src = imageSrc(proof.imageUrl)
  return (
    <div className="proof">
      {src && (
        <motion.button
          className="proof-thumb"
          onClick={() => onOpenImage?.({ src, layoutId: layoutKey })}
          whileHover={{ scale: 1.06, rotate: -2 }}
          whileTap={{ scale: 0.94 }}
          aria-label="Vezi poza-dovadă mărită"
        >
          <motion.img layoutId={layoutKey} src={src} alt="Dovada quest-ului" loading="lazy" />
        </motion.button>
      )}
      {proof.text && <p className="proof-text">„{proof.text}”</p>}
    </div>
  )
}
