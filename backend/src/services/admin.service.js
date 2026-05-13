const mongoose = require('mongoose');
const { User, Item, Rental, Review, Conversation, Message, Notification } = require('../models');
const ApiError = require('../utils/apiError');

/**
 * Fetches all users in the system.
 */
async function getAllUsers() {
  return User.find({}).sort({ createdAt: -1 });
}

/**
 * Deletes a user and ALL their associated data across the system.
 * This is a "Nuclear" delete to ensure no broken references remain.
 */
async function deleteUserAndData(userId) {
  const user = await User.findById(userId);
  if (!user) {
    throw ApiError.notFound('User not found');
  }

  // Prevent deleting the last admin if necessary, but for now we allow it
  
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // 1. Handle Items and their Images
    const userItems = await Item.find({ owner: userId });
    const allItemImages = userItems.reduce((acc, item) => [...acc, ...item.images], []);
    
    // 2. Handle Reports and their Evidence
    const userReports = await mongoose.model('Report').find({ 
      $or: [{ reporter: userId }, { reportedUser: userId }] 
    });
    const evidenceImages = userReports
      .map(r => r.evidenceImage)
      .filter(img => img && img !== '');

    // Execute Cloudinary purge (async but don't block the transaction)
    const { deleteMultipleImagesFromCloudinary } = require('../utils/cloudinary.utils');
    deleteMultipleImagesFromCloudinary([...allItemImages, ...evidenceImages]);

    // Proceed with DB deletions
    await Item.deleteMany({ owner: userId }, { session });

    // 2. Handle Rentals
    // Cancel all pending/approved/active rentals involving this user
    await Rental.updateMany(
      { 
        $or: [{ renter: userId }, { owner: userId }],
        status: { $in: ['pending', 'approved', 'active'] }
      },
      { 
        status: 'cancelled', 
        message: 'This rental was cancelled because one of the parties was removed from the system.' 
      },
      { session }
    );

    // 3. Delete all Reviews left BY or received BY the user
    await Review.deleteMany(
      { $or: [{ reviewer: userId }, { reviewee: userId }] },
      { session }
    );

    // 4. Delete all Notifications for this user
    await Notification.deleteMany({ recipient: userId }, { session });

    // 5. Delete Conversations and Messages
    // Find all conversations involving this user
    const userConversations = await Conversation.find({ participants: userId });
    const conversationIds = userConversations.map(c => c._id);

    // Delete messages in those conversations
    await Message.deleteMany({ conversation: { $in: conversationIds } }, { session });
    // Delete the conversations themselves
    await Conversation.deleteMany({ _id: { $in: conversationIds } }, { session });

    // 6. Delete all Reports involving this user
    await mongoose.model('Report').deleteMany(
      { $or: [{ reporter: userId }, { reportedUser: userId }] },
      { session }
    );
    
    // 7. Finally, delete the User profile itself
    await User.findByIdAndDelete(userId, { session });

    await session.commitTransaction();
    return { success: true, message: 'User and all associated data deleted successfully' };
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
}

module.exports = {
  getAllUsers,
  deleteUserAndData,
};
