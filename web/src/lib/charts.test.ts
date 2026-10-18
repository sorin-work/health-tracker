import { describe, expect, it } from 'vitest';
import { yDomain } from './charts';

describe('yDomain', () => {
  it('pads whole-number vitals by at least 2', () => {
    expect(yDomain([120, 120], 0)).toEqual([118, 122]);
    expect(yDomain([100, 200], 0)).toEqual([90, 210]);
  });

  it('keeps mmol/L glucose readable', () => {
    // A padding of 2 would stretch 5.4–5.8 mmol/L over 3–8 and flatten the line.
    expect(yDomain([5.4, 5.8], 1)).toEqual([4.5, 6.5]);
  });

  it('pads sleep and weight by half a unit', () => {
    expect(yDomain([7.5, 7.5], 1)).toEqual([7, 8]);
  });
});
