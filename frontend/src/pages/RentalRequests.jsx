import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { rentalService } from '../services/rental.service';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import { formatPrice, formatDate, timeAgo, getPlaceholderImage } from '../utils/helpers';
import { Check, X, Eye } from 'lucide-react';
import './RentalRequests.css';

/**
 * RentalRequests page — shows outgoing and incoming rental requests with status management.
 * Fix list applied:
 *  - offerPrice shown as badge on both tabs
 *  - rental duration (days) displayed
 *  - createdAt shown via timeAgo
 *  - item thumbnail on every card
 *  - renter email/campus shown on received tab
 *  - Mark Active button guarded to received tab only
 *  - Cancel button for requester on pending requests
 *  - Eye icon wired to /rentals/:id detail page
 */
export default function RentalRequests() {
  const [tab, setTab] = useState('mine');
  const [myRentals, setMyRentals] = useState([]);
  const [received, setReceived] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { toast, showToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    fetchRentals();
  }, []);

  async function fetchRentals() {
    setLoading(true);
    try {
      const [mineRes, receivedRes] = await Promise.all([
        rentalService.getMyRentals(),
        rentalService.getReceivedRequests(),
      ]);
      setMyRentals(mineRes.data.rentals || []);
      setReceived(receivedRes.data.rentals || []);
    } catch (err) {
      showToast('Failed to load rentals', 'error');
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusUpdate(rentalId, status) {
    try {
      await rentalService.updateStatus(rentalId, status);
      showToast(`Rental ${status} successfully`);
      fetchRentals();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  /** Compute rental duration in days */
  function getDays(rental) {
    const diff = new Date(rental.endDate) - new Date(rental.startDate);
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }

  const rentals = tab === 'mine' ? myRentals : received;

  return (
    <div className="page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        <h1 className="page-title">Rental Requests</h1>

        <div className="tabs" id="rental-tabs">
          <button
            className={`tab ${tab === 'mine' ? 'active' : ''}`}
            onClick={() => setTab('mine')}
          >
            My Requests ({myRentals.length})
          </button>
          <button
            className={`tab ${tab === 'received' ? 'active' : ''}`}
            onClick={() => setTab('received')}
          >
            Received ({received.length})
          </button>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : rentals.length === 0 ? (
          <div className="empty-state">
            <h3>No {tab === 'mine' ? 'outgoing' : 'incoming'} requests</h3>
            <p>
              {tab === 'mine'
                ? 'Browse items and send rental requests to get started.'
                : "When someone requests to rent your items, they'll appear here."}
            </p>
          </div>
        ) : (
          <div className="rental-list">
            {rentals.map((rental) => {
              const days = getDays(rental);
              const hasOffer = rental.offerPrice != null;
              const imgSrc = rental.item?.images?.[0] || getPlaceholderImage(rental.item?.category);

              return (
                <div key={rental._id} className="rental-card card" id={`rental-${rental._id}`}>
                  <div className="rental-card-inner">

                    {/* ── Thumbnail ── */}
                    <div
                      className="rental-thumb"
                      onClick={() => navigate(`/rentals/${rental._id}`)}
                      title="View details"
                    >
                      <img src={imgSrc} alt={rental.item?.title} />
                    </div>

                    {/* ── Item & Meta Info ── */}
                    <div className="rental-item-info">
                      {rental.item && (
                        <Link to={`/items/${rental.item._id}`} className="rental-item-title">
                          {rental.item.title}
                        </Link>
                      )}

                      {/* Dates + duration */}
                      <div className="rental-dates">
                        {formatDate(rental.startDate)} — {formatDate(rental.endDate)}
                        <span className="rental-duration">· {days} {days === 1 ? 'day' : 'days'}</span>
                      </div>

                      {/* Who */}
                      <div className="rental-person">
                        {tab === 'mine'
                          ? `Owner: ${rental.owner?.name || 'Unknown'}`
                          : `Renter: ${rental.renter?.name || 'Unknown'}`}
                        {/* Show renter email/campus to owner */}
                        {tab === 'received' && rental.renter?.email && (
                          <span className="rental-person-email"> · {rental.renter.email}</span>
                        )}
                        {tab === 'received' && rental.renter?.campus && (
                          <span className="rental-person-email"> · {rental.renter.campus}</span>
                        )}
                      </div>

                      {/* Request time */}
                      <div className="rental-time">
                        Requested {timeAgo(rental.createdAt)}
                      </div>

                      {/* Message preview */}
                      {rental.message && (
                        <div className="rental-message">"{rental.message}"</div>
                      )}
                    </div>

                    {/* ── Right: Prices + Badge + Actions ── */}
                    <div className="rental-right">
                      {/* Total (listed) price */}
                      <div className="rental-price">{formatPrice(rental.totalPrice)}</div>

                      {/* Offer price badge */}
                      {hasOffer && (
                        <div
                          className={`rental-offer-badge ${rental.offerPrice < rental.totalPrice ? 'lower' : rental.offerPrice > rental.totalPrice ? 'higher' : 'same'}`}
                          title={tab === 'mine' ? 'Your offer price' : "Renter's offer price"}
                        >
                          Offer: {formatPrice(rental.offerPrice)}
                        </div>
                      )}

                      <span className={`badge badge-${rental.status}`}>{rental.status}</span>

                      {/* View details */}
                      <button
                        className="btn btn-sm btn-secondary rd-view-btn"
                        onClick={() => navigate(`/rentals/${rental._id}`)}
                        title="View full details"
                        id={`view-rental-${rental._id}`}
                      >
                        <Eye size={14} /> Details
                      </button>

                      {/* Owner: approve / reject (pending) */}
                      {tab === 'received' && rental.status === 'pending' && (
                        <div className="rental-actions">
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={() => handleStatusUpdate(rental._id, 'approved')}
                            title="Approve"
                          >
                            <Check size={16} /> Approve
                          </button>
                          <button
                            className="btn btn-sm btn-danger"
                            onClick={() => handleStatusUpdate(rental._id, 'rejected')}
                            title="Reject"
                          >
                            <X size={16} /> Reject
                          </button>
                        </div>
                      )}

                      {/* Owner only: mark active (approved) — FIX: was missing tab guard */}
                      {tab === 'received' && rental.status === 'approved' && (
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => handleStatusUpdate(rental._id, 'active')}
                        >
                          Mark Active
                        </button>
                      )}

                      {/* Owner only: mark completed (active) */}
                      {tab === 'received' && rental.status === 'active' && (
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => handleStatusUpdate(rental._id, 'completed')}
                        >
                          Mark Completed
                        </button>
                      )}

                      {/* Requester: cancel (pending) — FIX: was completely missing */}
                      {tab === 'mine' && rental.status === 'pending' && (
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleStatusUpdate(rental._id, 'cancelled')}
                          title="Cancel request"
                        >
                          <X size={16} /> Cancel
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
