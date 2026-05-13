const mongoose = require('mongoose');

/**
 * Item Schema — represents a listable/rentable item on Rentify.
 * Tracks owner, pricing, availability, category, and images.
 */
const itemSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: [true, 'Title is required'],
      trim: true,
      maxlength: 100,
    },
    description: {
      type: String,
      required: [true, 'Description is required'],
      maxlength: 1000,
    },
    category: {
      type: String,
      required: [true, 'Category is required'],
      enum: [
        'textbooks',
        'electronics',
        'bikes',
        'cameras',
        'furniture',
        'clothing',
        'sports',
        'instruments',
        'other',
      ],
    },
    pricePerDay: {
      type: Number,
      required: [true, 'Price per day is required'],
      min: [0, 'Price cannot be negative'],
    },
    images: {
      type: [String],
      default: [],
    },
    condition: {
      type: String,
      enum: ['new', 'like-new', 'good', 'fair', 'poor'],
      default: 'good',
    },
    isAvailable: {
      type: Boolean,
      default: true,
    },
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    location: {
      type: String,
      trim: true,
      default: '',
    },
    rating: {
      type: Number,
      default: 0,
      min: 0,
      max: 5,
    },
    totalReviews: {
      type: Number,
      default: 0,
    },
  },
  {
    timestamps: true,
  }
);

// Indexes for common queries
itemSchema.index({ owner: 1 });
itemSchema.index({ category: 1 });
itemSchema.index({ isAvailable: 1 });
itemSchema.index({ title: 'text', description: 'text' });

module.exports = mongoose.model('Item', itemSchema);
