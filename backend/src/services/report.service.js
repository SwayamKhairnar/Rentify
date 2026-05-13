const mongoose = require('mongoose');
const { Report, Rental } = require('../models');
const notificationService = require('./notification.service');
const ApiError = require('../utils/apiError');

/**
 * Creates a new user report.
 * Automatically notifies all admins.
 */
async function createReport(reporterId, reportData) {
  const { reportedUserId, rentalId, reason, description, evidenceImage } = reportData;

  // Verify rental exists and involves both parties
  const rental = await Rental.findById(rentalId);
  if (!rental) {
    throw ApiError.notFound('Rental transaction not found');
  }

  // Ensure rental is completed
  if (rental.status !== 'completed') {
    throw ApiError.badRequest('You can only report users after the rental is completed.');
  }

  const report = await Report.create({
    reporter: reporterId,
    reportedUser: reportedUserId,
    rental: rentalId,
    reason,
    description,
    evidenceImage,
  });

  // Notify Admins
  await notificationService.notifyAdmins({
    type: 'system',
    title: 'New User Report Filed',
    message: `A new report has been filed against a user for: ${reason}.`,
    link: '/admin?tab=reports',
    sender: reporterId,
  });

  return report;
}

/**
 * Admin: Reply to a report.
 * Notifies the reporter.
 */
async function replyToReport(reportId, adminId, replyData) {
  const { message, status, action } = replyData;
  const { User, Item } = require('../models');

  const report = await Report.findById(reportId).populate('reporter');
  if (!report) {
    throw ApiError.notFound('Report not found');
  }

  report.adminNotes = message;
  if (status) report.status = status;
  if (action) report.adminAction = action;
  await report.save();

  // EXECUTE ACTUAL ACTION
  if (action === 'listing_removed') {
    // Delete the item associated with this report/rental
    if (report.rental) {
      const rental = await mongoose.model('Rental').findById(report.rental);
      if (rental && rental.item) {
        await Item.findByIdAndDelete(rental.item);
      }
    }
  } else if (action === 'account_suspended') {
    // Suspend the reported user
    await User.findByIdAndUpdate(report.reportedUser, { isSuspended: true });
  }

  // Format action label for notification
  const actionLabels = {
    none: 'No action taken',
    warned: 'User has been officially warned',
    listing_removed: 'The relevant listing has been removed',
    account_suspended: 'The reported user account has been suspended',
    resolved: 'Marked as resolved'
  };

  // Notify the reporter
  await notificationService.createNotification({
    recipient: report.reporter._id,
    sender: adminId,
    type: 'system',
    title: 'Your Report has been Resolved',
    message: `Resolution: ${actionLabels[action || 'none']}. \n\nAdmin Message: ${message}`,
    link: `/rentals/${report.rental}`,
  });

  return report;
}

/**
 * Admin: Get all reports.
 */
async function getAllReports() {
  return Report.find({})
    .populate('reporter', 'name email')
    .populate('reportedUser', 'name email')
    .populate('rental', 'startDate endDate')
    .sort({ createdAt: -1 });
}

module.exports = {
  createReport,
  replyToReport,
  getAllReports,
};
