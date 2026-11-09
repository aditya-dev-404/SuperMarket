import { useId, useState } from 'react';
import { FiEye, FiEyeOff } from 'react-icons/fi';

/** Tailwind password input with a show/hide toggle; extra props go to the input. */
export default function PasswordField({ label, error, className = '', ...inputProps }) {
  const id = useId();
  const errorId = `${id}-error`;
  const [revealed, setRevealed] = useState(false);

  return (
    <div className={className}>
      <label htmlFor={id} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      <div className="relative mt-1">
        <input
          id={id}
          type={revealed ? 'text' : 'password'}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? errorId : undefined}
          className={`block w-full rounded-md border px-3 py-2 pr-10 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
            error ? 'border-red-400' : 'border-gray-300'
          }`}
          {...inputProps}
        />
        <button
          type="button"
          onClick={() => setRevealed((current) => !current)}
          aria-label={revealed ? 'Hide password' : 'Show password'}
          aria-pressed={revealed}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
        >
          {revealed ? <FiEyeOff aria-hidden="true" /> : <FiEye aria-hidden="true" />}
        </button>
      </div>
      {error && (
        <p id={errorId} className="mt-1 text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}