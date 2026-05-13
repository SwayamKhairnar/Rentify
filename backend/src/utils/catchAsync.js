/**
 * Wraps an asynchronous function (like an Express controller) to catch any
 * errors and pass them to the global error handler middleware.
 * Eliminates the need for redundant try/catch blocks in controllers.
 */
const catchAsync = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch((err) => next(err));
};

module.exports = catchAsync;
