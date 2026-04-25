import type { ActivityType } from '../api/activities';
import type { GoalType } from '../api/goals';
import type { Flag, GlucoseContext, VitalReading, VitalType } from '../api/vitals';

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

/** `128/82`, `82.4`, `7.5` – without the unit. */
export function formatVitalValue(type: VitalType, value: number, secondary?: number | null): string {
  const { decimals } = VITAL_META[type];
  if (type === 'BLOOD_PRESSURE' && secondary !== undefined && secondary !== null) {
    return `${formatNumber(value, 0)}/${formatNumber(secondary, 0)}`;
  }
  return formatNumber(value, decimals);
}

export function formatReading(reading: Pick<VitalReading, 'type' | 'value' | 'secondaryValue'>): string {
  return `${formatVitalValue(reading.type, reading.value, reading.secondaryValue)} ${VITAL_META[reading.type].unit}`;
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

export const GOAL_META: Record<GoalType, { label: string; unit: string; describe: (target: number) => string }> = {
  WEIGHT: { label: 'Target weight', unit: 'kg', describe: (t) => `Reach ${formatNumber(t, 1)} kg` },
  DAILY_STEPS: {
    label: 'Daily steps',
    unit: 'steps',
    describe: (t) => `Average ${formatNumber(t)} steps a day`,
  },
  WEEKLY_ACTIVE_MINUTES: {
    label: 'Active minutes',
    unit: 'min',
    describe: (t) => `${formatNumber(t)} active minutes a week`,
  },
  WEEKLY_WORKOUTS: {
    label: 'Workouts',
    unit: 'sessions',
    describe: (t) => `${formatNumber(t)} workouts a week`,
  },
  SLEEP_HOURS: { label: 'Sleep', unit: 'h', describe: (t) => `Sleep ${formatNumber(t, 1)} h a night` },
};

/** Progress bar colour for an adherence percentage: green from 90% up. */
export function adherenceTone(percent: number | undefined): 'success' | 'warning' | 'default' {
  if (percent === undefined) return 'default';
  return percent >= 90 ? 'success' : 'warning';
}

export function severityLabel(severity: number): string {
  if (severity <= 3) return 'Mild';
  if (severity <= 6) return 'Moderate';
  if (severity <= 8) return 'Severe';
  return 'Very severe';
}
