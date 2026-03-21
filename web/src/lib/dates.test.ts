import { describe, expect, it } from 'vitest';
import { addDays, matchPreset, parseISODate, presetRange, shortTime, toISODate, toLocalInputValue } from './dates';

describe('dates', () => {
  it('formats local calendar days', () => {
    expect(toISODate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('parses ISO dates as local days', () => {
    const d = parseISODate('2026-03-01');
    expect(d.getFullYear()).toBe(2026);
    expect(d.getMonth()).toBe(2);
    expect(d.getDate()).toBe(1);
  });

  it('adds days across month and year ends', () => {
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
    expect(addDays('2025-12-31', 1)).toBe('2026-01-01');
  });

  it('builds preset ranges that include the end day', () => {
    expect(presetRange('7d', '2026-03-10')).toEqual({ from: '2026-03-04', to: '2026-03-10' });
    expect(presetRange('30d', '2026-03-10')).toEqual({ from: '2026-02-09', to: '2026-03-10' });
    expect(presetRange('1y', '2026-03-10').from).toBe('2025-03-11');
  });

  it('recognises preset ranges', () => {
    expect(matchPreset({ from: '2026-03-04', to: '2026-03-10' }, '2026-03-10')).toBe('7d');
    expect(matchPreset({ from: '2026-03-05', to: '2026-03-10' }, '2026-03-10')).toBeNull();
    expect(matchPreset({ from: '2026-03-03', to: '2026-03-09' }, '2026-03-10')).toBeNull();
  });

  it('formats datetime-local values', () => {
    expect(toLocalInputValue(new Date(2026, 2, 14, 8, 5))).toBe('2026-03-14T08:05');
  });

  it('shortens times of day', () => {
    expect(shortTime('08:00:00')).toBe('08:00');
    expect(shortTime('20:30')).toBe('20:30');
  });
});
