const { Router } = require('express');
const reportController = require('../controllers/report.controller');
const authenticate = require('../middlewares/auth');
const authorizeAdmin = require('../middlewares/admin');

const router = Router();

router.use(authenticate);

// POST /api/reports — File a report
router.post('/', reportController.submitReport);

// Admin-only routes
router.get('/admin', authorizeAdmin, reportController.getReports);
router.patch('/admin/:id/respond', authorizeAdmin, reportController.respondToReport);

module.exports = router;
