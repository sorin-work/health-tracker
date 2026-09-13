import { useId, useState, type KeyboardEvent } from 'react';
import { CloseIcon } from './icons';

const MAX_TAGS = 10;

/** Tags as removable chips; Enter or a comma adds what was typed. Suggestions come from earlier entries. */
export function TagInput({
  value,
  onChange,
  suggestions = [],
  label = 'Tags',
}: {
  value: string[];
  onChange: (tags: string[]) => void;
  suggestions?: string[];
  label?: string;
}) {
  const id = useId();
  const [draft, setDraft] = useState('');

  function add(raw: string) {
    const tag = raw.trim().replace(/\s+/g, ' ').toLowerCase();
    if (tag && !value.includes(tag) && value.length < MAX_TAGS) onChange([...value, tag]);
    setDraft('');
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      add(draft);
    } else if (e.key === 'Backspace' && draft === '' && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="tag-input">
        {value.map((tag) => (
          <span key={tag} className="tag">
            {tag}
            <button type="button" aria-label={`Remove ${tag}`} onClick={() => onChange(value.filter((t) => t !== tag))}>
              <CloseIcon width={12} height={12} />
            </button>
          </span>
        ))}
        <input
          id={id}
          list={`${id}-suggestions`}
          maxLength={30}
          value={draft}
          disabled={value.length >= MAX_TAGS}
          placeholder={value.length === 0 ? 'stress, after coffee…' : ''}
          onChange={(e) => (e.target.value.endsWith(',') ? add(e.target.value.slice(0, -1)) : setDraft(e.target.value))}
          onKeyDown={onKeyDown}
          onBlur={() => draft && add(draft)}
        />
      </div>
      <datalist id={`${id}-suggestions`}>
        {suggestions
          .filter((s) => !value.includes(s))
          .map((s) => (
            <option key={s} value={s} />
          ))}
      </datalist>
      <span className="field__hint">Press Enter or a comma after each tag</span>
    </div>
  );
}
