const ApiError = require('../utils/apiError');

/**
 * Middleware to restrict access to admin-only routes.
 * Assumes 'authenticate' middleware has already run and populated req.user.
 */
const authorizeAdmin = (req, res, next) => {
  if (!req.user || req.user.role !== 'admin') {
    return next(ApiError.forbidden('Access denied. Admin privileges required.'));
  }
  next();
};

module.exports = authorizeAdmin;
