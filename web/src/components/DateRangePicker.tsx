import { matchPreset, presetRange, RANGE_PRESETS, today, type DateRange } from '../lib/dates';

// min/max on a date input only limit the picker; a typed date can still cross the other end.
// Move the other end along instead of sending a range the API rejects.
const withFrom = (range: DateRange, from: string): DateRange => ({ from, to: from > range.to ? from : range.to });
const withTo = (range: DateRange, to: string): DateRange => ({ from: to < range.from ? to : range.from, to });

/** Preset buttons (7 days … 1 year) plus custom from/to inputs. */
export function DateRangePicker({ value, onChange }: { value: DateRange; onChange: (range: DateRange) => void }) {
  const active = matchPreset(value);
  const max = today();
  return (
    <div className="range-picker">
      <div className="segmented" role="group" aria-label="Date range">
        {RANGE_PRESETS.map((p) => (
          <button
            key={p.key}
            type="button"
            className={active === p.key ? 'active' : undefined}
            aria-pressed={active === p.key}
            onClick={() => onChange(presetRange(p.key))}
          >
            {p.label}
          </button>
        ))}
      </div>
      <div className="range-picker__custom">
        <input
          type="date"
          aria-label="From"
          value={value.from}
          max={value.to}
          onChange={(e) => e.target.value && onChange(withFrom(value, e.target.value))}
        />
        <span aria-hidden>–</span>
        <input
          type="date"
          aria-label="To"
          value={value.to}
          min={value.from}
          max={max}
          onChange={(e) => e.target.value && onChange(withTo(value, e.target.value))}
        />
      </div>
    </div>
  );
}
