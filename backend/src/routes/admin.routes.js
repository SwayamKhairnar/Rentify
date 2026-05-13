const { Router } = require('express');
const adminController = require('../controllers/admin.controller');
const authenticate = require('../middlewares/auth');
const authorizeAdmin = require('../middlewares/admin');

const router = Router();

// All admin routes require authentication AND admin role
router.use(authenticate);
router.use(authorizeAdmin);

// GET /api/admin/users — View all users
router.get('/users', adminController.getAllUsers);

// DELETE /api/admin/users/:id — Delete user and their data
router.delete('/users/:id', adminController.deleteUser);

module.exports = router;
