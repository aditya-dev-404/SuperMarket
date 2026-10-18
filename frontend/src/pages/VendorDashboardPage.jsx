import { useEffect, useState } from 'react';
import { FiPackage } from 'react-icons/fi';
import { getMyApplication } from '../features/vendor/vendorApi';

export default function VendorDashboardPage() {
  const [storeName, setStoreName] = useState('');

  useEffect(() => {
    getMyApplication()
      .then((app) => setStoreName(app.storeName))
      .catch(() => {});
  }, []);

  return (
    <section className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900">{storeName || 'Vendor dashboard'}</h1>
      <div className="mt-6 flex items-center gap-3 rounded-xl border border-dashed border-gray-300 p-8 text-gray-500">
        <FiPackage className="text-2xl" />
        <p>Product and catalog management arrives in Phase 3.</p>
      </div>
    </section>
  );
}