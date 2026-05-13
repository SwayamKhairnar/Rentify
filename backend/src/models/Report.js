const mongoose = require('mongoose');

/**
 * Report Schema — represents a formal complaint against a user.
 */
const reportSchema = new mongoose.Schema(
  {
    reporter: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    reportedUser: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    rental: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Rental',
      required: true,
    },
    reason: {
      type: String,
      required: true,
      enum: [
        'Late Return',
        'Item Damage',
        'Fake Product/Description',
        'Inappropriate Behavior',
        'Payment Issues',
        'No Show',
        'Other'
      ],
    },
    description: {
      type: String,
      required: true,
      maxlength: 1000,
    },
    evidenceImage: {
      type: String,
      default: '',
    },
    status: {
      type: String,
      enum: ['pending', 'reviewed', 'resolved', 'dismissed'],
      default: 'pending',
    },
    adminNotes: {
      type: String,
      default: '',
    },
    adminAction: {
      type: String,
      enum: ['none', 'warned', 'listing_removed', 'account_suspended', 'resolved'],
      default: 'none',
    },
  },
  {
    timestamps: true,
  }
);

// Index for fast fetching of reports for admin
reportSchema.index({ status: 1 });
reportSchema.index({ reportedUser: 1 });
reportSchema.index({ createdAt: -1 });

module.exports = mongoose.model('Report', reportSchema);
