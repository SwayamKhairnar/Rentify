const adminService = require('../services/admin.service');
const catchAsync = require('../utils/catchAsync');
const ApiResponse = require('../utils/apiResponse');

/**
 * Controller to fetch all users.
 */
const getAllUsers = catchAsync(async (req, res) => {
  const users = await adminService.getAllUsers();
  ApiResponse.success(res, 200, 'All users fetched successfully', { users });
});

/**
 * Controller to delete a user and their data.
 */
const deleteUser = catchAsync(async (req, res) => {
  const result = await adminService.deleteUserAndData(req.params.id);
  ApiResponse.success(res, 200, result.message);
});

module.exports = {
  getAllUsers,
  deleteUser,
};
