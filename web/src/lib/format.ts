import type { ActivityType } from '../api/activities';
import type { GoalType } from '../api/goals';
import type { DayOfWeek, Medication } from '../api/medications';
import type { Flag, GlucoseContext, VitalReading, VitalType } from '../api/vitals';
import {
  goalToDisplay,
  goalUnit,
  METRIC,
  vitalDecimals,
  vitalToDisplay,
  vitalUnit,
  waterToDisplay,
  waterUnit,
  type UnitPrefs,
} from './units';

export interface VitalMeta {
  label: string;
  unit: string;
  decimals: number;
  /** Placeholder for the main value input. */
  placeholder: string;
}

export const VITAL_META: Record<VitalType, VitalMeta> = {
  WEIGHT: { label: 'Weight', unit: 'kg', decimals: 1, placeholder: '72.5' },
  BLOOD_PRESSURE: { label: 'Blood pressure', unit: 'mmHg', decimals: 0, placeholder: '120' },
  HEART_RATE: { label: 'Heart rate', unit: 'bpm', decimals: 0, placeholder: '64' },
  BLOOD_GLUCOSE: { label: 'Blood glucose', unit: 'mg/dL', decimals: 0, placeholder: '95' },
  SLEEP: { label: 'Sleep', unit: 'h', decimals: 1, placeholder: '7.5' },
};

export const VITAL_TYPES = Object.keys(VITAL_META) as VitalType[];

export const GLUCOSE_CONTEXT_LABELS: Record<GlucoseContext, string> = {
  FASTING: 'Fasting',
  BEFORE_MEAL: 'Before a meal',
  AFTER_MEAL: 'After a meal',
  BEDTIME: 'Bedtime',
  RANDOM: 'Random',
};

export const FLAG_LABELS: Record<Flag, string> = {
  CRITICAL_LOW: 'Very low',
  LOW: 'Low',
  NORMAL: 'In range',
  HIGH: 'High',
  CRITICAL_HIGH: 'Very high',
};

export function isOutOfRange(flag: Flag | undefined | null): boolean {
  return !!flag && flag !== 'NORMAL';
}

export function formatNumber(value: number | null | undefined, decimals = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '–';
  return value.toLocaleString(undefined, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

/** `128/82`, `82.4`, `7.5` – without the unit, converted to the user's units. */
export function formatVitalValue(
  type: VitalType,
  value: number,
  secondary?: number | null,
  prefs: UnitPrefs = METRIC,
): string {
  if (type === 'BLOOD_PRESSURE' && secondary !== undefined && secondary !== null) {
    return `${formatNumber(value, 0)}/${formatNumber(secondary, 0)}`;
  }
  return formatNumber(vitalToDisplay(type, value, prefs), vitalDecimals(type, prefs));
}

export function formatReading(
  reading: Pick<VitalReading, 'type' | 'value' | 'secondaryValue'>,
  prefs: UnitPrefs = METRIC,
): string {
  return `${formatVitalValue(reading.type, reading.value, reading.secondaryValue, prefs)} ${vitalUnit(reading.type, prefs)}`;
}

/** `750 ml` or `25 fl oz`. */
export function formatWater(ml: number, prefs: UnitPrefs = METRIC): string {
  return `${formatNumber(waterToDisplay(ml, prefs))} ${waterUnit(prefs)}`;
}

/** 95 -> `1 h 35 min`, 40 -> `40 min` */
export function formatMinutes(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

export const ACTIVITY_LABELS: Record<ActivityType, string> = {
  WALK: 'Walk',
  RUN: 'Run',
  CYCLE: 'Cycling',
  SWIM: 'Swim',
  STRENGTH: 'Strength',
  YOGA: 'Yoga',
  HIIT: 'HIIT',
  OTHER: 'Other',
};

export const GOAL_META: Record<
  GoalType,
  { label: string; unit: string; decimals: number; describe: (target: string, unit: string) => string }
> = {
  WEIGHT: { label: 'Target weight', unit: 'kg', decimals: 1, describe: (t, u) => `Reach ${t} ${u}` },
  DAILY_STEPS: {
    label: 'Daily steps',
    unit: 'steps',
    decimals: 0,
    describe: (t) => `Average ${t} steps a day`,
  },
  WEEKLY_ACTIVE_MINUTES: {
    label: 'Active minutes',
    unit: 'min',
    decimals: 0,
    describe: (t) => `${t} active minutes a week`,
  },
  WEEKLY_WORKOUTS: {
    label: 'Workouts',
    unit: 'sessions',
    decimals: 0,
    describe: (t) => `${t} workouts a week`,
  },
  SLEEP_HOURS: { label: 'Sleep', unit: 'h', decimals: 1, describe: (t) => `Sleep ${t} h a night` },
  DAILY_WATER: { label: 'Water', unit: 'ml', decimals: 0, describe: (t, u) => `Drink ${t} ${u} a day` },
};

/** "Reach 176.4 lb", "Average 8,000 steps a day" in the user's units. */
export function describeGoal(type: GoalType, target: number, prefs: UnitPrefs = METRIC): string {
  const meta = GOAL_META[type];
  return meta.describe(formatNumber(goalToDisplay(type, target, prefs), meta.decimals), goalUnit(type, meta.unit, prefs));
}

export const WEEKDAYS: { key: DayOfWeek; short: string }[] = [
  { key: 'MONDAY', short: 'Mon' },
  { key: 'TUESDAY', short: 'Tue' },
  { key: 'WEDNESDAY', short: 'Wed' },
  { key: 'THURSDAY', short: 'Thu' },
  { key: 'FRIDAY', short: 'Fri' },
  { key: 'SATURDAY', short: 'Sat' },
  { key: 'SUNDAY', short: 'Sun' },
];

/** "08:00, 20:00", "Every 2 days at 08:00", "Mon, Thu at 09:00" or "As needed". */
export function describeSchedule(
  m: Pick<Medication, 'asNeeded' | 'times' | 'scheduleType' | 'intervalDays' | 'daysOfWeek'>,
): string {
  if (m.asNeeded) return 'As needed';
  const times = m.times.map((t) => t.slice(0, 5)).join(', ');
  switch (m.scheduleType) {
    case 'EVERY_N_DAYS':
      return m.intervalDays === 2 ? `Every other day at ${times}` : `Every ${m.intervalDays} days at ${times}`;
    case 'WEEKLY': {
      const days = WEEKDAYS.filter((d) => m.daysOfWeek?.includes(d.key)).map((d) => d.short);
      return days.length === 7 ? times : `${days.join(', ')} at ${times}`;
    }
    default:
      return times;
  }
}

/** "34 left · about 17 days", "2 left · runs out today", "8 left" (as needed). */
export function describeSupply(m: Pick<Medication, 'supply' | 'daysOfSupply' | 'lowSupply'>): string {
  if (m.supply === undefined || m.supply === null) return '';
  const left = `${formatNumber(m.supply, m.supply % 1 ? 1 : 0)} left`;
  if (m.daysOfSupply === undefined || m.daysOfSupply === null) return m.lowSupply ? `${left} · refill soon` : left;
  if (m.daysOfSupply === 0) return `${left} · runs out today`;
  return `${left} · about ${m.daysOfSupply} ${m.daysOfSupply === 1 ? 'day' : 'days'}`;
}

/** Progress bar colour for an adherence percentage: green from 90% up. */
export function adherenceTone(percent: number | undefined): 'success' | 'warning' | 'default' {
  if (percent === undefined) return 'default';
  return percent >= 90 ? 'success' : 'warning';
}

export const MOOD_LABELS: Record<number, string> = {
  1: 'Very low',
  2: 'Low',
  3: 'Okay',
  4: 'Good',
  5: 'Very good',
};

export const ENERGY_LABELS: Record<number, string> = {
  1: 'Exhausted',
  2: 'Tired',
  3: 'Normal',
  4: 'Energetic',
  5: 'Full of energy',
};

/** Label for a (possibly averaged) mood score, rounded to the nearest step. */
export function moodLabel(score: number): string {
  return MOOD_LABELS[Math.min(5, Math.max(1, Math.round(score)))];
}

export function severityLabel(severity: number): string {
  if (severity <= 3) return 'Mild';
  if (severity <= 6) return 'Moderate';
  if (severity <= 8) return 'Severe';
  return 'Very severe';
}
