const jwt = require('jsonwebtoken');
const ApiError = require('../utils/apiError');
const { User } = require('../models');

/**
 * JWT authentication middleware.
 * Extracts the Bearer token from the Authorization header,
 * verifies it, and attaches the user object to req.user.
 * Input: req.headers.authorization (Bearer <token>)
 * Output: sets req.user or throws 401 ApiError
 */
async function authenticate(req, res, next) {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw ApiError.unauthorized('Access token is required');
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const user = await User.findById(decoded.userId);
    if (!user) {
      throw ApiError.unauthorized('User no longer exists');
    }

    if (user.isSuspended) {
      throw ApiError.forbidden('Your account has been suspended by an administrator.');
    }

    req.user = user;
    next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError') {
      return next(ApiError.unauthorized('Invalid token'));
    }
    if (error.name === 'TokenExpiredError') {
      return next(ApiError.unauthorized('Token expired'));
    }
    next(error);
  }
}

module.exports = authenticate;
