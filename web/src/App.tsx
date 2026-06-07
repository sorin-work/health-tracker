import { Navigate, Route, Routes } from 'react-router';
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards';
import { Layout } from './components/Layout';
import { ActivityPage } from './pages/ActivityPage';
import { DashboardPage } from './pages/DashboardPage';
import { ExportPage } from './pages/ExportPage';
import { GoalsPage } from './pages/GoalsPage';
import { LoginPage } from './pages/LoginPage';
import { MedicationsPage } from './pages/MedicationsPage';
import { MoodPage } from './pages/MoodPage';
import { RegisterPage } from './pages/RegisterPage';
import { SettingsPage } from './pages/SettingsPage';
import { SymptomsPage } from './pages/SymptomsPage';
import { VitalsPage } from './pages/VitalsPage';
import { WaterPage } from './pages/WaterPage';

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
          <Route path="/export" element={<ExportPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
