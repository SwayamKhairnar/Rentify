const mongoose = require('mongoose');
const { Item } = require('./backend/src/models');
require('dotenv').config({ path: './backend/.env' });

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  const items = await Item.find({});
  console.log('All items:', items.map(i => ({ id: i._id, title: i.title, owner: i.owner, isAvailable: i.isAvailable })));
  process.exit(0);
}
run();
