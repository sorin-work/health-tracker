import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { importVitals, type ImportResult } from '../api/imports';
import { useUnitPrefs } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { PageHeader } from '../components/PageHeader';
import { formatDateTime } from '../lib/dates';
import { formatReading, GLUCOSE_CONTEXT_LABELS, VITAL_META } from '../lib/format';

const EXAMPLE = `measured_at,type,value,secondary_value,glucose_context,notes
2026-09-01 07:30,WEIGHT,82.4,,,
2026-09-01 08:00,BLOOD_PRESSURE,128,82,,
2026-09-02 06:45,BLOOD_GLUCOSE,96,,FASTING,before breakfast`;

/** Upload a CSV, check the preview, then import. Nothing is saved until the second step. */
export function ImportPage() {
  const queryClient = useQueryClient();
  const prefs = useUnitPrefs();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
  const [done, setDone] = useState<ImportResult | null>(null);

  const check = useMutation({
    mutationFn: (f: File) => importVitals(f, true),
    onSuccess: setPreview,
  });
  const save = useMutation({
    mutationFn: (f: File) => importVitals(f, false),
    onSuccess: (result) => {
      setDone(result);
      setPreview(null);
      setFile(null);
      queryClient.invalidateQueries({ queryKey: ['vitals'] });
      queryClient.invalidateQueries({ queryKey: ['goals'] });
    },
  });

  function choose(next: File | null) {
    setFile(next);
    setPreview(null);
    setDone(null);
    check.reset();
    save.reset();
    if (next) check.mutate(next);
  }

  const toImport = preview ? preview.rows - preview.duplicates - preview.errors.length : 0;

  return (
    <>
      <PageHeader
        title="Import"
        description="Bring in readings from another app or a spreadsheet. Times without a time zone are read in yours."
      />
      <div className="grid grid--2">
        <section className="card">
          <h2>Vitals from a CSV file</h2>
          <div className="field">
            <label htmlFor="import-file">CSV file</label>
            <input
              id="import-file"
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => choose(e.target.files?.[0] ?? null)}
            />
          </div>
          <ErrorMessage error={check.error ?? save.error} />
          {check.isPending && <p className="muted">Checking the file…</p>}

          {preview && (
            <div className="import-preview">
              <p>
                <strong>{preview.rows}</strong> rows · {toImport} new
                {preview.duplicates > 0 && ` · ${preview.duplicates} already in your log`}
                {preview.errors.length > 0 && ` · ${preview.errors.length} with problems`}
              </p>
              {preview.errors.length > 0 && (
                <div className="alert alert--error" role="alert">
                  Fix these rows and choose the file again. Nothing has been imported.
                  <ul>
                    {preview.errors.slice(0, 20).map((e) => (
                      <li key={e.line}>
                        Line {e.line}: {e.message}
                      </li>
                    ))}
                  </ul>
                  {preview.errors.length > 20 && <p>…and {preview.errors.length - 20} more.</p>}
                </div>
              )}
              {preview.preview.length > 0 && (
                <div className="table-wrap">
                  <table className="table">
                    <caption className="muted small">First rows</caption>
                    <thead>
                      <tr>
                        <th>When</th>
                        <th>Type</th>
                        <th className="num">Reading</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.preview.map((r, i) => (
                        <tr key={i}>
                          <td>{formatDateTime(r.measuredAt)}</td>
                          <td>
                            {VITAL_META[r.type].label}
                            {r.glucoseContext && (
                              <span className="muted small"> · {GLUCOSE_CONTEXT_LABELS[r.glucoseContext]}</span>
                            )}
                          </td>
                          <td className="num">
                            {formatReading({ ...r, secondaryValue: r.secondaryValue ?? undefined }, prefs)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <div className="form__actions">
                <button
                  className="button"
                  disabled={!file || preview.errors.length > 0 || toImport === 0 || save.isPending}
                  onClick={() => file && save.mutate(file)}
                >
                  {save.isPending ? 'Importing…' : `Import ${toImport} readings`}
                </button>
              </div>
            </div>
          )}

          {done && (
            <div className="alert alert--success" role="status">
              Imported {done.imported} readings
              {done.duplicates > 0 && `, skipped ${done.duplicates} that were already there`}.
            </div>
          )}
        </section>

        <section className="card">
          <h2>File format</h2>
          <p className="small">
            The first row names the columns; <code>measured_at</code>, <code>type</code> and <code>value</code> are
            required. A vitals export from this app can be imported as it is.
          </p>
          <ul className="small">
            <li>
              <code>type</code>: WEIGHT, BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE or SLEEP
            </li>
            <li>
              Values are always in kg, mmHg, bpm, mg/dL and hours, whichever units the app shows. Blood pressure
              puts the diastolic number in <code>secondary_value</code>.
            </li>
            <li>
              <code>glucose_context</code>: FASTING, BEFORE_MEAL, AFTER_MEAL, BEDTIME or RANDOM
            </li>
          </ul>
          <pre className="code-sample">{EXAMPLE}</pre>
        </section>
      </div>
    </>
  );
}
