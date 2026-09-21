export default function Stats({ profile }) {
  const { level, streak } = profile
  const percent = Math.round(level.progress * 100)

  return (
    <section className="stats">
      <div className="card streak" title={`Cel mai lung streak: ${streak.longest} zile`}>
        <span className="streak-flame" aria-hidden="true">🔥</span>
        <div>
          <strong className="streak-count">{streak.current}</strong>
          <span className="streak-label">{streak.current === 1 ? 'zi la rând' : 'zile la rând'}</span>
          <div className="muted small">record: {streak.longest}</div>
        </div>
      </div>

      <div className="card xp">
        <div className="xp-head">
          <strong>Nivel {level.level}</strong>
          <span className="xp-title">{level.title}</span>
        </div>
        <div
          className="xp-bar"
          role="progressbar"
          aria-label="Progres către nivelul următor"
          aria-valuemin={0}
          aria-valuemax={level.xpForNextLevel}
          aria-valuenow={level.xpIntoLevel}
        >
          <div className="xp-fill" style={{ width: `${percent}%` }} />
        </div>
        <div className="xp-foot muted small">
          {level.xpIntoLevel} / {level.xpForNextLevel} XP · total {level.totalXp} XP
        </div>
      </div>
    </section>
  )
}
