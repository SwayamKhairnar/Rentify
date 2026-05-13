import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { itemService } from '../services/item.service';
import { reviewService } from '../services/review.service';
import api from '../services/api';
import { useAuth } from '../hooks/useAuth';
import ItemCard from '../components/ItemCard';
import StarRating from '../components/StarRating';
import LoadingSpinner from '../components/LoadingSpinner';
import { Mail, MapPin, Phone } from 'lucide-react';
import { formatDate } from '../utils/helpers';
import './Profile.css';

/**
 * UserProfile page — displays public info for a specific user, their items, and their reviews.
 */
export default function UserProfile() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();

  const [targetUser, setTargetUser] = useState(null);
  const [tab, setTab] = useState('items');
  const [items, setItems] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (currentUser && currentUser._id === id) {
      navigate('/profile');
      return;
    }
    fetchData();
  }, [id, currentUser, navigate]);

  async function fetchData() {
    setLoading(true);
    try {
      const [userRes, itemsRes, reviewsRes] = await Promise.all([
        api.get(`/users/${id}`),
        itemService.getItems(`owner=${id}`),
        reviewService.getUserReviews(id),
      ]);
      setTargetUser(userRes.data.user);
      setItems(itemsRes.data || []);
      setReviews(reviewsRes.data.reviews || []);
    } catch (err) {
      console.error('Failed to load user profile');
      navigate('/');
    } finally {
      setLoading(false);
    }
  }

  if (loading) return <LoadingSpinner />;
  if (!targetUser) return null;

  return (
    <div className="page">
      <div className="container">
        <div className="profile-layout">
          {/* Profile Card */}
          <div className="profile-card card" id="profile-card">
            <div className="profile-card-body">
              <div className="profile-avatar">
                {targetUser.name?.charAt(0).toUpperCase()}
              </div>

              <h2 className="profile-name">{targetUser.name}</h2>
              <div className="profile-rating-group">
                <div className="overall-rating">
                  <StarRating rating={targetUser.rating} size={20} />
                  <span className="total-count">({targetUser.totalReviews || 0} reviews)</span>
                </div>
              </div>

              <div className="reputation-breakdown">
                <div className="rep-item">
                  <div className="rep-label">As a Lender</div>
                  <div className="rep-scores">
                    <div className="rep-sub">
                      <span>Behavior</span>
                      <StarRating rating={targetUser.lenderRating || 0} size={12} showValue={true} />
                    </div>
                    <div className="rep-sub">
                      <span>Products</span>
                      <StarRating rating={targetUser.itemQualityAverage || 0} size={12} showValue={true} />
                    </div>
                  </div>
                </div>
                <div className="rep-item">
                  <div className="rep-label">As a Renter</div>
                  <div className="rep-scores">
                    <div className="rep-sub">
                      <span>Behavior</span>
                      <StarRating rating={targetUser.renterRating || 0} size={12} showValue={true} />
                    </div>
                  </div>
                </div>
              </div>

              <div className="profile-details">
                <div className="profile-detail">
                  <Mail size={16} /> {targetUser.email}
                </div>
                {targetUser.campus && (
                  <div className="profile-detail">
                    <MapPin size={16} /> {targetUser.campus}
                  </div>
                )}
                {targetUser.phone && (
                  <div className="profile-detail">
                    <Phone size={16} /> {targetUser.phone}
                  </div>
                )}
                {targetUser.bio && <p className="profile-bio">{targetUser.bio}</p>}
                <div className="profile-detail" style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  Joined {formatDate(targetUser.createdAt)}
                </div>
              </div>
            </div>
          </div>

          {/* Content Area */}
          <div className="profile-content">
            <div className="tabs">
              <button className={`tab ${tab === 'items' ? 'active' : ''}`} onClick={() => setTab('items')}>
                Listed Items ({items.length})
              </button>
              <button className={`tab ${tab === 'reviews' ? 'active' : ''}`} onClick={() => setTab('reviews')}>
                Reviews ({reviews.length})
              </button>
            </div>

            {tab === 'items' ? (
              items.length === 0 ? (
                <div className="empty-state">
                  <h3>No items listed</h3>
                  <p>This user hasn't listed any items for rent yet.</p>
                </div>
              ) : (
                <div className="items-grid">
                  {items.map((item) => (
                    <ItemCard key={item._id} item={item} />
                  ))}
                </div>
              )
            ) : reviews.length === 0 ? (
              <div className="empty-state">
                <h3>No reviews yet</h3>
                <p>This user hasn't received any reviews yet.</p>
              </div>
            ) : (
              <div className="reviews-list">
                {reviews.map((review) => (
                  <div key={review._id} className="review-card card">
                    <div className="card-body">
                      <div className="review-header">
                        <div className="review-author-wrap">
                          <div className="review-author">{review.reviewer?.name || 'Anonymous'}</div>
                          <div className={`review-type-badge ${review.type}`}>
                            {review.type === 'lender' ? 'Lending Experience' : 'Renting Experience'}
                          </div>
                        </div>
                        <div className="review-ratings">
                          <div className="rating-item">
                            <span>{review.type === 'lender' ? 'Owner' : 'Renter'}:</span>
                            <StarRating rating={review.rating} size={12} />
                          </div>
                          {review.itemRating && (
                            <div className="rating-item">
                              <span>Product:</span>
                              <StarRating rating={review.itemRating} size={12} />
                            </div>
                          )}
                        </div>
                      </div>
                      {review.comment && <p className="review-comment">"{review.comment}"</p>}
                      <div className="review-date">{formatDate(review.createdAt)}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
