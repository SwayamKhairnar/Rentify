const mongoose = require('mongoose');
const bcrypt = require('bcrypt');

/**
 * User Schema — represents a registered student on Rentify.
 * Stores profile info, hashed password, and campus details.
 * Password is auto-hashed before save via pre-save hook.
 */
const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Name is required'],
      trim: true,
      minlength: 2,
      maxlength: 50,
    },
    email: {
      type: String,
      required: [true, 'Email is required'],
      unique: true,
      lowercase: true,
      trim: true,
      match: [/^\S+@\S+\.\S+$/, 'Please provide a valid email'],
    },
    role: {
      type: String,
      enum: ['student', 'admin'],
      default: 'student',
    },
    password: {
      type: String,
      required: [true, 'Password is required'],
      minlength: 6,
      select: false, // Never return password by default
    },
    avatar: {
      type: String,
      default: '',
    },
    campus: {
      type: String,
      trim: true,
      default: '',
    },
    bio: {
      type: String,
      maxlength: 300,
      default: '',
    },
    phone: {
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
    lenderRating: {
      type: Number,
      default: 0,
      min: 0,
      max: 5,
    },
    totalLenderReviews: {
      type: Number,
      default: 0,
    },
    renterRating: {
      type: Number,
      default: 0,
      min: 0,
      max: 5,
    },
    totalRenterReviews: {
      type: Number,
      default: 0,
    },
    itemQualityAverage: {
      type: Number,
      default: 0,
      min: 0,
      max: 5,
    },
    totalItemQualityReviews: {
      type: Number,
      default: 0,
    },
    isSuspended: {
      type: Boolean,
      default: false,
    },
  },
  {
    timestamps: true,
  }
);

/**
 * Pre-save hook: hashes the password before storing in DB.
 * Only runs when the password field is modified.
 */
userSchema.pre('save', async function (next) {
  if (!this.isModified('password')) return next();
  const salt = await bcrypt.genSalt(12);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

/**
 * Compares a plain-text password against the stored hash.
 * Input: candidatePassword (string)
 * Output: boolean — true if match, false otherwise
 */
userSchema.methods.comparePassword = async function (candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

/**
 * Returns a safe JSON representation without the password field.
 */
userSchema.methods.toJSON = function () {
  const obj = this.toObject();
  delete obj.password;
  return obj;
};

module.exports = mongoose.model('User', userSchema);
