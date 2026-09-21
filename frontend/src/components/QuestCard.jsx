const CATEGORY_ICON = {
  SOCIAL: '🗣️',
  FITNESS_EXPLORE: '🧭',
  FOCUS: '🎯',
  CREATIV: '🎨',
}

const DIFFICULTY_LABEL = {
  EASY: 'Ușor',
  MEDIUM: 'Mediu',
  HARD: 'Greu',
}

export default function QuestCard({ quest, completing, onComplete }) {
  const { completed, xpReward } = quest
  const { text, category, categoryLabel, difficulty } = quest.quest

  return (
    <section className={`card quest ${completed ? 'is-done' : ''}`} aria-live="polite">
      <div className="quest-meta">
        <span className="badge">{CATEGORY_ICON[category]} {categoryLabel}</span>
        <span className={`badge difficulty ${difficulty.toLowerCase()}`}>{DIFFICULTY_LABEL[difficulty]}</span>
      </div>

      <p className="quest-text">{text}</p>

      <div className="quest-footer">
        <span className="reward">+{xpReward} XP</span>
        {completed ? (
          <span className="done-note">Completed ✓ Revino mâine pentru altă misiune.</span>
        ) : (
          <button className="primary" onClick={onComplete} disabled={completing}>
            {completing ? 'Se salvează…' : 'Completed'}
          </button>
        )}
      </div>
    </section>
  )
}
