import { describe, expect, it } from 'vitest';
import {
  describeSchedule,
  formatMinutes,
  formatNumber,
  formatReading,
  formatVitalValue,
  isOutOfRange,
  severityLabel,
} from './format';

describe('format', () => {
  it('formats blood pressure as systolic/diastolic', () => {
    expect(formatVitalValue('BLOOD_PRESSURE', 128, 82)).toBe('128/82');
    expect(formatReading({ type: 'BLOOD_PRESSURE', value: 128, secondaryValue: 82 })).toBe('128/82 mmHg');
  });

  it('uses the right precision per type', () => {
    expect(formatReading({ type: 'WEIGHT', value: 82.44 })).toBe('82.4 kg');
    expect(formatReading({ type: 'HEART_RATE', value: 64 })).toBe('64 bpm');
    expect(formatReading({ type: 'SLEEP', value: 7.25 })).toMatch(/^7\.[23] h$/);
  });

  it('renders missing numbers as a dash', () => {
    expect(formatNumber(undefined)).toBe('–');
    expect(formatNumber(null)).toBe('–');
  });

  it('formats durations', () => {
    expect(formatMinutes(40)).toBe('40 min');
    expect(formatMinutes(60)).toBe('1 h');
    expect(formatMinutes(95)).toBe('1 h 35 min');
  });

  it('knows which flags are out of range', () => {
    expect(isOutOfRange('HIGH')).toBe(true);
    expect(isOutOfRange('CRITICAL_LOW')).toBe(true);
    expect(isOutOfRange('NORMAL')).toBe(false);
    expect(isOutOfRange(undefined)).toBe(false);
  });

  it('labels severity', () => {
    expect(severityLabel(2)).toBe('Mild');
    expect(severityLabel(5)).toBe('Moderate');
    expect(severityLabel(8)).toBe('Severe');
    expect(severityLabel(10)).toBe('Very severe');
  });

  it('formats readings in the user\'s units', () => {
    const imperial = { unitSystem: 'IMPERIAL', glucoseUnit: 'MMOL_L' } as const;

    expect(formatReading({ type: 'WEIGHT', value: 80 }, imperial)).toBe('176.4 lb');
    expect(formatReading({ type: 'BLOOD_GLUCOSE', value: 108 }, imperial)).toBe('6.0 mmol/L');
    expect(formatReading({ type: 'BLOOD_PRESSURE', value: 128, secondaryValue: 82 }, imperial)).toBe('128/82 mmHg');
  });

  it('describes medication schedules', () => {
    const base = { asNeeded: false, times: ['08:00:00', '20:00:00'], daysOfWeek: [] };

    expect(describeSchedule({ ...base, scheduleType: 'DAILY' })).toBe('08:00, 20:00');
    expect(describeSchedule({ ...base, scheduleType: 'EVERY_N_DAYS', intervalDays: 2 })).toBe(
      'Every other day at 08:00, 20:00',
    );
    expect(describeSchedule({ ...base, scheduleType: 'EVERY_N_DAYS', intervalDays: 3 })).toBe(
      'Every 3 days at 08:00, 20:00',
    );
    expect(
      describeSchedule({ ...base, times: ['09:00'], scheduleType: 'WEEKLY', daysOfWeek: ['THURSDAY', 'MONDAY'] }),
    ).toBe('Mon, Thu at 09:00');
    expect(describeSchedule({ ...base, asNeeded: true, times: [], scheduleType: 'DAILY' })).toBe('As needed');
  });
});
