function OptionGroup({ label, value, options, hasStock, onChoose }) {
  if (options.length === 0) return null;

  return (
    <div>
      <p className="text-sm font-medium text-gray-900">
        {label}: <span className="font-normal text-gray-600">{value}</span>
      </p>
      <div className="mt-2 flex flex-wrap gap-2">
        {options.map((option) => {
          const selected = option === value;
          const available = hasStock(option);
          return (
            <button
              key={option}
              type="button"
              onClick={() => onChoose(option)}
              aria-pressed={selected}
              title={available ? undefined : 'Out of stock'}
              className={`rounded-md border px-3 py-1.5 text-sm font-medium transition ${
                selected
                  ? 'border-gray-900 bg-gray-900 text-white'
                  : available
                    ? 'border-gray-300 text-gray-800 hover:border-gray-900'
                    : 'border-gray-200 text-gray-400 line-through hover:border-gray-300'
              }`}
            >
              {option}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/** `selection` is the object returned by useVariantSelection(). */
export default function VariantSelector({ selection }) {
  const { sizes, colours, size, colour, chooseSize, chooseColour, sizeHasStock, colourHasStock } =
    selection;

  return (
    <div className="space-y-4">
      <OptionGroup
        label="Colour"
        value={colour}
        options={colours}
        hasStock={colourHasStock}
        onChoose={chooseColour}
      />
      <OptionGroup
        label="Size"
        value={size}
        options={sizes}
        hasStock={sizeHasStock}
        onChoose={chooseSize}
      />
    </div>
  );
}