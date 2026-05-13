const notificationService = require('../services/notification.service');
const catchAsync = require('../utils/catchAsync');
const ApiResponse = require('../utils/apiResponse');

/**
 * Controller to fetch all notifications for the authenticated user.
 */
const getMyNotifications = catchAsync(async (req, res) => {
  const notifications = await notificationService.getUserNotifications(req.user._id);
  const unreadCount = await notificationService.getUnreadCount(req.user._id);

  ApiResponse.success(res, 200, 'Notifications fetched', { notifications, unreadCount });
});

/**
 * Controller to mark a single notification as read.
 */
const markRead = catchAsync(async (req, res) => {
  const notification = await notificationService.markAsRead(req.params.id, req.user._id);
  ApiResponse.success(res, 200, 'Notification marked as read', { notification });
});

/**
 * Controller to mark all notifications as read.
 */
const markAllRead = catchAsync(async (req, res) => {
  await notificationService.markAllAsRead(req.user._id);
  ApiResponse.success(res, 200, 'All notifications marked as read');
});

module.exports = {
  getMyNotifications,
  markRead,
  markAllRead,
};
