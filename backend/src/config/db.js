const mongoose = require('mongoose');

/**
 * Robust MongoDB Connection Utility.
 * Addresses Node 18/OpenSSL 3 SSL handshake issues and provides production-grade options.
 */
async function connectDB() {
  const uri = process.env.MONGODB_URI;

  if (!uri) {
    console.error('❌ MONGODB_URI is not defined in environment variables');
    process.exit(1);
  }

  // Connection Options for Stability and Security
  const options = {
    // Timeout for server selection (default 30s is too long for health checks)
    serverSelectionTimeoutMS: 5000,
    
    // Maintain a stable pool of connections
    maxPoolSize: 10,
    
    // Enable socket timeout to prevent hanging connections
    socketTimeoutMS: 45000,
    
    // Force IPv4 if IPv6 resolution is unstable (common on some ISPs)
    family: 4,

    // TLS/SSL Hardening - Explicitly setting these can help with Handshake failures
    // Note: If "tlsv1 alert internal error" persists, the issue is likely
    // a local SSL proxy or the srv record being blocked by a firewall.
    tls: true,
  };

  try {
    const conn = await mongoose.connect(uri, options);
    
    console.log('\n----------------------------------------');
    console.log(`✅ MongoDB Connected!`);
    console.log(`📡 Host: ${conn.connection.host}`);
    console.log(`🗄️  DB Name: ${conn.connection.name}`);
    console.log('----------------------------------------\n');

    // Handle connection loss after initial success
    mongoose.connection.on('error', (err) => {
      console.error(`💥 MongoDB runtime error: ${err}`);
    });

    mongoose.connection.on('disconnected', () => {
      console.warn('⚠️ MongoDB disconnected. Attempting to reconnect...');
    });

    return conn;
  } catch (error) {
    console.error('\n----------------------------------------');
    console.error('❌ MongoDB Connection Failed');
    console.error(`📝 Message: ${error.message}`);
    
    // Provide actionable advice for the specific SSL error
    if (error.message.includes('SSL routines')) {
      console.error('\n💡 TROUBLESHOOTING TIP:');
      console.error('This SSL error (Alert 80) is common with Node 18+ and OpenSSL 3.');
      console.error('Try one of the following:');
      console.error('1. Update your .env to use the "Standard Connection String" (mongodb:// instead of mongodb+srv://)');
      console.error('2. Run your app with: NODE_OPTIONS="--openssl-legacy-provider" npm run dev');
      console.error('3. Check if a VPN/Antivirus is intercepting your TLS handshake.');
    }
    console.log('----------------------------------------\n');
    
    process.exit(1);
  }
}

module.exports = connectDB;
