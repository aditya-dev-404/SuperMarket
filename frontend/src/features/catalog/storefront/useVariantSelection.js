import { useMemo, useState } from 'react';

const SIZE_ORDER = ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', '2XL', 'XXXL', '3XL', '4XL'];

/** Known clothing sizes first (S < M < L...), then everything else naturally sorted. */
export function sortSizes(sizes) {
  const rank = (s) => {
    const idx = SIZE_ORDER.indexOf(String(s).toUpperCase());
    return idx === -1 ? SIZE_ORDER.length : idx;
  };
  return [...sizes].sort(
    (a, b) => rank(a) - rank(b) || String(a).localeCompare(String(b), undefined, { numeric: true }),
  );
}

const distinct = (variants, key) => [...new Set(variants.map((v) => v[key]).filter(Boolean))];

/**
 * Size/colour selection for the product page.
 * Remount with `key={product.id}` when the product changes (initial state comes from `variants`).
 */
export function useVariantSelection(variants) {
  const initial = variants.find((v) => v.inStock) ?? variants[0] ?? null;
  const [size, setSize] = useState(initial?.size ?? null);
  const [colour, setColour] = useState(initial?.colour ?? null);

  const sizes = useMemo(() => sortSizes(distinct(variants, 'size')), [variants]);
  const colours = useMemo(() => distinct(variants, 'colour'), [variants]);

  const selected = useMemo(
    () =>
      variants.find((v) => (v.size ?? null) === size && (v.colour ?? null) === colour) ?? null,
    [variants, size, colour],
  );

  // When the new size has no variant in the current colour, move to the best colour for it.
  const chooseSize = (nextSize) => {
    const candidates = variants.filter((v) => v.size === nextSize);
    const match =
      candidates.find((v) => v.colour === colour) ??
      candidates.find((v) => v.inStock) ??
      candidates[0];
    setSize(nextSize);
    setColour(match?.colour ?? null);
  };

  const chooseColour = (nextColour) => {
    const candidates = variants.filter((v) => v.colour === nextColour);
    const match =
      candidates.find((v) => v.size === size) ??
      candidates.find((v) => v.inStock) ??
      candidates[0];
    setColour(nextColour);
    setSize(match?.size ?? null);
  };

  const sizeHasStock = (s) => variants.some((v) => v.size === s && v.inStock);
  const colourHasStock = (c) => variants.some((v) => v.colour === c && v.inStock);

  return {
    sizes,
    colours,
    size,
    colour,
    selected,
    chooseSize,
    chooseColour,
    sizeHasStock,
    colourHasStock,
  };
}