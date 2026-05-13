import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { notificationService } from '../services/notification.service';
import { useAuth } from '../hooks/useAuth';

const NotificationContext = createContext(null);

/**
 * NotificationProvider — manages global notification state.
 * Polling happens once here, and state is shared across all components.
 */
export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchNotifications = useCallback(async () => {
    if (!user) return;
    try {
      const res = await notificationService.getMyNotifications();
      setNotifications(res.data.notifications || []);
      setUnreadCount(res.data.unreadCount || 0);
    } catch (err) {
      console.error('Failed to fetch notifications');
    }
  }, [user]);

  // Initial fetch and global poll (30s)
  useEffect(() => {
    let timer;
    if (user) {
      fetchNotifications();
      timer = setInterval(fetchNotifications, 30000);
    } else {
      setNotifications([]);
      setUnreadCount(0);
    }

    return () => timer && clearInterval(timer);
  }, [user, fetchNotifications]);

  const markRead = async (id) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications(prev => 
        prev.map(n => n._id === id ? { ...n, isRead: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Failed to mark as read');
    }
  };

  const markAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read');
    }
  };

  return (
    <NotificationContext.Provider value={{ 
      notifications, 
      unreadCount, 
      loading, 
      refresh: fetchNotifications, 
      markRead, 
      markAllRead 
    }}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
