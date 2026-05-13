const { Router } = require('express');
const { z } = require('zod');
const reviewController = require('../controllers/review.controller');
const authenticate = require('../middlewares/auth');
const validate = require('../middlewares/validate');

const router = Router();

// Validation schema for creating reviews
const createReviewSchema = z.object({
  rentalId: z.string().min(1, 'Rental ID is required'),
  rating: z.number().int().min(1).max(5, 'Rating must be between 1 and 5'),
  itemRating: z.number().int().min(1).max(5, 'Item rating must be between 1 and 5').optional(),
  comment: z.string().max(500).optional().default(''),
});

// POST /api/reviews — Create a review (auth required)
router.post('/', authenticate, validate(createReviewSchema), reviewController.createReview);

// GET /api/reviews/user/:userId — Get reviews for a user (public)
router.get('/user/:userId', reviewController.getUserReviews);

// GET /api/reviews/rental/:rentalId — Get reviews for a rental (public)
router.get('/rental/:rentalId', reviewController.getRentalReviews);

module.exports = router;
