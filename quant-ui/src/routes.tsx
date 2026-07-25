import { Routes, Route, Navigate } from 'react-router-dom';
import DashboardPage from './pages/Dashboard/DashboardPage';
import BacktestRunnerPage from './pages/Backtest/BacktestRunnerPage';
import ResultsPage from './pages/Results/ResultsPage';

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/backtest" element={<BacktestRunnerPage />} />
      <Route path="/results/:jobId" element={<ResultsPage />} />
    </Routes>
  );
}

export default AppRoutes;
