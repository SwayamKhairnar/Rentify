import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { Bell, Check, ArrowRight, Trash2, Calendar } from 'lucide-react';
import { formatDate } from '../utils/helpers';
import LoadingSpinner from '../components/LoadingSpinner';
import './Notifications.css';

/**
 * Notifications page — displays a list of all user alerts.
 */
export default function Notifications() {
  const navigate = useNavigate();
  const { notifications, unreadCount, markRead, markAllRead, loading } = useNotifications();

  function handleNotificationClick(notification) {
    markRead(notification._id);
    if (notification.link) {
      navigate(notification.link);
    }
  }

  return (
    <div className="page notifications-page">
      <div className="container narrow">
        <div className="notifications-header">
          <div className="header-title">
            <Bell size={24} className="accent-icon" />
            <h1>Notifications</h1>
            {unreadCount > 0 && <span className="unread-badge">{unreadCount} New</span>}
          </div>
          {notifications.length > 0 && (
            <button className="btn btn-ghost btn-sm" onClick={markAllRead}>
              <Check size={16} /> Mark all as read
            </button>
          )}
        </div>

        <div className="notifications-list card">
          {loading ? (
            <LoadingSpinner />
          ) : notifications.length === 0 ? (
            <div className="empty-notifications">
              <div className="empty-icon-circle">
                <Bell size={48} />
              </div>
              <h3>All caught up!</h3>
              <p>You have no notifications at the moment. We'll alert you when something happens.</p>
              <button className="btn btn-primary" onClick={() => navigate('/')}>
                Browse Items
              </button>
            </div>
          ) : (
            notifications.map((n) => (
              <div
                key={n._id}
                className={`notification-item ${n.isRead ? 'read' : 'unread'}`}
                onClick={() => handleNotificationClick(n)}
              >
                <div className="n-icon">
                  <div className={`n-icon-inner ${n.type}`}>
                    {n.type === 'rental_request' && <Bell size={18} />}
                    {n.type === 'rental_status' && <Check size={18} />}
                    {n.type === 'review_received' && <Bell size={18} />}
                  </div>
                </div>
                <div className="n-content">
                  <div className="n-meta">
                    <span className="n-type">{n.title}</span>
                    <span className="n-date">
                      <Calendar size={12} /> {formatDate(n.createdAt)}
                    </span>
                  </div>
                  <h4 className="n-message">{n.message}</h4>
                  {n.link && (
                    <div className="n-action">
                      View Details <ArrowRight size={14} />
                    </div>
                  )}
                </div>
                {!n.isRead && <div className="unread-dot"></div>}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
