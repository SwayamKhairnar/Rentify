const mongoose = require('mongoose');
const { Rental, Item, Conversation } = require('../models');
const ApiError = require('../utils/apiError');
const notificationService = require('./notification.service');

/**
 * Creates a new rental request.
 * Validates that the renter is not the owner and the item is available.
 * Also creates an associated conversation for the rental.
 * Input: { itemId, startDate, endDate, message }, renterId (string)
 * Output: populated rental object
 */
async function createRental({ itemId, startDate, endDate, message, offerPrice }, renterId) {
  const item = await Item.findById(itemId).populate('owner', 'name');
  if (!item) {
    throw ApiError.notFound('Item not found');
  }

  const start = new Date(startDate);
  const end = new Date(endDate);
  
  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
    throw ApiError.badRequest('Invalid date format');
  }
  if (end <= start) {
    throw ApiError.badRequest('End date must be after start date');
  }

  // Check if item is available for these specific dates
  const overlappingRental = await Rental.findOne({
    item: itemId,
    status: { $in: ['approved', 'active'] },
    $or: [
      { startDate: { $lte: start }, endDate: { $gte: start } },
      { startDate: { $lte: end }, endDate: { $gte: end } },
      { startDate: { $gte: start }, endDate: { $lte: end } }
    ]
  });

  if (overlappingRental) {
    throw ApiError.badRequest('Item is already booked for the selected dates');
  }

  if (item.owner.toString() === renterId) {
    throw ApiError.badRequest('You cannot rent your own item');
  }

  const days = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
  const totalPrice = days * item.pricePerDay;

  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    const rental = await Rental.create(
      [
        {
          item: itemId,
          renter: renterId,
          owner: item.owner,
          startDate: start,
          endDate: end,
          totalPrice,
          message: message || '',
          offerPrice: (offerPrice !== undefined && offerPrice !== null) ? Number(offerPrice) : null,
        },
      ],
      { session }
    );

    // Create a conversation linked to this rental
    await Conversation.create(
      [
        {
          rental: rental[0]._id,
          participants: [renterId, item.owner._id.toString()],
        },
      ],
      { session }
    );

    // Notify the owner of the new request
    await notificationService.createNotification({
      recipient: item.owner._id,
      sender: renterId,
      type: 'rental_request',
      title: 'New Rental Request',
      message: `Someone wants to rent your ${item.title}!`,
      link: `/rentals/${rental[0]._id}`,
    }, { session });

    await session.commitTransaction();

    const populatedRental = await rental[0].populate([
      { path: 'item', select: 'title images pricePerDay' },
      { path: 'renter', select: 'name email avatar' },
      { path: 'owner', select: 'name email avatar' },
    ]);
    return populatedRental;
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
}

/**
 * Fetches all rentals where the user is the renter.
 * Input: userId (string)
 * Output: array of populated rental objects
 */
async function getMyRentals(userId) {
  return Rental.find({ renter: userId })
    .populate('item', 'title images pricePerDay category')
    .populate('owner', 'name email avatar campus')
    .sort({ createdAt: -1 });
}

/**
 * Fetches all rental requests received by the item owner.
 * Input: userId (string)
 * Output: array of populated rental objects
 */
async function getReceivedRequests(userId) {
  return Rental.find({ owner: userId })
    .populate('item', 'title images pricePerDay category')
    .populate('renter', 'name email avatar campus')
    .sort({ createdAt: -1 });
}

/**
 * Updates the status of a rental request.
 * Only the owner can approve/reject; either party can cancel.
 * Input: rentalId (string), status (string), userId (string)
 * Output: updated rental object
 */
async function updateRentalStatus(rentalId, status, userId) {
  const rental = await Rental.findById(rentalId).populate('item');
  if (!rental) {
    throw ApiError.notFound('Rental not found');
  }

  const isOwner = rental.owner.toString() === userId;
  const isRenter = rental.renter.toString() === userId;

  if (!isOwner && !isRenter) {
    throw ApiError.forbidden('You are not part of this rental');
  }

  // Only owner can approve or reject
  if (['approved', 'rejected'].includes(status) && !isOwner) {
    throw ApiError.forbidden('Only the item owner can approve or reject');
  }

  // Only allow cancellation by either party
  if (status === 'cancelled' && !isOwner && !isRenter) {
    throw ApiError.forbidden('You cannot cancel this rental');
  }

  // Validate status transitions
  const validTransitions = {
    pending: ['approved', 'rejected', 'cancelled'],
    approved: ['active', 'cancelled'],
    active: ['completed', 'cancelled'],
  };

  const oldStatus = rental.status;

  if (!validTransitions[oldStatus]?.includes(status)) {
    throw ApiError.badRequest(`Cannot change status from '${oldStatus}' to '${status}'`);
  }

  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    rental.status = status;
    await rental.save({ session });

    if (status === 'approved') {
      // Check for overlap one last time before committing
      const overlappingRental = await Rental.findOne({
        _id: { $ne: rental._id },
        item: rental.item._id,
        status: { $in: ['approved', 'active'] },
        $or: [
          { startDate: { $lte: rental.startDate }, endDate: { $gte: rental.startDate } },
          { startDate: { $lte: rental.endDate }, endDate: { $gte: rental.endDate } },
          { startDate: { $gte: rental.startDate }, endDate: { $lte: rental.endDate } }
        ]
      }).session(session);

      if (overlappingRental) {
        throw ApiError.badRequest('This item is already booked for overlapping dates.');
      }

      // ONLY cancel pending rentals that OVERLAP with this one
      await Rental.updateMany(
        { 
          item: rental.item._id, 
          _id: { $ne: rental._id }, 
          status: 'pending',
          $or: [
            { startDate: { $lte: rental.startDate }, endDate: { $gte: rental.startDate } },
            { startDate: { $lte: rental.endDate }, endDate: { $gte: rental.endDate } },
            { startDate: { $gte: rental.startDate }, endDate: { $lte: rental.endDate } }
          ]
        },
        { 
          status: 'cancelled',
          message: 'This request was cancelled because the item was booked for overlapping dates.' 
        },
        { session }
      );

      // Notify the renter that their request was approved
      await notificationService.createNotification({
        recipient: rental.renter,
        sender: userId,
        type: 'rental_status',
        title: 'Rental Approved!',
        message: `Your request for ${rental.item.title} has been approved.`,
        link: `/rentals/${rental._id}`,
      }, { session });

    } else if (status === 'rejected') {
      // Notify the renter of rejection
      await notificationService.createNotification({
        recipient: rental.renter,
        sender: userId,
        type: 'rental_status',
        title: 'Request Rejected',
        message: `Your request for ${rental.item.title} was declined.`,
        link: `/rentals/${rental._id}`,
      }, { session });

    } else if (status === 'active') {
      // Notify the renter that rental is now active
      await notificationService.createNotification({
        recipient: rental.renter,
        sender: userId,
        type: 'rental_status',
        title: 'Rental Started',
        message: `You are now renting the ${rental.item.title}.`,
        link: `/rentals/${rental._id}`,
      }, { session });

    } else if (status === 'completed') {
      // Notify both parties to leave reviews
      await notificationService.createNotification({
        recipient: rental.renter,
        sender: userId,
        type: 'rental_status',
        title: 'Rental Completed',
        message: `How was your experience? Please rate the item and the owner!`,
        link: `/rentals/${rental._id}`,
      }, { session });

      await notificationService.createNotification({
        recipient: rental.owner,
        sender: userId,
        type: 'rental_status',
        title: 'Rental Completed',
        message: `How was your experience? Please rate the renter!`,
        link: `/rentals/${rental._id}`,
      }, { session });

    } else if (status === 'cancelled') {
      // Notify the other party of cancellation
      const otherPartyId = isOwner ? rental.renter : rental.owner;
      await notificationService.createNotification({
        recipient: otherPartyId,
        sender: userId,
        type: 'rental_status',
        title: 'Rental Cancelled',
        message: `The rental for ${rental.item.title} has been cancelled.`,
        link: `/rentals/${rental._id}`,
      }, { session });
    }

    await session.commitTransaction();

    const populatedRental = await rental.populate([
      { path: 'item', select: 'title images pricePerDay' },
      { path: 'renter', select: 'name email avatar' },
      { path: 'owner', select: 'name email avatar' },
    ]);
    return populatedRental;
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
}

/**
 * Fetches a single rental by ID with full details.
 * Input: rentalId (string), userId (string)
 * Output: populated rental object
 */
async function getRentalById(rentalId, userId) {
  const rental = await Rental.findById(rentalId)
    .populate('item', 'title images pricePerDay category description')
    .populate('renter', 'name email avatar campus')
    .populate('owner', 'name email avatar campus');

  if (!rental) {
    throw ApiError.notFound('Rental not found');
  }

  const isOwner = rental.owner._id.toString() === userId;
  const isRenter = rental.renter._id.toString() === userId;

  if (!isOwner && !isRenter) {
    throw ApiError.forbidden('You are not part of this rental');
  }

  return rental;
}

module.exports = {
  createRental,
  getMyRentals,
  getReceivedRequests,
  updateRentalStatus,
  getRentalById,
};
