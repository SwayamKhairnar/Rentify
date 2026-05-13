const reportService = require('../services/report.service');
const catchAsync = require('../utils/catchAsync');
const ApiResponse = require('../utils/apiResponse');

/**
 * Controller to file a new report.
 */
const submitReport = catchAsync(async (req, res) => {
  const report = await reportService.createReport(req.user._id, req.body);
  ApiResponse.success(res, 201, 'Report submitted successfully. Admins have been notified.', { report });
});

/**
 * Controller for admin to get all reports.
 */
const getReports = catchAsync(async (req, res) => {
  const reports = await reportService.getAllReports();
  ApiResponse.success(res, 200, 'All reports fetched', { reports });
});

/**
 * Controller for admin to reply to a report.
 */
const respondToReport = catchAsync(async (req, res) => {
  const report = await reportService.replyToReport(req.params.id, req.user._id, req.body);
  ApiResponse.success(res, 200, 'Response sent to reporter', { report });
});

module.exports = {
  submitReport,
  getReports,
  respondToReport,
};
