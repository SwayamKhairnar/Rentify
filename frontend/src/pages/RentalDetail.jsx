import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Clock,
  User,
  Mail,
  MapPin,
  Tag,
  MessageCircle,
  Check,
  X,
  AlertCircle,
  Package,
  Star,
  AlertTriangle,
} from 'lucide-react';
import { rentalService } from '../services/rental.service';
import { reviewService } from '../services/review.service';
import { reportService } from '../services/report.service';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import ReviewModal from '../components/ReviewModal';
import ReportModal from '../components/ReportModal';
import StarRating from '../components/StarRating';
import { formatPrice, formatDate, timeAgo, getPlaceholderImage } from '../utils/helpers';
import './RentalDetail.css';

/**
 * RentalDetail page — full detailed view of a single rental request.
 * Shows different layouts/actions based on whether the viewer is the renter or owner.
 */
export default function RentalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast, showToast } = useToast();

  const [rental, setRental] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [hasReviewed, setHasReviewed] = useState(false);
  const [rentalReviews, setRentalReviews] = useState([]);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [reportSubmitting, setReportSubmitting] = useState(false);

  useEffect(() => {
    if (id) {
      fetchRental();
    }
  }, [id, user?._id]);

  async function fetchRental() {
    try {
      const res = await rentalService.getRental(id);
      setRental(res.data.rental);
      
      // Check if user has already reviewed
      if (res.data.rental.status === 'completed') {
        const reviewsRes = await reviewService.getRentalReviews(id);
        setRentalReviews(reviewsRes.data.reviews || []);
        const userReview = reviewsRes.data.reviews.find(r => r.reviewer._id === user?._id);
        if (userReview) setHasReviewed(true);
      }
    } catch (err) {
      showToast('Failed to load rental details', 'error');
      navigate('/rentals');
    } finally {
      setLoading(false);
    }
  }

  async function handleReviewSubmit({ rating, itemRating, comment }) {
    setReviewSubmitting(true);
    try {
      await reviewService.createReview({
        rentalId: id,
        rating,
        itemRating,
        comment
      });
      showToast('Review submitted successfully!');
      setIsReviewModalOpen(false);
      setHasReviewed(true);
      fetchRental(); // Refresh to show updated ratings if needed
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setReviewSubmitting(false);
    }
  }

  async function handleReportSubmit(reportData) {
    setReportSubmitting(true);
    try {
      await reportService.submitReport({
        ...reportData,
        rentalId: id,
        reportedUserId: isOwner ? rental.renter?._id : rental.owner?._id
      });
      showToast('Report submitted to admins. We will review it shortly.');
      setIsReportModalOpen(false);
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setReportSubmitting(false);
    }
  }

  async function handleAction(status) {
    setActionLoading(true);
    try {
      await rentalService.updateStatus(id, status);
      showToast(`Rental ${status} successfully`);
      
      // Auto-open review modal when marked completed
      if (status === 'completed') {
        setIsReviewModalOpen(true);
      }
      
      fetchRental();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) return <LoadingSpinner />;
  if (!rental) return null;

  const isRenter = user?._id === rental.renter?._id;
  const isOwner  = user?._id === rental.owner?._id;

  // Computed fields
  const start = new Date(rental.startDate);
  const end   = new Date(rental.endDate);
  const days  = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
  const itemImg = rental.item?.images?.[0] || getPlaceholderImage(rental.item?.category);

  const hasOffer  = rental.offerPrice != null;
  const savings   = hasOffer ? rental.totalPrice - rental.offerPrice : 0;
  const offerLess = hasOffer && rental.offerPrice < rental.totalPrice;
  const offerMore = hasOffer && rental.offerPrice > rental.totalPrice;

  const statusMeta = {
    pending:   { label: 'Pending',   cls: 'badge-pending'   },
    approved:  { label: 'Approved',  cls: 'badge-approved'  },
    active:    { label: 'Active',    cls: 'badge-active'    },
    completed: { label: 'Completed', cls: 'badge-completed' },
    rejected:  { label: 'Rejected',  cls: 'badge-rejected'  },
    cancelled: { label: 'Cancelled', cls: 'badge-cancelled' },
  };
  const sm = statusMeta[rental.status] || { label: rental.status, cls: 'badge-pending' };

  return (
    <div className="page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        {/* Back button */}
        <button className="btn btn-ghost back-btn" onClick={() => navigate('/rentals')}>
          <ArrowLeft size={18} /> Back to Requests
        </button>

        <div className="rd-layout">
          {/* ─── LEFT COLUMN ─── */}
          <div className="rd-left">

            {/* Item card */}
            <div className="rd-section card rd-item-card" id="rd-item-info">
              <img
                src={itemImg}
                alt={rental.item?.title || 'Deleted Item'}
                className="rd-item-img"
              />
              <div className="rd-item-body">
                <div className="rd-item-category">
                  <Package size={13} /> {rental.item?.category || 'No Category'}
                </div>
                {rental.item ? (
                  <Link to={`/items/${rental.item._id}`} className="rd-item-title">
                    {rental.item.title}
                  </Link>
                ) : (
                  <div className="rd-item-title-deleted">Product no longer available</div>
                )}
                <div className="rd-item-ppd">
                  {formatPrice(rental.item?.pricePerDay || 0)}
                  <span>/day</span>
                </div>
              </div>
            </div>

            {/* Rental period */}
            <div className="rd-section card" id="rd-period">
              <div className="rd-section-header">
                <Calendar size={16} /> Rental Period
              </div>
              <div className="rd-period-grid">
                <div className="rd-period-box">
                  <div className="rd-period-label">Start Date</div>
                  <div className="rd-period-val">{formatDate(rental.startDate)}</div>
                </div>
                <div className="rd-period-arrow">→</div>
                <div className="rd-period-box">
                  <div className="rd-period-label">End Date</div>
                  <div className="rd-period-val">{formatDate(rental.endDate)}</div>
                </div>
              </div>
              <div className="rd-duration">
                <Clock size={13} /> {days} {days === 1 ? 'day' : 'days'} total
              </div>
            </div>

            {/* Price breakdown */}
            <div className="rd-section card" id="rd-pricing">
              <div className="rd-section-header">
                <Tag size={16} /> Price Breakdown
              </div>
              <div className="rd-price-row">
                <span>
                  {formatPrice(rental.item?.pricePerDay || 0)}/day × {days} {days === 1 ? 'day' : 'days'}
                </span>
                <span className="rd-price-val">{formatPrice(rental.totalPrice)}</span>
              </div>
              <div className="rd-price-divider" />
              <div className="rd-price-row rd-price-total">
                <span>Listed Total</span>
                <span className="rd-price-total-val">{formatPrice(rental.totalPrice)}</span>
              </div>

              {hasOffer && (
                <div className={`rd-offer-box ${offerLess ? 'lower' : offerMore ? 'higher' : 'same'}`}>
                  <div className="rd-offer-header">
                    {isRenter ? 'Your Offer' : `${rental.renter?.name?.split(' ')[0]}'s Offer`}
                  </div>
                  <div className="rd-offer-amount">{formatPrice(rental.offerPrice)}</div>
                  {offerLess && (
                    <div className="rd-offer-note">
                      💰 {formatPrice(Math.abs(savings))} less than listed price
                    </div>
                  )}
                  {offerMore && (
                    <div className="rd-offer-note higher">
                      ⬆ {formatPrice(Math.abs(savings))} more than listed price
                    </div>
                  )}
                  {!offerLess && !offerMore && (
                    <div className="rd-offer-note same">
                      ✓ Matches listed price exactly
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* ─── RIGHT COLUMN ─── */}
          <div className="rd-right">

            {/* Status header */}
            <div className="rd-section card rd-status-card" id="rd-status">
              <div className="rd-status-top">
                <div>
                  <div className="rd-status-label">Request Status</div>
                  <span className={`badge ${sm.cls} rd-status-badge`}>{sm.label}</span>
                </div>
                <div className="rd-status-time">
                  <Clock size={13} />
                  Submitted {timeAgo(rental.createdAt)}
                </div>
              </div>

              {/* Action buttons */}
              {/* Owner: pending → approve / reject */}
              {isOwner && rental.status === 'pending' && (
                <div className="rd-actions">
                  <button
                    className="btn btn-primary"
                    onClick={() => handleAction('approved')}
                    disabled={actionLoading}
                    id="rd-approve-btn"
                  >
                    <Check size={16} /> Approve
                  </button>
                  <button
                    className="btn btn-danger"
                    onClick={() => handleAction('rejected')}
                    disabled={actionLoading}
                    id="rd-reject-btn"
                  >
                    <X size={16} /> Reject
                  </button>
                </div>
              )}

              {/* Owner: approved → mark active */}
              {isOwner && rental.status === 'approved' && (
                <div className="rd-actions">
                  <button
                    className="btn btn-primary"
                    onClick={() => handleAction('active')}
                    disabled={actionLoading}
                    id="rd-active-btn"
                  >
                    <Check size={16} /> Mark as Active
                  </button>
                </div>
              )}

              {/* Owner: active → complete */}
              {isOwner && rental.status === 'active' && (
                <div className="rd-actions">
                  <button
                    className="btn btn-primary"
                    onClick={() => handleAction('completed')}
                    disabled={actionLoading}
                    id="rd-complete-btn"
                  >
                    <Check size={16} /> Mark as Completed
                  </button>
                </div>
              )}

              {/* Renter: pending → cancel */}
              {isRenter && rental.status === 'pending' && (
                <div className="rd-actions">
                  <button
                    className="btn btn-danger"
                    onClick={() => handleAction('cancelled')}
                    disabled={actionLoading}
                    id="rd-cancel-btn"
                  >
                    <X size={16} /> Cancel Request
                  </button>
                </div>
              )}

              {/* Either party: approved/active → cancel */}
              {(isRenter || isOwner) && ['approved', 'active'].includes(rental.status) && (
                <div className="rd-actions">
                  <button
                    className="btn btn-danger"
                    onClick={() => handleAction('cancelled')}
                    disabled={actionLoading}
                    id="rd-cancel-late-btn"
                  >
                    <X size={16} /> Cancel Rental
                  </button>
                </div>
              )}
              {/* Completed actions: Review & Report */}
              {rental.status === 'completed' && (isRenter || isOwner) && (
                <div className="rd-actions rd-completed-actions">
                  {!hasReviewed && (
                    <button
                      className="btn btn-primary rd-review-btn"
                      onClick={() => setIsReviewModalOpen(true)}
                      id="rd-review-btn"
                    >
                      <Star size={16} fill="white" /> Leave a Review
                    </button>
                  )}
                  <button
                    className="btn btn-ghost rd-report-btn"
                    onClick={() => setIsReportModalOpen(true)}
                    id="rd-report-btn"
                  >
                    <AlertTriangle size={16} /> Report User
                  </button>
                </div>
              )}
            </div>

            {/* Existing Review Info */}
            {rental.status === 'completed' && hasReviewed && (
              <div className="rd-section card rd-info-note rd-success-banner">
                <Check size={14} /> You have already reviewed this rental. Thank you!
              </div>
            )}

            {/* Display Reviews */}
            {rental.status === 'completed' && rentalReviews.length > 0 && (
              <div className="rd-section card" id="rd-reviews">
                <div className="rd-section-header">
                  <Star size={16} /> Rental Reviews
                </div>
                <div className="rd-reviews-list">
                  {rentalReviews.map((rev) => (
                    <div key={rev._id} className="rd-review-item">
                      <div className="rd-review-item-header">
                        <div className="rd-review-item-author">{rev.reviewer?.name}</div>
                        <StarRating rating={rev.rating} size={12} />
                      </div>
                      {rev.comment && <p className="rd-review-item-comment">"{rev.comment}"</p>}
                      <div className="rd-review-item-date">
                        {formatDate(rev.createdAt)}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Person card: for owner — show renter info; for renter — show owner info */}
            {isOwner && rental.renter && (
              <div className="rd-section card rd-person-card" id="rd-renter-info">
                <div className="rd-section-header">
                  <User size={16} /> Requester Details
                </div>
                <div className="rd-person-avatar">
                  {rental.renter.name?.charAt(0).toUpperCase()}
                </div>
                <div className="rd-person-name">{rental.renter.name}</div>
                <div className="rd-person-meta">
                  <Mail size={13} /> {rental.renter.email}
                </div>
                {rental.renter.campus && (
                  <div className="rd-person-meta">
                    <MapPin size={13} /> {rental.renter.campus}
                  </div>
                )}
              </div>
            )}

            {isRenter && (
              <div className="rd-section card rd-person-card" id="rd-owner-info">
                <div className="rd-section-header">
                  <User size={16} /> Owner Details
                </div>
                {rental.owner ? (
                  <>
                    <Link to={`/profile/${rental.owner._id}`} className="rd-person-link">
                      <div className="rd-person-avatar">
                        {rental.owner.name?.charAt(0).toUpperCase()}
                      </div>
                      <div className="rd-person-name">{rental.owner.name}</div>
                    </Link>
                    <div className="rd-person-meta">
                      <Mail size={13} /> {rental.owner.email}
                    </div>
                    {rental.owner.campus && (
                      <div className="rd-person-meta">
                        <MapPin size={13} /> {rental.owner.campus}
                      </div>
                    )}
                  </>
                ) : (
                  <div className="rd-person-deleted">User account removed</div>
                )}
              </div>
            )}

            {/* Message from renter */}
            {rental.message && (
              <div className="rd-section card" id="rd-message">
                <div className="rd-section-header">
                  <MessageCircle size={16} />
                  {isOwner ? `Message from ${rental.renter?.name?.split(' ')[0]}` : 'Your Message'}
                </div>
                <p className="rd-message-text">"{rental.message}"</p>
              </div>
            )}

            {/* Chat link */}
            {(isRenter || isOwner) && (
              <Link to="/chat" className="btn btn-secondary rd-chat-btn" id="rd-chat-link">
                <MessageCircle size={16} /> Open Chat with {isOwner ? rental.renter?.name?.split(' ')[0] : rental.owner?.name?.split(' ')[0]}
              </Link>
            )}

            {/* Info note */}
            {rental.status === 'pending' && isRenter && (
              <div className="rd-info-note">
                <AlertCircle size={14} />
                Your request is awaiting approval. You'll be notified when the owner responds.
              </div>
            )}
          </div>
        </div>
      </div>

      <ReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        onSubmit={handleReviewSubmit}
        revieweeName={isOwner ? rental.renter?.name : rental.owner?.name}
        itemName={rental.item?.title}
        isLenderReview={isRenter}
        submitting={reviewSubmitting}
      />

      <ReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSubmit={handleReportSubmit}
        reportedUserName={isOwner ? rental.renter?.name : rental.owner?.name}
        submitting={reportSubmitting}
      />
    </div>
  );
}
