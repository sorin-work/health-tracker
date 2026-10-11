import { api, toQuery } from './client';
import type { JournalEntry } from './journal';
import type { AdherenceStats, DayOfWeek, ScheduleType } from './medications';
import type { VitalReading, VitalType } from './vitals';

export interface DoctorReport {
  patient: string;
  timezone: string;
  from: string;
  to: string;
  generatedAt: string;
  vitals: {
    type: VitalType;
    unit: string;
    count: number;
    average?: number;
    averageSecondary?: number;
    min?: number;
    max?: number;
    flagged: number;
  }[];
  flaggedReadings: VitalReading[];
  flaggedTotal: number;
  adherence: AdherenceStats;
  medications: {
    name: string;
    dosage?: string;
    schedule: ScheduleType | 'AS_NEEDED';
    times: string[];
    intervalDays?: number;
    daysOfWeek: DayOfWeek[];
    startDate: string;
    endDate?: string;
    asNeeded: boolean;
    adherence?: AdherenceStats;
    asNeededTaken: number;
  }[];
  symptoms: { name: string; count: number; averageSeverity: number; maxSeverity: number }[];
  mood: { checkIns: number; averageMood?: number; averageEnergy?: number };
  activity: { sessions: number; activeMinutes: number; averageMinutesPerWeek: number };
  notes: JournalEntry[];
}

export const doctorReport = (from?: string, to?: string) => api.get<DoctorReport>(`/report${toQuery({ from, to })}`);
