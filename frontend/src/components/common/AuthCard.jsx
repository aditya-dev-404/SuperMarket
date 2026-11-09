/** Centered card used by every sign-in / account-recovery page. */
export default function AuthCard({ title, children }) {
  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">{title}</h1>
        <div className="mt-4 space-y-4">{children}</div>
      </div>
    </section>
  );
}