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

  // Create Express app
  const app = createApp();
  const PORT = parseInt(env.PORT, 10) || 4000;

  // Start listening on 0.0.0.0 (important for Railway)
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`\n🚀 Rentify API Server running on port ${PORT}`);
    console.log(`📦 Environment: ${env.NODE_ENV}`);
    
    // Connect to MongoDB AFTER starting the server to pass platform health checks immediately
    connectDB().catch(err => {
      console.error('❌ Delayed MongoDB connection failed:', err);
    });
  });
}

startServer().catch((error) => {
  console.error('❌ Failed to start server:', error);
  process.exit(1);
});
