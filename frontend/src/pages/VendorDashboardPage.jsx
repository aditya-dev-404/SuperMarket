import { useEffect, useState } from 'react';
import { FiClipboard, FiPackage, FiPlusCircle } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { getMyApplication } from '../features/vendor/vendorApi';

const actions = [
  {
    to: '/vendor/orders',
    icon: FiClipboard,
    title: 'Orders',
    text: 'See orders for your products and update their shipping status.',
  },
  {
    to: '/vendor/products',
    icon: FiPackage,
    title: 'My products',
    text: 'View, edit, publish or archive your products.',
  },
  {
    to: '/vendor/products/new',
    icon: FiPlusCircle,
    title: 'Add a product',
    text: 'Create a new product, then add variants and images.',
  },
];

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
      <p className="mt-1 text-sm text-gray-500">Manage your catalog and orders from here.</p>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {actions.map(({ to, icon: Icon, title, text }) => (
          <Link
            key={to}
            to={to}
            className="flex items-start gap-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-indigo-300 hover:shadow"
          >
            <Icon className="mt-1 text-2xl text-indigo-600" />
            <div>
              <h2 className="font-semibold text-gray-900">{title}</h2>
              <p className="mt-1 text-sm text-gray-500">{text}</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}