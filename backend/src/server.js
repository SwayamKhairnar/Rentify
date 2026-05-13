const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const createApp = require('./app');
const connectDB = require('./config/db');
const validateEnv = require('./config/env');

/**
 * Application entry point.
 * Validates environment, connects to MongoDB, and starts the HTTP server.
 */
async function startServer() {
  // Validate environment variables
  const env = validateEnv();

  // Connect to MongoDB
  await connectDB();

  // Create and start Express app
  const app = createApp();
  const PORT = env.PORT || 4000;

  app.listen(PORT, () => {
    console.log(`\n🚀 Rentify API Server running on http://localhost:${PORT}`);
    console.log(`📦 Environment: ${env.NODE_ENV}`);
    console.log(`💾 Database: ${env.MONGODB_URI}\n`);
  });
}

startServer().catch((error) => {
  console.error('❌ Failed to start server:', error);
  process.exit(1);
});
