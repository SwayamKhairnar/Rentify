const ApiError = require('../utils/apiError');

/**
 * Global error handling middleware.
 * Catches all errors thrown in route handlers and middlewares.
 * Returns a consistent JSON error response.
 * Input: err (Error object)
 * Output: JSON error response with appropriate status code
 */
function errorHandler(err, req, res, next) {
  // Default values
  let statusCode = err.statusCode || 500;
  let message = err.message || 'Internal server error';

  // Mongoose validation error
  if (err.name === 'ValidationError') {
    statusCode = 400;
    const messages = Object.values(err.errors).map((e) => e.message);
    message = messages.join(', ');
  }

  // Mongoose duplicate key error
  if (err.code === 11000) {
    statusCode = 409;
    const field = Object.keys(err.keyValue)[0];
    message = `${field} already exists`;
  }

  // Mongoose cast error (invalid ObjectId)
  if (err.name === 'CastError') {
    statusCode = 400;
    message = `Invalid ${err.path}: ${err.value}`;
  }

  // Zod validation error
  if (err.name === 'ZodError') {
    statusCode = 400;
    message = err.errors.map((e) => `${e.path.join('.')}: ${e.message}`).join(', ');
  }

  // Log error in development
  if (process.env.NODE_ENV === 'development') {
    console.error('❌ Error:', err);
  }

  // In production, mask non-operational errors
  const finalMessage = process.env.NODE_ENV === 'production' && !err.isOperational 
    ? 'Something went wrong on our end' 
    : message;

  res.status(statusCode).json({
    success: false,
    message: finalMessage,
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack }),
  });
}

module.exports = errorHandler;
