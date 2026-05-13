const { cloudinary } = require('../config/cloudinary');

/**
 * Extracts the public_id from a Cloudinary URL.
 * Example URL: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/rentify/image_name.jpg
 * Public ID: rentify/image_name
 */
const getPublicIdFromUrl = (url) => {
  if (!url || !url.includes('upload/')) return null;
  
  try {
    const parts = url.split('upload/');
    if (parts.length < 2) return null;
    
    // Remove version (v1234567890/) if present
    let publicIdWithExt = parts[1];
    if (publicIdWithExt.startsWith('v')) {
      const slashIndex = publicIdWithExt.indexOf('/');
      if (slashIndex !== -1) {
        publicIdWithExt = publicIdWithExt.substring(slashIndex + 1);
      }
    }
    
    // Remove extension
    const lastDotIndex = publicIdWithExt.lastIndexOf('.');
    if (lastDotIndex !== -1) {
      return publicIdWithExt.substring(0, lastDotIndex);
    }
    
    return publicIdWithExt;
  } catch (error) {
    console.error('Error parsing Cloudinary URL:', error);
    return null;
  }
};

/**
 * Deletes an image from Cloudinary using its URL.
 */
const deleteImageFromCloudinary = async (url) => {
  const publicId = getPublicIdFromUrl(url);
  if (!publicId) return;
  
  try {
    await cloudinary.uploader.destroy(publicId);
  } catch (error) {
    console.error(`Failed to delete image ${publicId} from Cloudinary:`, error);
  }
};

/**
 * Deletes multiple images from Cloudinary.
 */
const deleteMultipleImagesFromCloudinary = async (urls) => {
  if (!urls || !Array.isArray(urls)) return;
  
  const publicIds = urls
    .map(getPublicIdFromUrl)
    .filter(id => id !== null);
    
  if (publicIds.length === 0) return;
  
  try {
    // Cloudinary supports bulk deletion but only via the Admin API which usually needs higher permissions.
    // For simplicity and safety, we use the Uploader API in a loop (or Promise.all)
    await Promise.all(publicIds.map(id => cloudinary.uploader.destroy(id)));
  } catch (error) {
    console.error('Failed to delete multiple images from Cloudinary:', error);
  }
};

module.exports = {
  getPublicIdFromUrl,
  deleteImageFromCloudinary,
  deleteMultipleImagesFromCloudinary
};
