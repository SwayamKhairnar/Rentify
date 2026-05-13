const mongoose = require('mongoose');

/**
 * Review Schema — represents a review left after a completed rental.
 * Links reviewer, reviewee, rental, and stores rating + comment.
 */
const reviewSchema = new mongoose.Schema(
  {
    rental: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Rental',
      required: true,
    },
    reviewer: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    reviewee: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    rating: {
      type: Number,
      required: [true, 'Rating is required'],
      min: 1,
      max: 5,
    },
    itemRating: {
      type: Number,
      min: 1,
      max: 5,
    },
    type: {
      type: String,
      enum: ['lender', 'renter'],
      required: true,
    },
    comment: {
      type: String,
      maxlength: 500,
      default: '',
    },
  },
  {
    timestamps: true,
  }
);

// Prevent duplicate reviews: one review per user per rental
reviewSchema.index({ rental: 1, reviewer: 1 }, { unique: true });
reviewSchema.index({ reviewee: 1 });

module.exports = mongoose.model('Review', reviewSchema);
