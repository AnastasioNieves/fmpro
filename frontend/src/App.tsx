import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Dashboard } from './pages/Dashboard';
import { Teams } from './pages/Teams';
import { Players } from './pages/Players';
import { Matches } from './pages/Matches';
import { MatchDetail } from './pages/MatchDetail';
import { Statistics } from './pages/Statistics';
import { Login } from './pages/Login';

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter basename={import.meta.env.BASE_URL.replace(/\/$/, '') || undefined}>
          <Routes>
            <Route path="login" element={<Login />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route index element={<Dashboard />} />
                <Route path="equipos" element={<Teams />} />
                <Route path="jugadores" element={<Players />} />
                <Route path="partidos" element={<Matches />} />
                <Route path="partidos/:id" element={<MatchDetail />} />
                <Route path="estadisticas" element={<Statistics />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}
