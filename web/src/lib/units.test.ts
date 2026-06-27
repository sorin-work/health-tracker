import { describe, expect, it } from 'vitest';
import {
  distanceFromDisplay,
  distanceToDisplay,
  METRIC,
  vitalDecimals,
  vitalFromDisplay,
  vitalToDisplay,
  vitalUnit,
  waterFromDisplay,
  waterToDisplay,
  type UnitPrefs,
} from './units';

const IMPERIAL: UnitPrefs = { unitSystem: 'IMPERIAL', glucoseUnit: 'MG_DL' };
const MMOL: UnitPrefs = { unitSystem: 'METRIC', glucoseUnit: 'MMOL_L' };

describe('units', () => {
  it('leaves metric values alone', () => {
    expect(vitalToDisplay('WEIGHT', 82.4, METRIC)).toBe(82.4);
    expect(vitalFromDisplay('BLOOD_GLUCOSE', 95, METRIC)).toBe(95);
    expect(vitalUnit('WEIGHT', METRIC)).toBe('kg');
  });

  it('converts weight to pounds and back', () => {
    expect(vitalToDisplay('WEIGHT', 80, IMPERIAL)).toBeCloseTo(176.37, 2);
    expect(vitalFromDisplay('WEIGHT', 176.4, IMPERIAL)).toBe(80.01);
    expect(vitalUnit('WEIGHT', IMPERIAL)).toBe('lb');
  });

  it('converts glucose to mmol/L independently of the unit system', () => {
    expect(vitalToDisplay('BLOOD_GLUCOSE', 90, MMOL)).toBeCloseTo(5.0, 1);
    expect(vitalFromDisplay('BLOOD_GLUCOSE', 5.5, MMOL)).toBe(99.1);
    expect(vitalUnit('BLOOD_GLUCOSE', MMOL)).toBe('mmol/L');
    expect(vitalDecimals('BLOOD_GLUCOSE', MMOL)).toBe(1);
    expect(vitalUnit('BLOOD_GLUCOSE', IMPERIAL)).toBe('mg/dL');
  });

  it('never converts blood pressure, heart rate or sleep', () => {
    expect(vitalToDisplay('BLOOD_PRESSURE', 120, IMPERIAL)).toBe(120);
    expect(vitalToDisplay('HEART_RATE', 64, MMOL)).toBe(64);
    expect(vitalFromDisplay('SLEEP', 7.5, IMPERIAL)).toBe(7.5);
  });

  it('converts distance and water', () => {
    expect(distanceToDisplay(5, IMPERIAL)).toBeCloseTo(3.107, 3);
    expect(distanceFromDisplay(3.1, IMPERIAL)).toBe(4.99);
    expect(waterToDisplay(500, IMPERIAL)).toBeCloseTo(16.9, 1);
    expect(waterFromDisplay(8, IMPERIAL)).toBe(237);
    expect(waterFromDisplay(250, METRIC)).toBe(250);
  });
});
