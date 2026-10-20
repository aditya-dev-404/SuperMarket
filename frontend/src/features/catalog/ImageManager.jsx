import { useState } from 'react';
import { FiTrash2, FiUploadCloud } from 'react-icons/fi';
import { addImage, deleteImage } from './catalogApi';
import { errorMessage, ui } from './catalogUi';
import { uploadToCloudinary } from './uploadImage';

const MAX_IMAGES = 8;

export default function ImageManager({ product, onChange }) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleFile = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // allow choosing the same file again
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const { url, publicId } = await uploadToCloudinary(file);
      onChange(await addImage(product.id, { url, publicId }));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const remove = async (image) => {
    setError('');
    try {
      onChange(await deleteImage(product.id, image.id));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const full = product.images.length >= MAX_IMAGES;

  return (
    <section className={ui.card}>
      <h2 className="text-lg font-semibold text-gray-900">Images</h2>
      <p className="mb-4 text-sm text-gray-500">
        First image is the main one. Up to {MAX_IMAGES} images, 5 MB each.
      </p>

      {error && <div className={`${ui.error} mb-3`}>{error}</div>}

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {product.images.map((img, index) => (
          <div key={img.id} className="group relative overflow-hidden rounded-md border">
            <img src={img.url} alt="" className="h-32 w-full object-cover" />
            {index === 0 && (
              <span className="absolute left-1 top-1 rounded bg-indigo-600 px-1.5 py-0.5 text-xs text-white">
                Main
              </span>
            )}
            <button
              type="button"
              onClick={() => remove(img)}
              className="absolute right-1 top-1 rounded bg-white/90 p-1.5 text-red-600 shadow hover:bg-white"
              aria-label="Delete image"
            >
              <FiTrash2 />
            </button>
          </div>
        ))}
      </div>

      <label
        className={`${ui.btnGhost} cursor-pointer ${full || uploading ? 'pointer-events-none opacity-50' : ''}`}
      >
        <FiUploadCloud />
        {uploading ? 'Uploading...' : full ? 'Image limit reached' : 'Upload image'}
        <input type="file" accept="image/*" className="hidden" onChange={handleFile} disabled={full || uploading} />
      </label>
    </section>
  );
}