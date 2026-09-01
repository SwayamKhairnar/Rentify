import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useNotifications } from '../hooks/useNotifications';
import { useTheme } from '../hooks/useTheme';
import { LogOut, Plus, MessageCircle, Package, User, Menu, X, Bell, Shield, Sun, Moon } from 'lucide-react';
import { useState } from 'react';
import logo from '../assets/logo_new.png';
import './Navbar.css';

/**
 * Navbar — top navigation bar with Rentify branding, nav links, auth actions, and theme switch.
 */
export default function Navbar() {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotifications();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const isAdmin = user?.role === 'admin';

  const handleLogout = () => {
    logout();
    navigate('/');
    setMenuOpen(false);
  };

  return (
    <nav className="navbar" id="main-navbar">
      <div className="container navbar-inner">
        <Link to="/" className="navbar-brand" id="navbar-brand">
          <img src={logo} alt="Rentify" className="navbar-logo-img" />
          <span className="brand-text">Rentify</span>
        </Link>

        <div className="navbar-right-actions">
          {/* Theme Toggle Button */}
          <button
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} Mode`}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? (
              <Sun size={19} className="theme-icon sun-icon" />
            ) : (
              <Moon size={19} className="theme-icon moon-icon" />
            )}
          </button>

          <button
            className="navbar-toggle"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        <div className={`navbar-menu ${menuOpen ? 'open' : ''}`}>
          <Link to="/" className="navbar-link" onClick={() => setMenuOpen(false)}>
            <span className="nav-text">Browse</span>
          </Link>

          {user ? (
            <>
              {isAdmin && (
                <Link to="/admin" className="navbar-link admin-link" onClick={() => setMenuOpen(false)}>
                  <Shield size={18} />
                  <span className="nav-text">Admin</span>
                </Link>
              )}
              <Link to="/items/new" className="navbar-link" onClick={() => setMenuOpen(false)}>
                <Plus size={18} />
                <span className="nav-text">List Item</span>
              </Link>
              <Link to="/rentals" className="navbar-link" onClick={() => setMenuOpen(false)}>
                <Package size={18} />
                <span className="nav-text">Rentals</span>
              </Link>
              <Link to="/chat" className="navbar-link nav-icon-only" onClick={() => setMenuOpen(false)} title="Chat">
                <MessageCircle size={18} />
                <span className="nav-text">Chat</span>
              </Link>
              <Link to="/notifications" className="navbar-link nav-notification nav-icon-only" onClick={() => setMenuOpen(false)} title="Notifications">
                <div className="bell-wrap">
                  <Bell size={18} />
                  {unreadCount > 0 && <span className="notification-badge">{unreadCount}</span>}
                </div>
                <span className="nav-text">Notifications</span>
              </Link>
              <Link to="/profile" className="navbar-link nav-icon-only" onClick={() => setMenuOpen(false)} title="Profile">
                <User size={18} />
                <span className="nav-text">Profile</span>
              </Link>
              <button className="btn btn-ghost btn-sm" onClick={handleLogout} id="logout-btn">
                <LogOut size={16} />
                <span className="nav-text">Logout</span>
              </button>
            </>
          ) : (
            <div className="navbar-auth">
              <Link
                to="/login"
                className="btn btn-ghost btn-sm"
                onClick={() => setMenuOpen(false)}
                id="login-btn"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="btn btn-primary btn-sm"
                onClick={() => setMenuOpen(false)}
                id="register-btn"
              >
                Sign up
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
