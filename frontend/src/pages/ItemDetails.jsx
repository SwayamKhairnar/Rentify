import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { MapPin, Calendar, ArrowLeft, User, ChevronLeft, ChevronRight, X as CloseIcon, Edit3, Trash2 } from 'lucide-react';
import { itemService } from '../services/item.service';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { rentalService } from '../services/rental.service';
import StarRating from '../components/StarRating';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import { formatPrice, formatDate, getPlaceholderImage } from '../utils/helpers';
import './ItemDetails.css';

/**
 * ItemDetails page — shows full item info with rental request form.
 */
export default function ItemDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast, showToast } = useToast();

  const [item, setItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [message, setMessage] = useState('');
  const [offerPrice, setOfferPrice] = useState('');
  const [renting, setRenting] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Computed rental duration & cost (safe: item may be null on first render)
  const rentalDays = (() => {
    if (!startDate || !endDate) return 0;
    const diff = new Date(endDate) - new Date(startDate);
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
    return days > 0 ? days : 0;
  })();
  const totalCost = item ? rentalDays * (item.pricePerDay || 0) : 0;

  useEffect(() => {
    async function fetchItem() {
      try {
        const res = await itemService.getItem(id);
        setItem(res.data.item);
      } catch (err) {
        showToast('Item not found', 'error');
        navigate('/');
      } finally {
        setLoading(false);
      }
    }
    fetchItem();
  }, [id]);

  async function handleRent(e) {
    e.preventDefault();
    if (!user) {
      navigate('/login');
      return;
    }
    setRenting(true);
    try {
      await rentalService.createRental({
        itemId: item._id,
        startDate,
        endDate,
        message,
        offerPrice: offerPrice ? Number(offerPrice) : undefined,
      });
      showToast('Rental request sent!');
      setTimeout(() => navigate('/rentals'), 1500);
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setRenting(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm('Are you sure you want to delete this item?')) return;
    
    setDeleting(true);
    try {
      await itemService.deleteItem(item._id);
      showToast('Item deleted successfully');
      navigate('/profile');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setDeleting(false);
    }
  }

  if (loading) return <LoadingSpinner />;
  if (!item) return null;

  const imgSrc = item.images?.length > 0 ? item.images[activeImageIndex] : getPlaceholderImage(item.category);
  const isOwner = user && item.owner?._id === user._id;

  return (
    <div className="page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        <button className="btn btn-ghost back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={18} /> Back
        </button>

        <div className="item-detail" id="item-detail">
          <div className="item-detail-gallery">
            <div 
              className="item-detail-image id-image-zoom" 
              onClick={() => setIsLightboxOpen(true)}
              title="Click to zoom"
            >
              <img src={imgSrc} alt={item.title} />
              <span className="item-card-category">{item.category}</span>
            </div>
            {item.images?.length > 1 && (
              <div className="item-image-thumbnails">
                {item.images.map((img, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className={`thumbnail-btn ${idx === activeImageIndex ? 'active' : ''}`}
                    onClick={() => setActiveImageIndex(idx)}
                  >
                    <img src={img} alt={`${item.title} - ${idx + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="item-detail-info">
            <h1 className="item-detail-title">{item.title}</h1>

            {isOwner && (
              <div className="item-owner-actions id-owner-actions">
                <Link to={`/items/${item._id}/edit`} className="btn btn-secondary btn-sm">
                  <Edit3 size={16} /> Edit Item
                </Link>
                <button 
                  className="btn btn-sm id-btn-delete" 
                  onClick={handleDelete}
                  disabled={deleting}
                >
                  <Trash2 size={16} /> {deleting ? 'Deleting...' : 'Delete Item'}
                </button>
              </div>
            )}

            <div className="item-detail-header-row">
              <div className="item-detail-price">
                {formatPrice(item.pricePerDay)}
                <span>/day</span>
              </div>
              <div className="item-product-rating">
                <StarRating rating={item.rating || 0} size={18} showValue={true} />
                <span className="item-review-count">({item.totalReviews || 0} item reviews)</span>
              </div>
            </div>

            <div className="item-detail-meta">
              <span className={`badge badge-${item.condition === 'new' ? 'approved' : 'active'}`}>
                {item.condition}
              </span>
              {item.location && (
                <span className="item-detail-location">
                  <MapPin size={16} /> {item.location}
                </span>
              )}
              <span className="item-detail-date">
                <Calendar size={16} /> Listed {formatDate(item.createdAt)}
              </span>
            </div>

            <div className="item-detail-desc">
              <h3>Description</h3>
              <p>{item.description}</p>
            </div>

            {item.owner && (
              <Link to={`/profile/${item.owner._id}`} className="item-detail-owner">
                <div className="owner-avatar">
                  <User size={20} />
                </div>
                <div>
                  <div className="owner-name">{item.owner.name}</div>
                  <StarRating rating={item.owner.rating} size={14} />
                </div>
              </Link>
            )}

            {!isOwner && item.isAvailable && (
              <form className="rent-form" onSubmit={handleRent} id="rent-form">
                <h3>Request to Rent</h3>
                <div className="rent-dates">
                  <div className="form-group">
                    <label className="form-label">Start Date</label>
                    <input
                      type="date"
                      className="form-input"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      required
                      min={new Date().toISOString().split('T')[0]}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">End Date</label>
                    <input
                      type="date"
                      className="form-input"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      required
                      min={startDate || new Date().toISOString().split('T')[0]}
                    />
                  </div>
                </div>

                {/* ── Price Breakdown Card ── */}
                {rentalDays > 0 && (
                  <div className="price-breakdown" id="price-breakdown">
                    <div className="price-breakdown-header">
                      <span>Price Breakdown</span>
                    </div>
                    <div className="price-breakdown-row">
                      <span className="pb-label">
                        ₹{item.pricePerDay.toLocaleString('en-IN')}/day × {rentalDays} {rentalDays === 1 ? 'day' : 'days'}
                      </span>
                      <span className="pb-value">₹{totalCost.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="price-breakdown-total">
                      <span>Total (listed price)</span>
                      <span className="pb-total-value">₹{totalCost.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                )}

                {/* ── Offer Price ── */}
                <div className="form-group">
                  <label className="form-label offer-label">
                    Your Offer Price
                    <span className="offer-optional">(optional — negotiate with owner)</span>
                  </label>
                  <div className="offer-input-wrap">
                    <span className="offer-currency">₹</span>
                    <input
                      type="number"
                      className="form-input offer-input"
                      placeholder={rentalDays > 0 ? totalCost.toString() : 'e.g. 180'}
                      value={offerPrice}
                      onChange={(e) => setOfferPrice(e.target.value)}
                      min={1}
                      id="offer-price"
                    />
                  </div>
                  {offerPrice && rentalDays > 0 && Number(offerPrice) < totalCost && (
                    <p className="offer-savings">
                      You're offering ₹{(totalCost - Number(offerPrice)).toLocaleString('en-IN')} less than the listed price
                    </p>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">Message (optional)</label>
                  <textarea
                    className="form-input"
                    placeholder="Introduce yourself and explain your need..."
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    rows={3}
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary btn-lg id-rent-submit"
                  disabled={renting}
                  id="rent-submit"
                >
                  {renting ? 'Sending request...' : 'Send Rental Request'}
                </button>
              </form>
            )}

            {!item.isAvailable && (
              <div className="badge badge-cancelled id-unavailable-banner">
                This item is currently unavailable
              </div>
            )}
          </div>
        </div>
      </div>

      {isLightboxOpen && (
        <div className="lightbox-overlay" onClick={() => setIsLightboxOpen(false)}>
          <button className="lightbox-close" onClick={() => setIsLightboxOpen(false)}>
            <CloseIcon size={24} />
          </button>
          
          {item.images?.length > 1 && (
            <button 
              className="lightbox-prev" 
              onClick={(e) => {
                e.stopPropagation();
                setActiveImageIndex((prev) => (prev > 0 ? prev - 1 : item.images.length - 1));
              }}
            >
              <ChevronLeft size={36} />
            </button>
          )}

          <img 
            src={imgSrc} 
            alt={item.title} 
            className="lightbox-img" 
            onClick={(e) => e.stopPropagation()} 
          />

          {item.images?.length > 1 && (
            <button 
              className="lightbox-next" 
              onClick={(e) => {
                e.stopPropagation();
                setActiveImageIndex((prev) => (prev < item.images.length - 1 ? prev + 1 : 0));
              }}
            >
              <ChevronRight size={36} />
            </button>
          )}
        </div>
      )}
    </div>
  );
}
