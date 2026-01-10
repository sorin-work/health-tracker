export function ProgressBar({
  percent,
  label,
  tone = 'default',
}: {
  percent: number;
  label: string;
  tone?: 'default' | 'success' | 'warning';
}) {
  const clamped = Math.max(0, Math.min(100, percent));
  return (
    <div
      className={`progress progress--${tone}`}
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped)}
    >
      <div className="progress__bar" style={{ width: `${clamped}%` }} />
    </div>
  );
}
