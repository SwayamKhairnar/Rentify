/**
 * Central export for all Mongoose models.
 * Import models from here for a single source of truth.
 */
const User = require('./User');
const Item = require('./Item');
const Rental = require('./Rental');
const Conversation = require('./Conversation');
const Message = require('./Message');
const Review = require('./Review');
const Notification = require('./Notification');
const Report = require('./Report');

module.exports = {
  User,
  Item,
  Rental,
  Conversation,
  Message,
  Review,
  Notification,
  Report,
};
