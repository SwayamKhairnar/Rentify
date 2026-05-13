import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { itemService } from '../services/item.service';
import { uploadService } from '../services/upload.service';
import { useToast } from '../hooks/useToast';
import Toast from '../components/Toast';
import { ArrowLeft } from 'lucide-react';
import './CreateItem.css'; // Reusing CSS from CreateItem

const CATEGORIES = [
  'textbooks', 'electronics', 'bikes', 'cameras',
  'furniture', 'clothing', 'sports', 'instruments', 'other',
];

const CONDITIONS = ['new', 'like-new', 'good', 'fair', 'poor'];

export default function EditItem() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast, showToast } = useToast();
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: 'other',
    pricePerDay: '',
    condition: 'good',
    location: '',
  });
  
  const [existingImages, setExistingImages] = useState([]);
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);

  useEffect(() => {
    async function fetchItem() {
      try {
        const res = await itemService.getItem(id);
        const item = res.data.item;
        
        setForm({
          title: item.title,
          description: item.description,
          category: item.category,
          pricePerDay: item.pricePerDay,
          condition: item.condition,
          location: item.location || '',
        });
        
        setExistingImages(item.images || []);
      } catch (err) {
        showToast('Failed to load item for editing', 'error');
        setTimeout(() => navigate('/'), 1500);
      } finally {
        setLoading(false);
      }
    }
    
    fetchItem();
  }, [id, navigate]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: name === 'pricePerDay' ? (value === '' ? '' : Number(value)) : value,
    }));
  }

  function handleImageChange(e) {
    const files = Array.from(e.target.files);
    if (files.length + imageFiles.length + existingImages.length > 5) {
      showToast('Maximum 5 images allowed in total', 'error');
      return;
    }
    
    setImageFiles(prev => [...prev, ...files]);
    
    const newPreviews = files.map(file => URL.createObjectURL(file));
    setImagePreviews(prev => [...prev, ...newPreviews]);
  }

  function removeExistingImage(index) {
    setExistingImages(prev => prev.filter((_, i) => i !== index));
  }

  function removeNewImage(index) {
    setImageFiles(prev => prev.filter((_, i) => i !== index));
    URL.revokeObjectURL(imagePreviews[index]);
    setImagePreviews(prev => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      let uploadedImageUrls = [];
      if (imageFiles.length > 0) {
        uploadedImageUrls = await uploadService.uploadImages(imageFiles);
      }

      await itemService.updateItem(id, {
        ...form,
        pricePerDay: Number(form.pricePerDay),
        images: [...existingImages, ...uploadedImageUrls],
      });
      
      showToast('Item updated successfully!');
      setTimeout(() => navigate(`/items/${id}`), 1500);
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="page" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>Loading...</div>;
  }

  return (
    <div className="page">
      {toast && <Toast message={toast.message} type={toast.type} />}

      <div className="container">
        <button className="btn btn-ghost back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={18} /> Back
        </button>

        <div className="create-item-container">
          <h1 className="create-item-title">Edit Item</h1>
          <p className="create-item-subtitle">Update the details of your rental listing</p>

          <form className="create-item-form" onSubmit={handleSubmit} id="edit-item-form">
            <div className="form-group">
              <label className="form-label" htmlFor="item-title">Title</label>
              <input
                type="text"
                id="item-title"
                name="title"
                className="form-input"
                value={form.title}
                onChange={handleChange}
                required
                maxLength={100}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="item-description">Description</label>
              <textarea
                id="item-description"
                name="description"
                className="form-input"
                value={form.description}
                onChange={handleChange}
                required
                maxLength={1000}
                rows={4}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="item-category">Category</label>
                <select
                  id="item-category"
                  name="category"
                  className="form-select"
                  value={form.category}
                  onChange={handleChange}
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat.charAt(0).toUpperCase() + cat.slice(1)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="item-condition">Condition</label>
                <select
                  id="item-condition"
                  name="condition"
                  className="form-select"
                  value={form.condition}
                  onChange={handleChange}
                >
                  {CONDITIONS.map((cond) => (
                    <option key={cond} value={cond}>
                      {cond.charAt(0).toUpperCase() + cond.slice(1)}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="item-price">Price per Day (₹)</label>
                <input
                  type="number"
                  id="item-price"
                  name="pricePerDay"
                  className="form-input"
                  value={form.pricePerDay}
                  onChange={handleChange}
                  required
                  min={1}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="item-location">Location</label>
                <input
                  type="text"
                  id="item-location"
                  name="location"
                  className="form-input"
                  value={form.location}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Images (Max 5 Total)</label>
              <div className="image-upload-container">
                <input
                  type="file"
                  id="item-images"
                  className="file-input-hidden"
                  multiple
                  accept="image/*"
                  onChange={handleImageChange}
                  disabled={existingImages.length + imageFiles.length >= 5}
                />
                <label 
                  htmlFor="item-images" 
                  className={`image-upload-dropzone ${existingImages.length + imageFiles.length >= 5 ? 'disabled' : ''}`}
                >
                  <div className="dropzone-content">
                    <span className="upload-icon">📸</span>
                    <span>Click to add more photos</span>
                  </div>
                </label>
                
                {(existingImages.length > 0 || imagePreviews.length > 0) && (
                  <div className="image-previews">
                    {/* Existing Images */}
                    {existingImages.map((src, index) => (
                      <div key={`existing-${index}`} className="image-preview-wrapper">
                        <img src={src} alt="existing" className="image-preview" />
                        <button 
                          type="button" 
                          className="remove-image-btn"
                          onClick={() => removeExistingImage(index)}
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                    {/* New Images */}
                    {imagePreviews.map((src, index) => (
                      <div key={`new-${index}`} className="image-preview-wrapper">
                        <img src={src} alt="new preview" className="image-preview" style={{ border: '2px solid var(--primary)' }} />
                        <button 
                          type="button" 
                          className="remove-image-btn"
                          onClick={() => removeNewImage(index)}
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-lg"
              disabled={saving}
              style={{ width: '100%', marginTop: '8px' }}
            >
              {saving ? 'Updating...' : 'Save Changes'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
