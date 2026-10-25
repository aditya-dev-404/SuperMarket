import { useState } from 'react';
import { FiImage } from 'react-icons/fi';
import { fitWidth, thumbnail } from './imageUrl';

/** images: [{ url, position }] already ordered by the API (first = main). */
export default function ImageGallery({ images, alt }) {
  const [active, setActive] = useState(0);
  const current = images[active] ?? images[0];

  if (!current) {
    return (
      <div className="flex aspect-square items-center justify-center rounded-lg bg-gray-100 text-gray-300">
        <FiImage className="h-16 w-16" aria-hidden="true" />
      </div>
    );
  }

  return (
    <div>
      <div className="aspect-square overflow-hidden rounded-lg bg-gray-100">
        <img src={fitWidth(current.url)} alt={alt} className="h-full w-full object-contain" />
      </div>

      {images.length > 1 && (
        <ul className="mt-3 grid grid-cols-5 gap-2 sm:grid-cols-6">
          {images.map((image, index) => (
            <li key={image.url}>
              <button
                type="button"
                onClick={() => setActive(index)}
                aria-label={`Show image ${index + 1}`}
                aria-current={index === active}
                className={`aspect-square w-full overflow-hidden rounded-md border-2 bg-gray-100 ${
                  index === active ? 'border-gray-900' : 'border-transparent hover:border-gray-300'
                }`}
              >
                <img
                  src={thumbnail(image.url, 160)}
                  alt=""
                  loading="lazy"
                  className="h-full w-full object-cover"
                />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}