import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { adminService } from '../services/admin.service';
import { reportService } from '../services/report.service';
import { useToast } from '../hooks/useToast';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import { Users, Trash2, Mail, Shield, User, Calendar, Search, AlertTriangle, MessageCircle, Eye, CheckCircle, XCircle, Clock } from 'lucide-react';
import { formatDate } from '../utils/helpers';
import './AdminDashboard.css';

/**
 * AdminDashboard page — allows super admins to manage users and system state.
 */
export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('users'); // 'users' or 'reports'
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [replyingTo, setReplyingTo] = useState(null);
  const [adminReply, setAdminReply] = useState('');
  const [selectedAction, setSelectedAction] = useState('none');
  const { toast, showToast } = useToast();

  useEffect(() => {
    if (activeTab === 'users') {
      fetchUsers();
    } else {
      fetchReports();
    }
  }, [activeTab]);

  async function fetchUsers() {
    setLoading(true);
    try {
      const res = await adminService.getAllUsers();
      setUsers(res.data.users);
    } catch (err) {
      showToast('Failed to load users', 'error');
    } finally {
      setLoading(false);
    }
  }

  async function fetchReports() {
    setLoading(true);
    try {
      const res = await reportService.getAdminReports();
      setReports(res.data.reports);
    } catch (err) {
      showToast('Failed to load reports', 'error');
    } finally {
      setLoading(false);
    }
  }

  async function handleReportReply(e) {
    e.preventDefault();
    if (!adminReply.trim()) return;

    try {
      await reportService.respondToReport(replyingTo._id, {
        message: adminReply,
        status: 'reviewed',
        action: selectedAction
      });
      showToast('Response sent to reporter');
      setReplyingTo(null);
      setAdminReply('');
      setSelectedAction('none');
      fetchReports();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function handleDeleteUser(id, name) {
    if (!window.confirm(`Are you sure you want to delete ${name}? This will remove ALL their data, items, and rentals. This action CANNOT be undone.`)) {
      return;
    }

    setDeletingId(id);
    try {
      await adminService.deleteUser(id);
      showToast(`User ${name} and all associated data deleted.`);
      setUsers(users.filter(u => u._id !== id));
    } catch (err) {
      showToast(err.message || 'Failed to delete user', 'error');
    } finally {
      setDeletingId(null);
    }
  }

  const filteredUsers = users.filter(u => 
    u.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) return <LoadingSpinner />;

  return (
    <div className="page admin-dashboard">
      {toast && <Toast message={toast.message} type={toast.type} />}
      
      <div className="container">
        <div className="admin-header">
          <div className="admin-title-wrap">
            <div className="admin-icon">
              <Shield size={24} />
            </div>
            <div>
              <h1>Admin Dashboard</h1>
              <p>System Management & User Moderation</p>
            </div>
          </div>
          
          <div className="admin-stats">
            <div className="stat-card">
              <div className="stat-val">{users.length}</div>
              <div className="stat-label">Total Users</div>
            </div>
          </div>
        </div>

        <div className="admin-tabs">
          <button 
            className={`admin-tab-btn ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
          >
            <Users size={18} />
            Users
          </button>
          <button 
            className={`admin-tab-btn ${activeTab === 'reports' ? 'active' : ''}`}
            onClick={() => setActiveTab('reports')}
          >
            <AlertTriangle size={18} />
            Reports
            {reports.filter(r => r.status === 'pending').length > 0 && (
              <span className="tab-badge">{reports.filter(r => r.status === 'pending').length}</span>
            )}
          </button>
        </div>

        <div className="admin-controls card">
          <div className="search-wrap">
            <Search size={18} />
            <input 
              type="text" 
              placeholder={activeTab === 'users' ? "Search users by name or email..." : "Search reports..."} 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="admin-search"
            />
          </div>
        </div>

        <div className="admin-content card">
          {activeTab === 'users' ? (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  <th>Joined</th>
                  <th>Rating</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-table">No users found matching your search.</td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => (
                    <tr key={u._id}>
                      <td>
                        <div className="table-user">
                          <div className="table-avatar">
                            {u.name.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div className="u-name">{u.name}</div>
                            <div className="u-email">{u.email}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="admin-role-wrap">
                          <span className={`role-badge ${u.role}`}>
                            {u.role === 'admin' ? <Shield size={12} /> : <User size={12} />}
                            {u.role}
                          </span>
                          {u.isSuspended && (
                            <span className="badge badge-rejected" style={{ marginLeft: '4px', fontSize: '0.65rem' }}>
                              Suspended
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <div className="u-date">
                          <Calendar size={13} />
                          {formatDate(u.createdAt)}
                        </div>
                      </td>
                      <td>
                        <div className="u-rating">
                          ★ {u.rating.toFixed(1)}
                        </div>
                      </td>
                      <td>
                        <button 
                          className="btn-delete"
                          onClick={() => handleDeleteUser(u._id, u.name)}
                          disabled={deletingId === u._id}
                          title="Delete User & Data"
                        >
                          {deletingId === u._id ? '...' : <Trash2 size={18} />}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Reporter</th>
                  <th>Reported User</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {reports.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-table">No reports filed yet.</td>
                  </tr>
                ) : (
                  reports.map((r) => (
                    <tr key={r._id}>
                      <td>
                        <div className="table-user">
                          <div className="u-name">{r.reporter?.name}</div>
                          <div className="u-email">{r.reporter?.email}</div>
                        </div>
                      </td>
                      <td>
                        <div className="table-user">
                          <div className="u-name">{r.reportedUser?.name}</div>
                          <div className="u-email">{r.reportedUser?.email}</div>
                        </div>
                      </td>
                      <td>
                        <div className="report-reason-cell">
                          <span className="reason-text">{r.reason}</span>
                          <div className="reason-date">{formatDate(r.createdAt)}</div>
                        </div>
                      </td>
                      <td>
                        <div className="report-status-cell">
                          <span className={`status-pill ${r.status}`}>
                            {r.status === 'pending' && <Clock size={12} />}
                            {r.status === 'reviewed' && <CheckCircle size={12} />}
                            {r.status}
                          </span>
                          {r.adminAction && r.adminAction !== 'none' && (
                            <span className="action-tag">
                              {r.adminAction.replace('_', ' ')}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <div className="report-actions">
                          <button 
                            className="btn btn-primary btn-sm btn-action"
                            onClick={() => setReplyingTo(r)}
                            title="Review Case & Take Action"
                          >
                            <AlertTriangle size={14} /> Action
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Reply Modal */}
      {replyingTo && (
        <div className="rm-overlay" onClick={(e) => e.target === e.currentTarget && setReplyingTo(null)}>
          <div className="report-detail-modal card">
            <div className="modal-header">
              <h3>Case #{replyingTo._id.slice(-6).toUpperCase()}</h3>
              <button className="btn-close" onClick={() => setReplyingTo(null)}><XCircle size={20} /></button>
            </div>
            
            <div className="modal-body">
              <div className="report-grid">
                <div className="grid-item">
                  <label>Reporter</label>
                  <p>{replyingTo.reporter?.name}</p>
                </div>
                <div className="grid-item">
                  <label>Reported User</label>
                  <p>{replyingTo.reportedUser?.name}</p>
                </div>
              </div>

              <div className="report-info-section">
                <label>Complaint</label>
                <div className="complaint-bubble">
                  <strong>{replyingTo.reason}</strong>
                  <p>{replyingTo.description}</p>
                </div>
              </div>

              {replyingTo.evidenceImage && (
                <div className="report-info-section">
                  <label>Evidence Image</label>
                  <img src={replyingTo.evidenceImage} alt="Evidence" className="evidence-img-large" />
                </div>
              )}

              {replyingTo.adminNotes && (
                <div className="report-info-section">
                  <label>Previous Admin Note</label>
                  <div className="admin-note-bubble">
                    <p>{replyingTo.adminNotes}</p>
                  </div>
                </div>
              )}

              <div className="report-action-header">
                <Shield size={18} />
                <h4>Take Administrative Action</h4>
              </div>

              <form onSubmit={handleReportReply} className="reply-form">
                <div className="form-group">
                  <label>Select Action</label>
                  <select 
                    className="admin-select"
                    value={selectedAction}
                    onChange={(e) => setSelectedAction(e.target.value)}
                    required
                  >
                    <option value="none">No Action Taken</option>
                    <option value="warned">Official Warning</option>
                    <option value="listing_removed">Remove Relevant Listing</option>
                    <option value="account_suspended">Suspend User Account</option>
                    <option value="resolved">Mark as Resolved</option>
                  </select>
                  <p className="form-help">The selected action will be implemented and the reporter will be notified.</p>
                </div>

                <div className="form-group">
                  <label>Message to Reporter</label>
                  <textarea 
                    placeholder="Briefly explain the resolution to the reporter..."
                    value={adminReply}
                    onChange={(e) => setAdminReply(e.target.value)}
                    rows={3}
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-block">
                  <CheckCircle size={16} /> Finalize Resolution
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
