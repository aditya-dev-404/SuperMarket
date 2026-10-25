/**
 * Cloudinary delivery helpers. List cards should load a small square thumbnail,
 * not the full-size upload. Non-Cloudinary URLs are returned unchanged.
 */
function withTransformation(url, transformation) {
  if (!url || !url.includes('res.cloudinary.com') || !url.includes('/upload/')) {
    return url;
  }
  return url.replace('/upload/', `/upload/${transformation}/`);
}

/** Square crop, auto format/quality. */
export function thumbnail(url, size = 400) {
  return withTransformation(url, `f_auto,q_auto,c_fill,w_${size},h_${size}`);
}

/** Keep aspect ratio, limit width. */
export function fitWidth(url, width = 900) {
  return withTransformation(url, `f_auto,q_auto,c_limit,w_${width}`);
}