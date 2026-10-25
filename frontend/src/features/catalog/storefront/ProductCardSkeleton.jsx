export default function ProductCardSkeleton() {
  return (
    <div className="animate-pulse" aria-hidden="true">
      <div className="aspect-square rounded-lg bg-gray-200" />
      <div className="mt-2 h-4 w-3/4 rounded bg-gray-200" />
      <div className="mt-2 h-4 w-1/3 rounded bg-gray-200" />
    </div>
  );
}