import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Mail, Lock, User, Building2, Eye, EyeOff } from 'lucide-react';
import Toast from '../components/Toast';
import { useToast } from '../hooks/useToast';
import './Auth.css';

/**
 * Register page — creates a new student account on Rentify.
 */
export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '', campus: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();
  const { toast, showToast } = useToast();

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    try {
      await register(form);
      navigate('/');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-icon-box">
            <User size={32} />
          </div>
          <h1 className="auth-title">Create Account</h1>
          <p className="auth-subtitle">Join the student marketplace</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} id="register-form">
          <div className="form-group">
            <label className="form-label" htmlFor="reg-name">Full Name</label>
            <input
              type="text"
              id="reg-name"
              name="name"
              className="form-input"
              placeholder="John Doe"
              value={form.name}
              onChange={handleChange}
              required
              minLength={2}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-email">Email Address</label>
            <input
              type="email"
              id="reg-email"
              name="email"
              className="form-input"
              placeholder="you@university.edu"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-password">Password</label>
            <input
              type={showPassword ? 'text' : 'password'}
              id="reg-password"
              name="password"
              className="form-input"
              placeholder="Min 6 characters"
              value={form.password}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-campus">Campus (optional)</label>
            <input
              type="text"
              id="reg-campus"
              name="campus"
              className="form-input"
              placeholder="e.g. MIT, Stanford"
              value={form.campus}
              onChange={handleChange}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-lg id-rent-submit"
            disabled={loading}
            id="register-submit"
            style={{ width: '100%', marginTop: '8px' }}
          >
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign In</Link>
        </p>
      </div>
    </div>
  );
}
