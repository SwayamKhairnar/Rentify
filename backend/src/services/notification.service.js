const { Notification, User } = require('../models');

/**
 * Creates a new notification for a user.
 * Input: { recipientId, senderId, type, title, message, link }
 * Output: created notification object
 */
async function createNotification({ recipient, sender, type, title, message, link }) {
  return Notification.create({
    recipient,
    sender,
    type,
    title,
    message,
    link,
  });
}

/**
 * Sends a notification to all registered admins.
 */
async function notifyAdmins({ type, title, message, link, sender = null }) {
  const admins = await User.find({ role: 'admin' }).select('_id');
  if (admins.length === 0) return;

  const notifications = admins.map((admin) => ({
    recipient: admin._id,
    sender,
    type,
    title,
    message,
    link,
  }));

  return Notification.insertMany(notifications);
}

/**
 * Fetches all notifications for a specific user.
 * Input: userId (string)
 * Output: array of notification objects
 */
async function getUserNotifications(userId) {
  return Notification.find({ recipient: userId })
    .sort({ createdAt: -1 })
    .limit(50);
}

/**
 * Marks all unread notifications for a user as read.
 * Input: userId (string)
 */
async function markAllAsRead(userId) {
  return Notification.updateMany({ recipient: userId, isRead: false }, { isRead: true });
}

/**
 * Marks a single notification as read.
 * Input: notificationId (string), userId (string)
 */
async function markAsRead(notificationId, userId) {
  return Notification.findOneAndUpdate(
    { _id: notificationId, recipient: userId },
    { isRead: true },
    { new: true }
  );
}

/**
 * Gets the count of unread notifications for a user.
 * Input: userId (string)
 * Output: number
 */
async function getUnreadCount(userId) {
  return Notification.countDocuments({ recipient: userId, isRead: false });
}

module.exports = {
  createNotification,
  notifyAdmins,
  getUserNotifications,
  markAllAsRead,
  markAsRead,
  getUnreadCount,
};
