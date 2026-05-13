const mongoose = require('mongoose');
const { Review, Rental } = require('../models');
const userService = require('./user.service');
const itemService = require('./item.service');
const notificationService = require('./notification.service');
const ApiError = require('../utils/apiError');

/**
 * Creates a review for a completed rental.
 * Validates that the rental is completed and the reviewer is a participant.
 * Prevents duplicate reviews per user per rental.
 * Supports dual-ratings (behavior + item) for lenders.
 * Input: { rentalId, rating, itemRating, comment }, reviewerId (string)
 * Output: populated review object
 */
async function createReview({ rentalId, rating, itemRating, comment }, reviewerId) {
  const rental = await Rental.findById(rentalId);
  if (!rental) {
    throw ApiError.notFound('Rental not found');
  }

  if (rental.status !== 'completed') {
    throw ApiError.badRequest('Can only review completed rentals');
  }

  const isOwner = rental.owner.toString() === reviewerId;
  const isRenter = rental.renter.toString() === reviewerId;

  if (!isOwner && !isRenter) {
    throw ApiError.forbidden('You are not part of this rental');
  }

  // Determine type and reviewee
  const type = isRenter ? 'lender' : 'renter';
  const revieweeId = isOwner ? rental.renter.toString() : rental.owner.toString();

  // Check for existing review
  const existingReview = await Review.findOne({ rental: rentalId, reviewer: reviewerId });
  if (existingReview) {
    throw ApiError.conflict('You have already reviewed this rental');
  }

  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    const reviewData = {
      rental: rentalId,
      reviewer: reviewerId,
      reviewee: revieweeId,
      rating,
      type,
      comment: comment || '',
    };

    // If renter is reviewing lender, they can also rate the item
    if (type === 'lender' && itemRating) {
      reviewData.itemRating = itemRating;
    }

    const review = await Review.create([reviewData], { session });

    // Update Scores
    if (type === 'lender') {
      // Renter is rating Owner behavior
      await userService.updateLenderRating(revieweeId, rating, session);
      
      // If item was rated, update Item score and Owner's item quality average
      if (itemRating) {
        await itemService.updateItemRating(rental.item, itemRating, session);
        await userService.updateItemQualityAverage(revieweeId, itemRating, session);
      }
    } else {
      // Owner is rating Renter behavior
      await userService.updateRenterRating(revieweeId, rating, session);
    }

    // Always update the overall composite rating
    // Note: We use an average of all rating inputs for simplicity in the legacy 'rating' field
    const compositeRating = (type === 'lender' && itemRating) ? (rating + itemRating) / 2 : rating;
    await userService.updateUserRating(revieweeId, compositeRating, session);

    // Notify the reviewee
    await notificationService.createNotification({
      recipient: revieweeId,
      sender: reviewerId,
      type: 'review_received',
      title: 'New Review!',
      message: `You received a new ${type === 'lender' ? 'Lender' : 'Renter'} review!`,
      link: `/profile`,
    }, { session });

    await session.commitTransaction();

    const populatedReview = await review[0].populate([
      { path: 'reviewer', select: 'name avatar' },
      { path: 'reviewee', select: 'name avatar' },
    ]);
    return populatedReview;
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
}

/**
 * Fetches all reviews received by a specific user.
 * Input: userId (string)
 * Output: array of populated review objects
 */
async function getReviewsForUser(userId) {
  return Review.find({ reviewee: userId })
    .populate('reviewer', 'name avatar')
    .populate('rental', 'item')
    .sort({ createdAt: -1 });
}

/**
 * Fetches all reviews for a specific rental.
 * Input: rentalId (string)
 * Output: array of populated review objects
 */
async function getReviewsForRental(rentalId) {
  return Review.find({ rental: rentalId })
    .populate('reviewer', 'name avatar')
    .populate('reviewee', 'name avatar')
    .sort({ createdAt: -1 });
}

module.exports = { createReview, getReviewsForUser, getReviewsForRental };
