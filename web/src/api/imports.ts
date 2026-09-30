import { api } from './client';
import type { VitalPayload } from './vitals';

export interface ImportResult {
  /** True when nothing was saved: a preview, or a file with errors. */
  dryRun: boolean;
  rows: number;
  imported: number;
  duplicates: number;
  errors: { line: number; message: string }[];
  preview: VitalPayload[];
}

export const importVitals = (file: File, dryRun: boolean) =>
  api.upload<ImportResult>(`/import/vitals${dryRun ? '?dryRun=true' : ''}`, file, file.name);
