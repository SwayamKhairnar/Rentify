import { useState } from 'react';
import { Star, X, Send } from 'lucide-react';
import './ReviewModal.css';

/**
 * ReviewModal — a premium modal overlay for submitting a rating + comment
 * after a rental is completed.
 *
 * Props:
 *  - isOpen (boolean)
 *  - onClose (function)
 *  - onSubmit (function({ rating, itemRating, comment }))
 *  - revieweeName (string) — name of the person being reviewed
 *  - itemName (string) — name of the item being reviewed (optional)
 *  - isLenderReview (boolean) — if true, shows item rating as well
 *  - submitting (boolean) — loading state during API call
 */
export default function ReviewModal({ 
  isOpen, 
  onClose, 
  onSubmit, 
  revieweeName, 
  itemName = 'the product',
  isLenderReview = false, 
  submitting = false 
}) {
  const [rating, setRating] = useState(0);
  const [hovered, setHovered] = useState(0);
  const [itemRating, setItemRating] = useState(0);
  const [itemHovered, setItemHovered] = useState(0);
  const [comment, setComment] = useState('');

  if (!isOpen) return null;

  const labels = ['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent'];
  const activeRating = hovered || rating;

  function handleSubmit(e) {
    e.preventDefault();
    if (rating === 0) return;
    if (isLenderReview && itemRating === 0) return;
    
    onSubmit({ 
      rating, 
      itemRating: isLenderReview ? itemRating : undefined, 
      comment 
    });
  }

  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) onClose();
  }

  return (
    <div className="rm-overlay" onClick={handleOverlayClick}>
      <div className="rm-modal card" id="review-modal">
        {/* Close button */}
        <button className="rm-close" onClick={onClose} aria-label="Close">
          <X size={20} />
        </button>

        {/* Header */}
        <div className="rm-header">
          <div className="rm-icon-wrap">
            <Star size={28} fill="#FDCB6E" stroke="#FDCB6E" />
          </div>
          <h2 className="rm-title">Rate your experience</h2>
          <p className="rm-subtitle">
            {isLenderReview 
              ? `Share your feedback about the product and the owner.` 
              : `How was your experience with ${revieweeName}?`}
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Section 1: User Behavior */}
          <div className="rm-stars-section">
            <div className="rm-section-label">
              {isLenderReview ? `Owner Behavior (${revieweeName})` : 'Renter Behavior'}
            </div>
            <div className="rm-stars-wrap">
              <div className="rm-stars">
                {[1, 2, 3, 4, 5].map((val) => (
                  <button
                    key={val}
                    type="button"
                    className={`rm-star-btn ${val <= activeRating ? 'active' : ''}`}
                    onMouseEnter={() => setHovered(val)}
                    onMouseLeave={() => setHovered(0)}
                    onClick={() => setRating(val)}
                    aria-label={`Rate ${val} star${val > 1 ? 's' : ''}`}
                    id={`star-user-${val}`}
                  >
                    <Star
                      size={isLenderReview ? 28 : 36}
                      fill={val <= activeRating ? '#FDCB6E' : 'transparent'}
                      stroke={val <= activeRating ? '#FDCB6E' : '#6C6C8A'}
                      strokeWidth={1.5}
                    />
                  </button>
                ))}
              </div>
              <div className={`rm-star-label ${activeRating > 0 ? 'visible' : ''}`}>
                {labels[activeRating] || 'Select a rating'}
              </div>
            </div>
          </div>

          {/* Section 2: Item Quality (Lender Reviews Only) */}
          {isLenderReview && (
            <div className="rm-stars-section">
              <div className="rm-section-label">Product Quality ({itemName})</div>
              <div className="rm-stars-wrap">
                <div className="rm-stars">
                  {[1, 2, 3, 4, 5].map((val) => {
                    const activeItem = itemHovered || itemRating;
                    return (
                      <button
                        key={val}
                        type="button"
                        className={`rm-star-btn ${val <= activeItem ? 'active' : ''}`}
                        onMouseEnter={() => setItemHovered(val)}
                        onMouseLeave={() => setItemHovered(0)}
                        onClick={() => setItemRating(val)}
                        aria-label={`Rate item ${val} star${val > 1 ? 's' : ''}`}
                        id={`star-item-${val}`}
                      >
                        <Star
                          size={28}
                          fill={val <= activeItem ? '#FDCB6E' : 'transparent'}
                          stroke={val <= activeItem ? '#FDCB6E' : '#6C6C8A'}
                          strokeWidth={1.5}
                        />
                      </button>
                    );
                  })}
                </div>
                <div className={`rm-star-label ${itemHovered || itemRating > 0 ? 'visible' : ''}`}>
                  {labels[itemHovered || itemRating] || 'Select a rating'}
                </div>
              </div>
            </div>
          )}

          {/* Comment */}
          <div className="form-group rm-comment-group">
            <label className="form-label">Comment (optional)</label>
            <textarea
              className="form-input rm-textarea"
              placeholder="Share your experience..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
              maxLength={500}
              id="review-comment"
            />
            <div className="rm-char-count">{comment.length}/500</div>
          </div>

          {/* Actions */}
          <div className="rm-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={onClose}
              disabled={submitting}
            >
              Skip
            </button>
            <button
              type="submit"
              className="btn btn-primary rm-submit-btn"
              disabled={rating === 0 || (isLenderReview && itemRating === 0) || submitting}
              id="submit-review"
            >
              <Send size={16} />
              {submitting ? 'Submitting...' : 'Submit Review'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
