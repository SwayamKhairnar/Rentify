/**
 * VERIFICATION SCRIPT: Overlap Logic
 * This script simulates:
 * 1. An item is listed.
 * 2. User A requests Dec 1-5.
 * 3. User B requests Dec 3-7 (OVERLAP).
 * 4. User C requests Dec 10-15 (NO OVERLAP).
 * 5. Admin approves User A (Dec 1-5).
 * 6. EXPECTED: User B is cancelled, User C stays pending.
 */

const mongoose = require('mongoose');
require('dotenv').config({ path: './.env' });
const { Rental, Item, User } = require('../src/models');
const rentalService = require('../src/services/rental.service');

async function testOverlap() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to DB');

  try {
    // 1. Setup
    const owner = await User.findOne({ email: 'owner@test.com' }) || await User.create({ name: 'Owner', email: 'owner@test.com', password: 'password', campus: 'Main' });
    const renterA = await User.findOne({ email: 'a@test.com' }) || await User.create({ name: 'Renter A', email: 'a@test.com', password: 'password', campus: 'Main' });
    const renterB = await User.findOne({ email: 'b@test.com' }) || await User.create({ name: 'Renter B', email: 'b@test.com', password: 'password', campus: 'Main' });
    const renterC = await User.findOne({ email: 'c@test.com' }) || await User.create({ name: 'Renter C', email: 'c@test.com', password: 'password', campus: 'Main' });

    const item = await Item.create({
      title: 'Test Overlap Item',
      description: 'Test',
      category: 'other',
      pricePerDay: 100,
      owner: owner._id,
      isAvailable: true
    });

    console.log('Created Item:', item._id);

    // 2. Create requests
    const rA = await rentalService.createRental({
      itemId: item._id,
      startDate: '2026-12-01',
      endDate: '2026-12-05',
      message: 'Req A'
    }, renterA._id.toString());

    const rB = await rentalService.createRental({
      itemId: item._id,
      startDate: '2026-12-03',
      endDate: '2026-12-07',
      message: 'Req B (Overlap)'
    }, renterB._id.toString());

    const rC = await rentalService.createRental({
      itemId: item._id,
      startDate: '2026-12-10',
      endDate: '2026-12-15',
      message: 'Req C (No Overlap)'
    }, renterC._id.toString());

    console.log('Requests created. Statuses: Pending');

    // 3. Approve A
    console.log('Approving Req A (Dec 1-5)...');
    await rentalService.updateRentalStatus(rA._id, 'approved', owner._id.toString());

    // 4. Verify
    const finalRA = await Rental.findById(rA._id);
    const finalRB = await Rental.findById(rB._id);
    const finalRC = await Rental.findById(rC._id);

    console.log('--- RESULTS ---');
    console.log('Req A (Approved?):', finalRA.status); // Should be approved
    console.log('Req B (Cancelled?):', finalRB.status); // Should be cancelled (overlaps A)
    console.log('Req C (Pending?):', finalRC.status); // Should be pending (NO overlap)

    if (finalRA.status === 'approved' && finalRB.status === 'cancelled' && finalRC.status === 'pending') {
      console.log('SUCCESS: Overlap logic is working surgeries!');
    } else {
      console.error('FAILURE: Overlap logic failed!');
    }

    // Cleanup
    await Item.findByIdAndDelete(item._id);
    await Rental.deleteMany({ item: item._id });

  } catch (err) {
    console.error(err);
  } finally {
    await mongoose.disconnect();
  }
}

testOverlap();
