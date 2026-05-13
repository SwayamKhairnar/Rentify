const { Router } = require('express');
const multer = require('multer');
const authenticate = require('../middlewares/auth');
const ApiError = require('../utils/apiError');
const ApiResponse = require('../utils/apiResponse');

const { storage } = require('../config/cloudinary');

const router = Router();

// File filter to allow only images
const fileFilter = (req, file, cb) => {
  if (file.mimetype.startsWith('image/')) {
    cb(null, true);
  } else {
    cb(ApiError.badRequest('Only image files are allowed!'), false);
  }
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit per file
});

/**
 * POST /api/upload
 * Accepts an array of image files under the key 'images'.
 * Returns an array of absolute Cloudinary URLs.
 */
router.post('/', authenticate, upload.array('images', 5), (req, res, next) => {
  try {
    if (!req.files || req.files.length === 0) {
      throw ApiError.badRequest('No files uploaded');
    }

    // Cloudinary returns the full URL in 'path' (or 'secure_url' depending on version)
    const imageUrls = req.files.map((file) => file.path);
    ApiResponse.success(res, 201, 'Images uploaded successfully', { imageUrls });
  } catch (error) {
    next(error);
  }
});

// Error handling for Multer
router.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return next(ApiError.badRequest('File size cannot exceed 5MB'));
    }
    if (err.code === 'LIMIT_UNEXPECTED_FILE') {
      return next(ApiError.badRequest('Maximum 5 files allowed'));
    }
    return next(ApiError.badRequest(`Upload error: ${err.message}`));
  }
  next(err);
});

module.exports = router;
