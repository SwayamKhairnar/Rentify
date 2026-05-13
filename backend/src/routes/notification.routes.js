const { Router } = require('express');
const notificationController = require('../controllers/notification.controller');
const authenticate = require('../middlewares/auth');

const router = Router();

// All notification routes require authentication
router.use(authenticate);

// GET /api/notifications — Get all notifications for the user
router.get('/', notificationController.getMyNotifications);

// PATCH /api/notifications/mark-all-read — Mark all as read
router.patch('/mark-all-read', notificationController.markAllRead);

// PATCH /api/notifications/:id/mark-read — Mark a single one as read
router.patch('/:id/mark-read', notificationController.markRead);

module.exports = router;
