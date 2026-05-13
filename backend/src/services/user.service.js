const { User } = require('../models');
const ApiError = require('../utils/apiError');

/**
 * Fetches a user profile by ID.
 * Input: userId (ObjectId string)
 * Output: user object (public profile)
 */
async function getUserById(userId) {
  const user = await User.findById(userId);
  if (!user) {
    throw ApiError.notFound('User not found');
  }
  return user;
}

/**
 * Updates the authenticated user's profile fields.
 * Only allows updating safe fields (name, bio, campus, phone, avatar).
 * Input: userId (string), updateData (object with allowed fields)
 * Output: updated user object
 */
async function updateProfile(userId, updateData) {
  const allowedFields = ['name', 'bio', 'campus', 'phone', 'avatar'];
  const filteredData = {};

  for (const field of allowedFields) {
    if (updateData[field] !== undefined) {
      filteredData[field] = updateData[field];
    }
  }

  const user = await User.findByIdAndUpdate(userId, filteredData, {
    new: true,
    runValidators: true,
  });

  if (!user) {
    throw ApiError.notFound('User not found');
  }

  return user;
}

/**
 * Generic helper to update a specific rating field on the User model atomically.
 */
async function updateRatingField(userId, fieldName, countFieldName, newRating, session = null) {
  return User.findByIdAndUpdate(
    userId,
    [
      {
        $set: {
          [countFieldName]: { $add: [{ $ifNull: [`$${countFieldName}`, 0] }, 1] },
          [fieldName]: {
            $round: [
              {
                $divide: [
                  { 
                    $add: [
                      { $multiply: [{ $ifNull: [`$${fieldName}`, 0] }, { $ifNull: [`$${countFieldName}`, 0] }] }, 
                      newRating 
                    ] 
                  },
                  { $add: [{ $ifNull: [`$${countFieldName}`, 0] }, 1] },
                ],
              },
              1,
            ],
          },
        },
      },
    ],
    { new: true, session }
  );
}

/**
 * Updates a user's overall average rating.
 */
async function updateUserRating(userId, newRating, session = null) {
  return updateRatingField(userId, 'rating', 'totalReviews', newRating, session);
}

/**
 * Updates a user's lender (host behavior) rating.
 */
async function updateLenderRating(userId, newRating, session = null) {
  return updateRatingField(userId, 'lenderRating', 'totalLenderReviews', newRating, session);
}

/**
 * Updates a user's renter rating.
 */
async function updateRenterRating(userId, newRating, session = null) {
  return updateRatingField(userId, 'renterRating', 'totalRenterReviews', newRating, session);
}

/**
 * Updates a user's average item quality score (across all their items).
 */
async function updateItemQualityAverage(userId, newRating, session = null) {
  return updateRatingField(userId, 'itemQualityAverage', 'totalItemQualityReviews', newRating, session);
}

module.exports = { 
  getUserById, 
  updateProfile, 
  updateUserRating, 
  updateLenderRating, 
  updateRenterRating, 
  updateItemQualityAverage 
};
