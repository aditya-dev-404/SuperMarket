import { useState } from 'react';
import { buildCategoryTree } from './categoryTree';
import { sortSizes } from './useVariantSelection';

const hasValue = (value) => value !== null && value !== undefined;

function CategoryNode({ node, selected, onSelect, depth = 0 }) {
  const isSelected = node.slug === selected;
  return (
    <li>
      <button
        type="button"
        onClick={() => onSelect(node.slug)}
        style={{ paddingLeft: `${depth * 12 + 8}px` }}
        className={`w-full rounded py-1 pr-2 text-left text-sm ${
          isSelected ? 'bg-gray-900 font-medium text-white' : 'text-gray-700 hover:bg-gray-100'
        }`}
      >
        {node.name}
      </button>
      {node.children.length > 0 && (
        <ul>
          {node.children.map((child) => (
            <CategoryNode
              key={child.slug}
              node={child}
              selected={selected}
              onSelect={onSelect}
              depth={depth + 1}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

function Chip({ active, children, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-md border px-2.5 py-1 text-sm ${
        active
          ? 'border-gray-900 bg-gray-900 text-white'
          : 'border-gray-300 text-gray-700 hover:border-gray-900'
      }`}
    >
      {children}
    </button>
  );
}

function Section({ title, children }) {
  return (
    <section className="border-b border-gray-200 py-4 first:pt-0">
      <h3 className="mb-2 text-sm font-semibold text-gray-900">{title}</h3>
      {children}
    </section>
  );
}

/** Remounted via `key` whenever the applied range changes, so initial state is always fresh. */
function PriceRange({ initialMin, initialMax, placeholderMin, placeholderMax, onApply }) {
  const [min, setMin] = useState(initialMin);
  const [max, setMax] = useState(initialMax);

  const submit = (event) => {
    event.preventDefault();
    onApply(min, max);
  };

  const inputClass =
    'w-full rounded-md border border-gray-300 px-2 py-1 text-sm focus:border-gray-900 focus:outline-none';

  return (
    <form onSubmit={submit} className="flex items-center gap-2">
      <input
        type="number"
        min="0"
        inputMode="decimal"
        value={min}
        onChange={(e) => setMin(e.target.value)}
        placeholder={placeholderMin}
        aria-label="Minimum price"
        className={inputClass}
      />
      <span className="text-gray-400">–</span>
      <input
        type="number"
        min="0"
        inputMode="decimal"
        value={max}
        onChange={(e) => setMax(e.target.value)}
        placeholder={placeholderMax}
        aria-label="Maximum price"
        className={inputClass}
      />
      <button
        type="submit"
        className="rounded-md bg-gray-900 px-3 py-1 text-sm font-medium text-white hover:bg-gray-700"
      >
        Go
      </button>
    </form>
  );
}

/**
 * filters: current applied filters (from the URL)
 * options: { categories, sizes, colours, minPrice, maxPrice } from /products/filters
 * onChange(patch): merge a patch into the URL params
 */
export default function ProductFilters({ filters, options, onChange, onClear, hasActiveFilters }) {
  const tree = buildCategoryTree(options?.categories);
  const sizes = sortSizes(options?.sizes ?? []);
  const colours = options?.colours ?? [];

  // sizes / colours / price are scoped to a category, so a category change resets them
  const selectCategory = (slug) =>
    onChange({ category: slug, size: '', colour: '', minPrice: '', maxPrice: '' });

  const toggle = (key, value) => onChange({ [key]: filters[key] === value ? '' : value });

  return (
    <div>
      {hasActiveFilters && (
        <button
          type="button"
          onClick={onClear}
          className="mb-3 text-sm font-medium text-gray-900 underline"
        >
          Clear all filters
        </button>
      )}

      <Section title="Category">
        <ul className="space-y-0.5">
          <li>
            <button
              type="button"
              onClick={() => selectCategory('')}
              className={`w-full rounded px-2 py-1 text-left text-sm ${
                !filters.category
                  ? 'bg-gray-900 font-medium text-white'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              All categories
            </button>
          </li>
          {tree.map((node) => (
            <CategoryNode
              key={node.slug}
              node={node}
              selected={filters.category}
              onSelect={selectCategory}
            />
          ))}
        </ul>
      </Section>

      <Section title="Price (₹)">
        <PriceRange
          key={`${filters.minPrice}-${filters.maxPrice}`}
          initialMin={filters.minPrice}
          initialMax={filters.maxPrice}
          placeholderMin={hasValue(options?.minPrice) ? String(Math.floor(options.minPrice)) : 'Min'}
          placeholderMax={hasValue(options?.maxPrice) ? String(Math.ceil(options.maxPrice)) : 'Max'}
          onApply={(min, max) => onChange({ minPrice: min, maxPrice: max })}
        />
      </Section>

      {sizes.length > 0 && (
        <Section title="Size">
          <div className="flex flex-wrap gap-2">
            {sizes.map((size) => (
              <Chip key={size} active={filters.size === size} onClick={() => toggle('size', size)}>
                {size}
              </Chip>
            ))}
          </div>
        </Section>
      )}

      {colours.length > 0 && (
        <Section title="Colour">
          <div className="flex flex-wrap gap-2">
            {colours.map((colour) => (
              <Chip
                key={colour}
                active={filters.colour === colour}
                onClick={() => toggle('colour', colour)}
              >
                {colour}
              </Chip>
            ))}
          </div>
        </Section>
      )}

      <Section title="Availability">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={filters.inStock}
            onChange={(e) => onChange({ inStock: e.target.checked })}
            className="h-4 w-4 rounded border-gray-300"
          />
          In stock only
        </label>
      </Section>
    </div>
  );
}