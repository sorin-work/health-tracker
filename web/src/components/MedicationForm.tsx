import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import {
  createMedication,
  updateMedication,
  type DayOfWeek,
  type Medication,
  type MedicationPayload,
  type ScheduleType,
} from '../api/medications';
import { parseISODate, shortTime, today } from '../lib/dates';
import { WEEKDAYS } from '../lib/format';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';
import { CloseIcon, PlusIcon } from './icons';

const JS_DAYS: DayOfWeek[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

function weekdayOf(iso: string): DayOfWeek {
  return JS_DAYS[parseISODate(iso).getDay()];
}

export function MedicationForm({ medication, onDone }: { medication?: Medication; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(medication?.name ?? '');
  const [dosage, setDosage] = useState(medication?.dosage ?? '');
  const [instructions, setInstructions] = useState(medication?.instructions ?? '');
  const [asNeeded, setAsNeeded] = useState(medication?.asNeeded ?? false);
  const [times, setTimes] = useState<string[]>(
    medication && medication.times.length > 0 ? medication.times.map(shortTime) : ['08:00'],
  );
  const [startDate, setStartDate] = useState(medication?.startDate ?? today());
  const [scheduleType, setScheduleType] = useState<ScheduleType>(medication?.scheduleType ?? 'DAILY');
  const [intervalDays, setIntervalDays] = useState(String(medication?.intervalDays ?? 2));
  const [daysOfWeek, setDaysOfWeek] = useState<DayOfWeek[]>(
    medication?.daysOfWeek?.length ? medication.daysOfWeek : [weekdayOf(medication?.startDate ?? today())],
  );
  const [endDate, setEndDate] = useState(medication?.endDate ?? '');
  const [trackSupply, setTrackSupply] = useState(medication?.supply !== undefined && medication?.supply !== null);
  const [supply, setSupply] = useState(medication?.supply !== undefined ? String(medication.supply) : '');
  const [doseQuantity, setDoseQuantity] = useState(String(medication?.doseQuantity ?? 1));
  const [warningDays, setWarningDays] = useState(String(medication?.refillWarningDays ?? 7));

  const mutation = useMutation({
    mutationFn: (payload: MedicationPayload) =>
      medication ? updateMedication(medication.id, payload) : createMedication(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medications'] });
      queryClient.invalidateQueries({ queryKey: ['doses'] });
      onDone();
    },
  });
  const fields = mutation.error instanceof ApiError ? mutation.error.fields : {};

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      name: name.trim(),
      dosage: dosage.trim() || null,
      instructions: instructions.trim() || null,
      asNeeded,
      times: asNeeded ? [] : times.filter(Boolean),
      startDate,
      endDate: endDate || null,
      scheduleType: asNeeded ? 'DAILY' : scheduleType,
      intervalDays: !asNeeded && scheduleType === 'EVERY_N_DAYS' ? Number(intervalDays) : null,
      daysOfWeek: !asNeeded && scheduleType === 'WEEKLY' ? daysOfWeek : null,
      supply: trackSupply && supply !== '' ? Number(supply) : null,
      doseQuantity: Number(doseQuantity) || 1,
      refillWarningDays: Number(warningDays),
    });
  }

  function toggleDay(day: DayOfWeek) {
    setDaysOfWeek(daysOfWeek.includes(day) ? daysOfWeek.filter((d) => d !== day) : [...daysOfWeek, day]);
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form__row">
        <Field label="Name" error={fields.name}>
          <input required maxLength={120} value={name} placeholder="Metformin" onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Dose" hint="e.g. 500 mg, 2 puffs">
          <input maxLength={60} value={dosage} onChange={(e) => setDosage(e.target.value)} />
        </Field>
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={asNeeded} onChange={(e) => setAsNeeded(e.target.checked)} />
        Take only when needed (no schedule)
      </label>

      {!asNeeded && (
        <fieldset className="schedule">
          <legend>How often</legend>
          <div className="schedule__row">
            <select aria-label="How often" value={scheduleType} onChange={(e) => setScheduleType(e.target.value as ScheduleType)}>
              <option value="DAILY">Every day</option>
              <option value="EVERY_N_DAYS">Every few days</option>
              <option value="WEEKLY">On certain days of the week</option>
            </select>
            {scheduleType === 'EVERY_N_DAYS' && (
              <label className="schedule__interval">
                every
                <input
                  type="number"
                  required
                  min={2}
                  max={90}
                  aria-label="Days between doses"
                  value={intervalDays}
                  onChange={(e) => setIntervalDays(e.target.value)}
                />
                days, from the start date
              </label>
            )}
          </div>
          {scheduleType === 'WEEKLY' && (
            <div className="weekdays" role="group" aria-label="Days of the week">
              {WEEKDAYS.map((d) => (
                <button
                  key={d.key}
                  type="button"
                  className="weekdays__day"
                  aria-pressed={daysOfWeek.includes(d.key)}
                  onClick={() => toggleDay(d.key)}
                >
                  {d.short}
                </button>
              ))}
            </div>
          )}
          {fields.daysOfWeek && <span className="field__error">{fields.daysOfWeek}</span>}
        </fieldset>
      )}

      {!asNeeded && (
        <fieldset className="times">
          <legend>Times of day</legend>
          {times.map((time, i) => (
            <div className="times__row" key={i}>
              <input
                type="time"
                required
                aria-label={`Time ${i + 1}`}
                value={time}
                onChange={(e) => setTimes(times.map((t, j) => (j === i ? e.target.value : t)))}
              />
              {times.length > 1 && (
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`Remove time ${i + 1}`}
                  onClick={() => setTimes(times.filter((_, j) => j !== i))}
                >
                  <CloseIcon />
                </button>
              )}
            </div>
          ))}
          {times.length < 12 && (
            <button type="button" className="button button--ghost button--small" onClick={() => setTimes([...times, '20:00'])}>
              <PlusIcon /> Add a time
            </button>
          )}
        </fieldset>
      )}

      <div className="form__row">
        <Field label="Start date" error={fields.startDate}>
          <input type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </Field>
        <Field label="End date" hint="Leave empty if ongoing">
          <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
        </Field>
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={trackSupply} onChange={(e) => setTrackSupply(e.target.checked)} />
        Track how many are left and warn me before they run out
      </label>

      {trackSupply && (
        <div className="form__row">
          <Field label="Left now" hint="Tablets, puffs…" error={fields.supply}>
            <input type="number" required min={0} step="any" value={supply} onChange={(e) => setSupply(e.target.value)} />
          </Field>
          <Field label="Per dose" error={fields.doseQuantity}>
            <input
              type="number"
              required
              min={0.25}
              step="any"
              value={doseQuantity}
              onChange={(e) => setDoseQuantity(e.target.value)}
            />
          </Field>
          <Field label="Warn days ahead" error={fields.refillWarningDays}>
            <input
              type="number"
              required
              min={0}
              max={90}
              value={warningDays}
              onChange={(e) => setWarningDays(e.target.value)}
            />
          </Field>
        </div>
      )}

      <Field label="Instructions">
        <textarea rows={2} maxLength={500} value={instructions} placeholder="With food" onChange={(e) => setInstructions(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error && Object.keys(fields).length === 0 ? mutation.error : null} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button
          type="submit"
          className="button"
          disabled={mutation.isPending || (!asNeeded && scheduleType === 'WEEKLY' && daysOfWeek.length === 0)}
        >
          {mutation.isPending ? 'Saving…' : medication ? 'Save changes' : 'Add medication'}
        </button>
      </div>
    </form>
  );
}
