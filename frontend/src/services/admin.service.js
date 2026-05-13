import api from './api';

/**
 * Frontend service for administrative tasks.
 */
export const adminService = {
  /**
   * Fetches all users (Admin only).
   */
  getAllUsers: async () => {
    return api.get('/admin/users');
  },

  /**
   * Deletes a user and their data (Admin only).
   */
  deleteUser: async (id) => {
    return api.delete(`/admin/users/${id}`);
  },
};
