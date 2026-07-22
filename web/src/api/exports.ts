import { api, toQuery } from './client';

export type ExportDataset = 'vitals' | 'medications' | 'doses' | 'symptoms' | 'activities' | 'mood' | 'water' | 'journal';

/** Downloads a CSV export and hands it to the browser as a file. */
export async function downloadExport(dataset: ExportDataset, from?: string, to?: string): Promise<string> {
  const { blob, filename } = await api.download(`/export/${dataset}.csv${toQuery({ from, to })}`);
  const name = filename ?? `health-tracker-${dataset}.csv`;
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return name;
}
