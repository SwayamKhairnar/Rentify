import api from './api';

/**
 * Frontend service for user reports.
 */
export const reportService = {
  /**
   * Files a new report.
   */
  submitReport: async (reportData) => {
    return api.post('/reports', reportData);
  },

  /**
   * Admin: Fetches all reports.
   */
  getAdminReports: async () => {
    return api.get('/reports/admin');
  },

  /**
   * Admin: Responds to a specific report.
   */
  respondToReport: async (id, responseData) => {
    return api.patch(`/reports/admin/${id}/respond`, responseData);
  },
};
