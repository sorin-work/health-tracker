import { lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router';
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards';
import { Layout } from './components/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';

// Everything behind the dashboard is loaded on first visit, so signing in does not wait for it.
const ActivityPage = lazy(() => import('./pages/ActivityPage').then((m) => ({ default: m.ActivityPage })));
const ExportPage = lazy(() => import('./pages/ExportPage').then((m) => ({ default: m.ExportPage })));
const GoalsPage = lazy(() => import('./pages/GoalsPage').then((m) => ({ default: m.GoalsPage })));
const ImportPage = lazy(() => import('./pages/ImportPage').then((m) => ({ default: m.ImportPage })));
const JournalPage = lazy(() => import('./pages/JournalPage').then((m) => ({ default: m.JournalPage })));
const MedicationsPage = lazy(() => import('./pages/MedicationsPage').then((m) => ({ default: m.MedicationsPage })));
const MoodPage = lazy(() => import('./pages/MoodPage').then((m) => ({ default: m.MoodPage })));
const ReportPage = lazy(() => import('./pages/ReportPage').then((m) => ({ default: m.ReportPage })));
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })));
const SymptomsPage = lazy(() => import('./pages/SymptomsPage').then((m) => ({ default: m.SymptomsPage })));
const VitalsPage = lazy(() => import('./pages/VitalsPage').then((m) => ({ default: m.VitalsPage })));
const WaterPage = lazy(() => import('./pages/WaterPage').then((m) => ({ default: m.WaterPage })));

export function App() {
  return (
    <Routes>
      <Route element={<RedirectIfAuthenticated />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="/vitals" element={<VitalsPage />} />
          <Route path="/medications" element={<MedicationsPage />} />
          <Route path="/symptoms" element={<SymptomsPage />} />
          <Route path="/mood" element={<MoodPage />} />
          <Route path="/water" element={<WaterPage />} />
          <Route path="/activity" element={<ActivityPage />} />
          <Route path="/goals" element={<GoalsPage />} />
          <Route path="/journal" element={<JournalPage />} />
          <Route path="/export" element={<ExportPage />} />
          <Route path="/import" element={<ImportPage />} />
          <Route path="/report" element={<ReportPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
