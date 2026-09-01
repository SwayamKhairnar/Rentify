import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './hooks/useAuth';
import { ThemeProvider } from './context/ThemeContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ItemDetails from './pages/ItemDetails';
import CreateItem from './pages/CreateItem';
import EditItem from './pages/EditItem';
import RentalRequests from './pages/RentalRequests';
import RentalDetail from './pages/RentalDetail';
import Chat from './pages/Chat';
import Profile from './pages/Profile';
import UserProfile from './pages/UserProfile';
import Notifications from './pages/Notifications';
import AdminDashboard from './pages/AdminDashboard';
import AdminRoute from './components/AdminRoute';

import { NotificationProvider } from './context/NotificationContext';

/**
 * Root App component — sets up routing, theme, and auth context.
 */
export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <NotificationProvider>
            <Navbar />
            <Routes>
              {/* Public routes */}
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/items/:id" element={<ItemDetails />} />
              <Route path="/profile/:id" element={<UserProfile />} />

              {/* Protected routes */}
              <Route path="/items/new" element={<ProtectedRoute><CreateItem /></ProtectedRoute>} />
              <Route path="/items/:id/edit" element={<ProtectedRoute><EditItem /></ProtectedRoute>} />
              <Route path="/rentals" element={<ProtectedRoute><RentalRequests /></ProtectedRoute>} />
              <Route path="/rentals/:id" element={<ProtectedRoute><RentalDetail /></ProtectedRoute>} />
              <Route path="/chat" element={<ProtectedRoute><Chat /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
              <Route path="/notifications" element={<ProtectedRoute><Notifications /></ProtectedRoute>} />

              {/* Admin-only routes */}
              <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}
