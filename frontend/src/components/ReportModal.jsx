import { useState } from 'react';
import { X, AlertTriangle, Camera, Send, Loader2 } from 'lucide-react';
import { uploadService } from '../services/upload.service';
import './ReportModal.css';

/**
 * ReportModal — form for reporting a user after a transaction.
 */
export default function ReportModal({ 
  isOpen, 
  onClose, 
  onSubmit, 
  reportedUserName,
  submitting = false 
}) {
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState('');
  const [uploading, setUploading] = useState(false);

  if (!isOpen) return null;

  const reasons = [
    'Late Return',
    'Item Damage',
    'Fake Product/Description',
    'Inappropriate Behavior',
    'Payment Issues',
    'No Show',
    'Other'
  ];

  async function handleImageChange(e) {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    try {
      const urls = await uploadService.uploadImages([file]);
      if (urls && urls.length > 0) {
        setImage(urls[0]);
        setPreview(URL.createObjectURL(file));
      }
    } catch (err) {
      console.error('Image upload failed');
    } finally {
      setUploading(false);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!reason || !description) return;
    
    onSubmit({ 
      reason, 
      description, 
      evidenceImage: image 
    });
  }

  return (
    <div className="rm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="report-modal card">
        <button className="rm-close" onClick={onClose} aria-label="Close">
          <X size={20} />
        </button>

        <div className="report-header">
          <div className="report-icon-bg">
            <AlertTriangle size={24} color="#FF7675" />
          </div>
          <h2>Report User</h2>
          <p>Submit a formal complaint against <strong>{reportedUserName}</strong>.</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Reason for Report</label>
            <select 
              className="form-input" 
              value={reason} 
              onChange={(e) => setReason(e.target.value)}
              required
            >
              <option value="">Select a reason...</option>
              {reasons.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Detailed Description</label>
            <textarea
              className="form-input"
              placeholder="Describe what happened in detail..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              required
              maxLength={1000}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Supporting Evidence (Photo)</label>
            <div className="report-upload">
              {preview ? (
                <div className="report-preview-wrap">
                  <img src={preview} alt="Evidence preview" className="report-preview" />
                  <button 
                    type="button" 
                    className="report-remove-img" 
                    onClick={() => { setImage(null); setPreview(''); }}
                  >
                    <X size={14} />
                  </button>
                </div>
              ) : (
                <label className="report-upload-placeholder">
                  <input type="file" accept="image/*" onChange={handleImageChange} hidden />
                  {uploading ? (
                    <Loader2 size={24} className="animate-spin" />
                  ) : (
                    <>
                      <Camera size={24} />
                      <span>Click to upload evidence</span>
                    </>
                  )}
                </label>
              )}
            </div>
          </div>

          <div className="report-footer">
            <button 
              type="button" 
              className="btn btn-ghost" 
              onClick={onClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="btn btn-danger report-submit-btn" 
              disabled={!reason || !description || submitting || uploading}
            >
              <Send size={16} />
              {submitting ? 'Submitting...' : 'Send Report to Admin'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
