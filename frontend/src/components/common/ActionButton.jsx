const variants = {
  primary: 'bg-indigo-600 text-white hover:bg-indigo-700',
  secondary: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50',
};

/** Tailwind button; `variant` is "primary" (default) or "secondary". Extra props go to the button. */
export default function ActionButton({ variant = 'primary', className = '', type = 'button', ...props }) {
  return (
    <button
      type={type}
      className={`rounded-md px-4 py-2 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60 ${variants[variant]} ${className}`}
      {...props}
    />
  );
}