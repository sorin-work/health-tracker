import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { doctorReport, type DoctorReport } from '../api/report';
import { useUnitPrefs } from '../auth/AuthContext';
import { DateRangePicker } from '../components/DateRangePicker';
import { Disclaimer } from '../components/Disclaimer';
import { ErrorMessage } from '../components/ErrorMessage';
import { FlagBadge } from '../components/FlagBadge';
import { formatDate, formatDateTime, presetRange, type DateRange } from '../lib/dates';
import {
  describeSchedule,
  formatMinutes,
  formatNumber,
  formatReading,
  formatVitalValue,
  moodLabel,
  severityLabel,
  VITAL_META,
} from '../lib/format';
import { vitalUnit, type UnitPrefs } from '../lib/units';

/** A one-page summary to print (or save as PDF) and bring to an appointment. */
export function ReportPage() {
  const [range, setRange] = useState<DateRange>(presetRange('90d'));
  const report = useQuery({
    queryKey: ['report', range.from, range.to],
    queryFn: () => doctorReport(range.from, range.to),
  });
  const prefs = useUnitPrefs();

  return (
    <div className="report">
      <div className="page-header no-print">
        <div>
          <h1>Doctor report</h1>
          <p className="muted">Pick a period, check the summary, then print it or save it as a PDF.</p>
        </div>
        <div className="page-header__actions">
          <button className="button" onClick={() => window.print()} disabled={!report.data}>
            Print or save as PDF
          </button>
        </div>
      </div>
      <div className="card no-print">
        <DateRangePicker value={range} onChange={setRange} />
      </div>
      <ErrorMessage error={report.error} />
      {report.isPending && <p className="muted">Putting the report together…</p>}
      {report.data && <ReportBody report={report.data} prefs={prefs} />}
    </div>
  );
}

function ReportBody({ report, prefs }: { report: DoctorReport; prefs: UnitPrefs }) {
  return (
    <article className="report__sheet" aria-label="Report">
      <header className="report__header">
        <div>
          <h2>Health summary: {report.patient}</h2>
          <p>
            {formatDate(report.from)} – {formatDate(report.to)} · times in {report.timezone}
          </p>
        </div>
        <p className="muted small">Generated {formatDateTime(report.generatedAt)} with Health Tracker</p>
      </header>

      <section>
        <h3>Vitals</h3>
        {report.vitals.length === 0 ? (
          <p className="muted">No readings in this period.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Measure</th>
                <th className="num">Readings</th>
                <th className="num">Average</th>
                <th className="num">Lowest – highest</th>
                <th className="num">Out of range</th>
              </tr>
            </thead>
            <tbody>
              {report.vitals.map((v) => (
                <tr key={v.type}>
                  <td>{VITAL_META[v.type].label}</td>
                  <td className="num">{v.count}</td>
                  <td className="num">
                    {v.average === undefined ? '–' : formatVitalValue(v.type, v.average, v.averageSecondary, prefs)}{' '}
                    {vitalUnit(v.type, prefs)}
                  </td>
                  <td className="num">
                    {v.min === undefined || v.max === undefined
                      ? '–'
                      : `${formatVitalValue(v.type, v.min, null, prefs)} – ${formatVitalValue(v.type, v.max, null, prefs)}`}
                  </td>
                  <td className="num">{v.flagged}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {report.flaggedReadings.length > 0 && (
        <section>
          <h3>
            Out-of-range readings
            {report.flaggedTotal > report.flaggedReadings.length &&
              ` (latest ${report.flaggedReadings.length} of ${report.flaggedTotal})`}
          </h3>
          <table className="table">
            <thead>
              <tr>
                <th>When</th>
                <th>Measure</th>
                <th className="num">Reading</th>
                <th>Flag</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {report.flaggedReadings.map((r) => (
                <tr key={r.id}>
                  <td>{formatDateTime(r.measuredAt)}</td>
                  <td>{VITAL_META[r.type].label}</td>
                  <td className="num">{formatReading(r, prefs)}</td>
                  <td>
                    <FlagBadge flag={r.flag} />
                  </td>
                  <td>{r.notes}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <section>
        <h3>
          Medications
          {report.adherence.percent !== undefined && report.adherence.percent !== null && (
            <span className="muted"> · {formatNumber(report.adherence.percent, 0)}% of scheduled doses taken</span>
          )}
        </h3>
        {report.medications.length === 0 ? (
          <p className="muted">No medications in this period.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Medication</th>
                <th>Schedule</th>
                <th>Taken since</th>
                <th className="num">Adherence</th>
              </tr>
            </thead>
            <tbody>
              {report.medications.map((m) => (
                <tr key={`${m.name}-${m.startDate}`}>
                  <td>
                    <strong>{m.name}</strong> {m.dosage}
                  </td>
                  <td>
                    {describeSchedule({
                      asNeeded: m.asNeeded,
                      times: m.times,
                      scheduleType: m.schedule === 'AS_NEEDED' ? 'DAILY' : m.schedule,
                      intervalDays: m.intervalDays,
                      daysOfWeek: m.daysOfWeek,
                    })}
                  </td>
                  <td>
                    {formatDate(m.startDate)}
                    {m.endDate && ` until ${formatDate(m.endDate)}`}
                  </td>
                  <td className="num">
                    {m.asNeeded
                      ? `taken ${m.asNeededTaken}×`
                      : m.adherence?.percent !== undefined && m.adherence?.percent !== null
                        ? `${formatNumber(m.adherence.percent, 0)}% (${m.adherence.missed} missed)`
                        : '–'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section>
        <h3>Symptoms</h3>
        {report.symptoms.length === 0 ? (
          <p className="muted">No symptoms logged.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Symptom</th>
                <th className="num">Times</th>
                <th className="num">Average severity</th>
                <th className="num">Worst</th>
              </tr>
            </thead>
            <tbody>
              {report.symptoms.map((s) => (
                <tr key={s.name}>
                  <td>{s.name}</td>
                  <td className="num">{s.count}</td>
                  <td className="num">
                    {formatNumber(s.averageSeverity, 1)} / 10 ({severityLabel(s.averageSeverity)})
                  </td>
                  <td className="num">{s.maxSeverity} / 10</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="report__pair">
        <div>
          <h3>Mood</h3>
          {report.mood.checkIns === 0 || report.mood.averageMood === undefined ? (
            <p className="muted">No check-ins.</p>
          ) : (
            <p>
              {formatNumber(report.mood.averageMood, 1)} / 5 on average ({moodLabel(report.mood.averageMood)}) over{' '}
              {report.mood.checkIns} check-ins
              {report.mood.averageEnergy !== undefined && `, energy ${formatNumber(report.mood.averageEnergy, 1)} / 5`}
              .
            </p>
          )}
        </div>
        <div>
          <h3>Activity</h3>
          <p>
            {report.activity.sessions} sessions, {formatMinutes(report.activity.activeMinutes)} in total (about{' '}
            {formatMinutes(report.activity.averageMinutesPerWeek)} a week).
          </p>
        </div>
      </section>

      {report.notes.length > 0 && (
        <section>
          <h3>Notes</h3>
          <ul className="report__notes">
            {report.notes.map((n) => (
              <li key={n.id}>
                <strong>
                  {formatDate(n.entryDate)}
                  {n.title && ` · ${n.title}`}
                </strong>
                <p>{n.body}</p>
              </li>
            ))}
          </ul>
        </section>
      )}

      <footer className="report__footer">
        <Disclaimer />
      </footer>
    </article>
  );
}
