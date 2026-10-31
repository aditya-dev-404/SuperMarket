export default function PageSkeleton({ rows = 3 }) {
  return (
    <div className="mx-auto max-w-5xl animate-pulse px-4 py-8" aria-hidden="true">
      <div className="h-7 w-48 rounded bg-gray-200" />
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} className="mt-6 h-28 rounded-lg bg-gray-200" />
      ))}
    </div>
  );
}