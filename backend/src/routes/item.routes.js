const { Router } = require('express');
const { z } = require('zod');
const itemController = require('../controllers/item.controller');
const authenticate = require('../middlewares/auth');
const validate = require('../middlewares/validate');
const { ITEM_CATEGORIES, ITEM_CONDITIONS } = require('../utils/constants');

const router = Router();

// Validation schema for creating/updating items
const createItemSchema = z.object({
  title: z.string().min(1, 'Title is required').max(100),
  description: z.string().min(1, 'Description is required').max(1000),
  category: z.enum(ITEM_CATEGORIES, { message: 'Invalid category' }),
  pricePerDay: z.number().positive('Price must be positive').max(100000, 'Price cannot exceed ₹1,00,000 per day'),
  images: z.array(z.string()).max(5).optional().default([]),
  condition: z.enum(ITEM_CONDITIONS).optional().default('good'),
  location: z.string().max(200).optional().default(''),
});

const updateItemSchema = z.object({
  title: z.string().min(1).max(100).optional(),
  description: z.string().min(1).max(1000).optional(),
  category: z.enum(ITEM_CATEGORIES).optional(),
  pricePerDay: z.number().positive().max(100000).optional(),
  images: z.array(z.string()).max(5).optional(),
  condition: z.enum(ITEM_CONDITIONS).optional(),
  location: z.string().max(200).optional(),
  isAvailable: z.boolean().optional(),
});

// GET /api/items — Get paginated items with optional filters
router.get('/', itemController.getItems);

// GET /api/items/mine — Get authenticated user's items
router.get('/mine', authenticate, itemController.getMyItems);

// GET /api/items/:id — Get a single item
router.get('/:id', itemController.getItem);

// POST /api/items — Create a new item (auth required)
router.post('/', authenticate, validate(createItemSchema), itemController.createItem);

// PUT /api/items/:id — Update an item (auth + owner required)
router.put('/:id', authenticate, validate(updateItemSchema), itemController.updateItem);

// DELETE /api/items/:id — Delete an item (auth + owner required)
router.delete('/:id', authenticate, itemController.deleteItem);

module.exports = router;
