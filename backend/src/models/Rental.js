const mongoose = require('mongoose');

/**
 * Rental Schema — represents a rental transaction between two students.
 * Tracks the item, renter, owner, date range, status, and total cost.
 */
const rentalSchema = new mongoose.Schema(
  {
    item: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Item',
      required: true,
    },
    renter: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    startDate: {
      type: Date,
      required: [true, 'Start date is required'],
    },
    endDate: {
      type: Date,
      required: [true, 'End date is required'],
    },
    totalPrice: {
      type: Number,
      required: true,
      min: 0,
    },
    status: {
      type: String,
      enum: ['pending', 'approved', 'rejected', 'active', 'completed', 'cancelled'],
      default: 'pending',
    },
    message: {
      type: String,
      maxlength: 500,
      default: '',
    },
    offerPrice: {
      type: Number,
      min: 0,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

// Indexes for querying rentals by user roles and status
rentalSchema.index({ renter: 1, status: 1 });
rentalSchema.index({ owner: 1, status: 1 });
rentalSchema.index({ item: 1 });

module.exports = mongoose.model('Rental', rentalSchema);
