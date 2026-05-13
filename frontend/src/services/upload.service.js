import { api } from './api';

export const uploadService = {
  /**
   * Uploads an array of files to the backend.
   * @param {File[]} files - Array of File objects
   * @returns {Promise<string[]>} - Promise resolving to an array of image URLs
   */
  uploadImages: async (files) => {
    if (!files || files.length === 0) return [];

    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
      formData.append('images', files[i]);
    }

    const response = await api.post('/upload', formData);
    return response.data.imageUrls;
  },
};
