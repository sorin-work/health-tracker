/**
 * Y axis bounds with some room above and below the data. A flat line still gets a little space,
 * scaled to the unit: 2 for whole numbers (mmHg, bpm, mg/dL), 0.5 for values with decimals (kg, h,
 * mmol/L), where a padding of 2 would squash the line flat.
 */
export function yDomain(values: number[], decimals: number): [number, number] {
  const min = Math.min(...values);
  const max = Math.max(...values);
  const pad = Math.max((max - min) * 0.1, decimals > 0 ? 0.5 : 2);
  const step = decimals > 0 ? 0.5 : 1;
  return [Math.floor((min - pad) / step) * step, Math.ceil((max + pad) / step) * step];
}
