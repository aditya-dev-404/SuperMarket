import { useId, useState } from 'react';
import { FiEye, FiEyeOff } from 'react-icons/fi';

/** Label + input + inline error, wired together for accessibility. Password fields get a show/hide toggle. */
export default function FormField({ label, error, type = 'text', className = '', ...inputProps }) {
  const id = useId();
  const errorId = `${id}-error`;
  const [revealed, setRevealed] = useState(false);

  const isPassword = type === 'password';
  const inputType = isPassword && revealed ? 'text' : type;
  // The `!` utilities are important on purpose: they must win over the base `.field input` rules so the
  // input fills exactly the wrapper (the icon is positioned against it) and the text never runs under the icon.
  const inputClassName = isPassword
    ? `${className} box-border! m-0! block w-full! pr-10!`.trim()
    : className || undefined;

  const input = (
    <input
      id={id}
      type={inputType}
      aria-invalid={error ? 'true' : undefined}
      aria-describedby={error ? errorId : undefined}
      className={inputClassName}
      {...inputProps}
    />
  );

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {isPassword ? (
        <div className="relative w-full">
          {input}
          <button
            type="button"
            onClick={() => setRevealed((current) => !current)}
            aria-label={revealed ? 'Hide password' : 'Show password'}
            aria-pressed={revealed}
            className="absolute right-0 top-1/2 flex h-full w-10 -translate-y-1/2 items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-gray-900"
          >
            {revealed ? <FiEyeOff aria-hidden="true" /> : <FiEye aria-hidden="true" />}
          </button>
        </div>
      ) : (
        input
      )}
      {error && (
        <p id={errorId} className="field__error">
          {error}
        </p>
      )}
    </div>
  );
}