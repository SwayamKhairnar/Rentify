import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { itemService } from '../services/item.service';
import { reviewService } from '../services/review.service';
import Toast from '../components/Toast';
import ItemCard from '../components/ItemCard';
import StarRating from '../components/StarRating';
import LoadingSpinner from '../components/LoadingSpinner';
import { User, Mail, MapPin, Phone, Edit3, LogOut } from 'lucide-react';
import { formatDate } from '../utils/helpers';
import './Profile.css';

/**
 * Profile page — displays user info, their listed items, and received reviews.
 */
export default function Profile() {
  const { user, logout, setUser } = useAuth();
  const { toast, showToast } = useToast();

  const [tab, setTab] = useState('items');
  const [items, setItems] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ name: '', bio: '', campus: '', phone: '' });

  useEffect(() => {
    if (user) {
      setForm({ name: user.name, bio: user.bio || '', campus: user.campus || '', phone: user.phone || '' });
      fetchData();
    }
  }, [user]);

  async function fetchData() {
    setLoading(true);
    try {
      const [itemsRes, reviewsRes] = await Promise.all([
        itemService.getMyItems(),
        reviewService.getUserReviews(user._id),
      ]);
      setItems(itemsRes.data.items || []);
      setReviews(reviewsRes.data.reviews || []);
    } catch (err) {
      console.error('Failed to load profile data');
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    try {
      const api = (await import('../services/api')).default;
      const res = await api.put('/users/profile', form);
      setUser(res.data.user);
      setEditing(false);
      showToast('Profile updated');
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  if (!user) return <LoadingSpinner />;

  return (
    <div className="page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        <div className="profile-layout">
          {/* Profile Card */}
          <div className="profile-card card" id="profile-card">
            <div className="profile-card-body">
              <div className="profile-avatar">
                {user.name?.charAt(0).toUpperCase()}
              </div>

              {editing ? (
                <form onSubmit={handleSaveProfile} className="profile-edit-form">
                  <div className="form-group">
                    <label className="form-label">Name</label>
                    <input
                      className="form-input"
                      value={form.name}
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Bio</label>
                    <textarea
                      className="form-input"
                      value={form.bio}
                      onChange={(e) => setForm({ ...form, bio: e.target.value })}
                      rows={3}
                      maxLength={300}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Campus</label>
                    <input
                      className="form-input"
                      value={form.campus}
                      onChange={(e) => setForm({ ...form, campus: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Phone</label>
                    <input
                      className="form-input"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    />
                  </div>
                  <div className="profile-edit-actions">
                    <button type="submit" className="btn btn-primary btn-sm">Save</button>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing(false)}>Cancel</button>
                  </div>
                </form>
              ) : (
                <>
                  <h2 className="profile-name">{user.name}</h2>
                  <div className="profile-rating-group">
                    <div className="overall-rating">
                      <StarRating rating={user.rating} size={20} />
                      <span className="total-count">({user.totalReviews || 0} reviews)</span>
                    </div>
                  </div>

                  <div className="reputation-breakdown">
                    <div className="rep-item">
                      <div className="rep-label">As a Lender</div>
                      <div className="rep-scores">
                        <div className="rep-sub">
                          <span>Behavior</span>
                          <StarRating rating={user.lenderRating || 0} size={12} showValue={true} />
                        </div>
                        <div className="rep-sub">
                          <span>Products</span>
                          <StarRating rating={user.itemQualityAverage || 0} size={12} showValue={true} />
                        </div>
                      </div>
                    </div>
                    <div className="rep-item">
                      <div className="rep-label">As a Renter</div>
                      <div className="rep-scores">
                        <div className="rep-sub">
                          <span>Behavior</span>
                          <StarRating rating={user.renterRating || 0} size={12} showValue={true} />
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="profile-details">
                    <div className="profile-detail">
                      <Mail size={16} /> {user.email}
                    </div>
                    {user.campus && (
                      <div className="profile-detail">
                        <MapPin size={16} /> {user.campus}
                      </div>
                    )}
                    {user.phone && (
                      <div className="profile-detail">
                        <Phone size={16} /> {user.phone}
                      </div>
                    )}
                    {user.bio && <p className="profile-bio">{user.bio}</p>}
                    <div className="profile-detail" style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      Joined {formatDate(user.createdAt)}
                    </div>
                  </div>

                  <div className="profile-actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => setEditing(true)}>
                      <Edit3 size={14} /> Edit Profile
                    </button>
                    <button className="btn btn-ghost btn-sm" onClick={logout} style={{ color: 'var(--danger)' }}>
                      <LogOut size={14} /> Logout
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Content Area */}
          <div className="profile-content">
            <div className="tabs">
              <button className={`tab ${tab === 'items' ? 'active' : ''}`} onClick={() => setTab('items')}>
                My Items ({items.length})
              </button>
              <button className={`tab ${tab === 'reviews' ? 'active' : ''}`} onClick={() => setTab('reviews')}>
                Reviews ({reviews.length})
              </button>
            </div>

            {loading ? (
              <LoadingSpinner />
            ) : tab === 'items' ? (
              items.length === 0 ? (
                <div className="empty-state">
                  <h3>No items listed</h3>
                  <p>Start listing items to rent them out to fellow students!</p>
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
                <p>Complete some rentals to start receiving reviews.</p>
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
