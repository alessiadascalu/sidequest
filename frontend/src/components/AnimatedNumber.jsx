import { animate } from 'framer-motion'
import { useEffect, useRef } from 'react'

/** Un număr care "numără" până la valoarea nouă în loc să sară direct la ea. */
export default function AnimatedNumber({ value, duration = 0.9 }) {
  const ref = useRef(null)
  const shown = useRef(0)

  useEffect(() => {
    const controls = animate(shown.current, value, {
      duration,
      ease: [0.22, 1, 0.36, 1],
      onUpdate: (v) => {
        shown.current = v
        if (ref.current) ref.current.textContent = Math.round(v).toLocaleString('ro-RO')
      },
    })
    return () => controls.stop()
  }, [value, duration])

  return <span ref={ref}>0</span>
}
