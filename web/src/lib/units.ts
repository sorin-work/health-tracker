import type { VitalType } from '../api/vitals';

/** Everything is stored in metric units; these only change what the user sees and types. */
export type UnitSystem = 'METRIC' | 'IMPERIAL';
export type GlucoseUnit = 'MG_DL' | 'MMOL_L';

export interface UnitPrefs {
  unitSystem: UnitSystem;
  glucoseUnit: GlucoseUnit;
}

export const METRIC: UnitPrefs = { unitSystem: 'METRIC', glucoseUnit: 'MG_DL' };

const LB_PER_KG = 2.20462262;
const MI_PER_KM = 0.621371192;
const ML_PER_FL_OZ = 29.5735296;
/** mg/dL of glucose per mmol/L (molar mass of glucose / 10). */
const MGDL_PER_MMOL = 18.0182;

export const kgToLb = (kg: number) => kg * LB_PER_KG;
export const lbToKg = (lb: number) => lb / LB_PER_KG;
export const kmToMi = (km: number) => km * MI_PER_KM;
export const miToKm = (mi: number) => mi / MI_PER_KM;
export const mlToFlOz = (ml: number) => ml / ML_PER_FL_OZ;
export const flOzToMl = (oz: number) => oz * ML_PER_FL_OZ;
export const mgdlToMmol = (mgdl: number) => mgdl / MGDL_PER_MMOL;
export const mmolToMgdl = (mmol: number) => mmol * MGDL_PER_MMOL;

const imperial = (prefs: UnitPrefs) => prefs.unitSystem === 'IMPERIAL';
const mmol = (prefs: UnitPrefs) => prefs.glucoseUnit === 'MMOL_L';

/** A stored (metric) vital value in the user's units. Blood pressure, heart rate and sleep never change. */
export function vitalToDisplay(type: VitalType, value: number, prefs: UnitPrefs): number {
  if (type === 'WEIGHT' && imperial(prefs)) return kgToLb(value);
  if (type === 'BLOOD_GLUCOSE' && mmol(prefs)) return mgdlToMmol(value);
  return value;
}

/** A value typed in the user's units, converted back to what the API stores. */
export function vitalFromDisplay(type: VitalType, value: number, prefs: UnitPrefs): number {
  if (type === 'WEIGHT' && imperial(prefs)) return round(lbToKg(value), 2);
  if (type === 'BLOOD_GLUCOSE' && mmol(prefs)) return round(mmolToMgdl(value), 1);
  return value;
}

export function vitalUnit(type: VitalType, prefs: UnitPrefs): string {
  switch (type) {
    case 'WEIGHT':
      return imperial(prefs) ? 'lb' : 'kg';
    case 'BLOOD_GLUCOSE':
      return mmol(prefs) ? 'mmol/L' : 'mg/dL';
    case 'BLOOD_PRESSURE':
      return 'mmHg';
    case 'HEART_RATE':
      return 'bpm';
    case 'SLEEP':
      return 'h';
  }
}

export function vitalDecimals(type: VitalType, prefs: UnitPrefs): number {
  switch (type) {
    case 'WEIGHT':
    case 'SLEEP':
      return 1;
    case 'BLOOD_GLUCOSE':
      return mmol(prefs) ? 1 : 0;
    default:
      return 0;
  }
}

export const distanceUnit = (prefs: UnitPrefs) => (imperial(prefs) ? 'mi' : 'km');
export const distanceToDisplay = (km: number, prefs: UnitPrefs) => (imperial(prefs) ? kmToMi(km) : km);
export const distanceFromDisplay = (value: number, prefs: UnitPrefs) =>
  imperial(prefs) ? round(miToKm(value), 2) : value;

export const waterUnit = (prefs: UnitPrefs) => (imperial(prefs) ? 'fl oz' : 'ml');
export const waterToDisplay = (ml: number, prefs: UnitPrefs) => (imperial(prefs) ? mlToFlOz(ml) : ml);
export const waterFromDisplay = (value: number, prefs: UnitPrefs) =>
  imperial(prefs) ? Math.round(flOzToMl(value)) : Math.round(value);

function round(value: number, decimals: number): number {
  const f = 10 ** decimals;
  return Math.round(value * f) / f;
}
