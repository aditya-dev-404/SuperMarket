import { getUploadSignature } from './catalogApi';

const MAX_BYTES = 5 * 1024 * 1024;

/** Signed direct upload to Cloudinary. Returns { url, publicId }. */
export async function uploadToCloudinary(file) {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose an image file');
  }
  if (file.size > MAX_BYTES) {
    throw new Error('Image must be 5 MB or smaller');
  }

  const sig = await getUploadSignature();
  const form = new FormData();
  form.append('file', file);
  form.append('api_key', sig.apiKey);
  form.append('timestamp', sig.timestamp);
  form.append('folder', sig.folder);
  form.append('signature', sig.signature);

  // plain fetch on purpose: our JWT must not be sent to Cloudinary
  const res = await fetch(`https://api.cloudinary.com/v1_1/${sig.cloudName}/image/upload`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) {
    throw new Error('Image upload failed');
  }
  const json = await res.json();
  return { url: json.secure_url, publicId: json.public_id };
}