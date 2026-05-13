const mongoose = require('mongoose');
const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });
const { User } = require('../models');

async function promoteAdmin() {
  const email = process.argv[2];

  if (!email) {
    console.error('❌ Please provide an email address. Usage: node promoteAdmin.js <email>');
    process.exit(1);
  }

  try {
    console.log('📡 Connecting to database...');
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected.');

    const user = await User.findOne({ email: email.toLowerCase() });

    if (!user) {
      console.error(`❌ User with email "${email}" not found.`);
      process.exit(1);
    }

    user.role = 'admin';
    await user.save();

    console.log(`\n🎉 SUCCESS! "${user.name}" (${user.email}) is now a Super Admin.`);
    console.log('👉 Please log out and log back in to see the changes.\n');

  } catch (error) {
    console.error('❌ Error promoting user:', error.message);
  } finally {
    await mongoose.disconnect();
    console.log('🔌 Disconnected from database.');
    process.exit(0);
  }
}

promoteAdmin();
