import Alert from './Alert';

/** Full-page error with a retry button, used when a page's main query fails. */
export default function PageError({ error, onRetry }) {
  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <Alert message={error?.message ?? 'Something went wrong'} />
      {onRetry && (
        <button
          type="button"
          onClick={() => onRetry()}
          className="mt-4 rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Try again
        </button>
      )}
    </div>
  );
}