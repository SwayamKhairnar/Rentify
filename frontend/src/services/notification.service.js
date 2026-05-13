import api from './api';

/**
 * Frontend service for managing user notifications.
 */
export const notificationService = {
  /**
   * Fetches all notifications and unread count for the current user.
   */
  getMyNotifications: async () => {
    return api.get('/notifications');
  },

  /**
   * Marks a specific notification as read.
   */
  markAsRead: async (id) => {
    return api.patch(`/notifications/${id}/mark-read`);
  },

  /**
   * Marks all notifications for the current user as read.
   */
  markAllAsRead: async () => {
    return api.patch('/notifications/mark-all-read');
  },
};
