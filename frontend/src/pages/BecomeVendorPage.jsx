import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { FiCheckCircle, FiClock, FiShoppingBag, FiXCircle } from 'react-icons/fi';
import Loader from '../components/common/Loader';
import * as authApi from '../features/auth/authApi';
import { useAuth } from '../features/auth/useAuth';
import {
  applyForVendor,
  getMyApplication,
  resubmitApplication,
} from '../features/vendor/vendorApi';

const EMPTY_FORM = {
  storeName: '',
  storeDescription: '',
  contactPhone: '',
  businessAddress: '',
  gstNumber: '',
};

const inputClass =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200';

function Field({ label, optional, error, children }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-gray-700">
        {label} {optional && <span className="font-normal text-gray-400">(optional)</span>}
      </span>
      {children}
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  );
}

export default function BecomeVendorPage() {
  const { user, setUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [application, setApplication] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [activating, setActivating] = useState(false);

  useEffect(() => {
    if (user.role === 'VENDOR') {
      return;
    }
    getMyApplication()
      .then((app) => {
        setApplication(app);
        setForm({
          storeName: app.storeName,
          storeDescription: app.storeDescription ?? '',
          contactPhone: app.contactPhone,
          businessAddress: app.businessAddress,
          gstNumber: app.gstNumber ?? '',
        });
      })
      .catch((err) => {
        if (err.status !== 404) {
          setError(err.message);
        }
      })
      .finally(() => setLoading(false));
  }, [user.role]);

  if (user.role === 'VENDOR') {
    return <Navigate to="/vendor/dashboard" replace />;
  }
  if (loading) {
    return <Loader label="Loading your application..." />;
  }

  const handleChange = (event) =>
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    setSubmitting(true);
    try {
      const saved = application ? await resubmitApplication(form) : await applyForVendor(form);
      setApplication(saved);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  };

  // After approval the role in the current access token is stale; a refresh issues a fresh one.
  const handleActivate = async () => {
    setError('');
    setActivating(true);
    try {
      const data = await authApi.refreshSession();
      setUser(data.user);
      if (data.user.role !== 'VENDOR') {
        setError('Please log out and log in again to activate your vendor access.');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setActivating(false);
    }
  };

  const status = application?.status;
  const showForm = !application || status === 'REJECTED';

  return (
    <section className="mx-auto max-w-2xl px-4 py-10">
      <div className="mb-6 flex items-center gap-3">
        <FiShoppingBag className="text-3xl text-indigo-600" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Become a vendor</h1>
          <p className="text-sm text-gray-500">Apply to sell your products on SuperMarket.</p>
        </div>
      </div>

      {status === 'PENDING' && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-5">
          <p className="flex items-center gap-2 font-semibold text-amber-800">
            <FiClock /> Application under review
          </p>
          <p className="mt-1 text-sm text-amber-700">
            <b>{application.storeName}</b> is waiting for admin approval.
          </p>
        </div>
      )}

      {status === 'APPROVED' && (
        <div className="rounded-xl border border-green-200 bg-green-50 p-5">
          <p className="flex items-center gap-2 font-semibold text-green-800">
            <FiCheckCircle /> Application approved
          </p>
          <p className="mt-1 text-sm text-green-700">
            <b>{application.storeName}</b> is approved. Activate your vendor access to continue.
          </p>
          <button
            type="button"
            onClick={handleActivate}
            disabled={activating}
            className="mt-3 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-60"
          >
            {activating ? 'Activating...' : 'Activate vendor access'}
          </button>
        </div>
      )}

      {status === 'REJECTED' && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-5">
          <p className="flex items-center gap-2 font-semibold text-red-800">
            <FiXCircle /> Application rejected
          </p>
          <p className="mt-1 text-sm text-red-700">Reason: {application.rejectionReason}</p>
          <p className="mt-1 text-sm text-red-700">Update your details below and resubmit.</p>
        </div>
      )}

      {showForm && (
        <form
          onSubmit={handleSubmit}
          noValidate
          className="space-y-4 rounded-xl border border-gray-200 bg-white p-6"
        >
          <Field label="Store name" error={fieldErrors.storeName}>
            <input name="storeName" value={form.storeName} onChange={handleChange} maxLength={100} className={inputClass} />
          </Field>
          <Field label="Store description" optional error={fieldErrors.storeDescription}>
            <textarea name="storeDescription" value={form.storeDescription} onChange={handleChange} rows={3} maxLength={1000} className={inputClass} />
          </Field>
          <Field label="Contact phone" error={fieldErrors.contactPhone}>
            <input name="contactPhone" type="tel" value={form.contactPhone} onChange={handleChange} placeholder="9876543210" className={inputClass} />
          </Field>
          <Field label="Business address" error={fieldErrors.businessAddress}>
            <textarea name="businessAddress" value={form.businessAddress} onChange={handleChange} rows={2} maxLength={500} className={inputClass} />
          </Field>
          <Field label="GSTIN" optional error={fieldErrors.gstNumber}>
            <input name="gstNumber" value={form.gstNumber} onChange={handleChange} maxLength={15} placeholder="22AAAAA0000A1Z5" className={`${inputClass} uppercase`} />
          </Field>

          {error && !Object.keys(fieldErrors).length && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            {submitting ? 'Submitting...' : application ? 'Resubmit application' : 'Submit application'}
          </button>
        </form>
      )}

      {!showForm && error && (
        <p role="alert" className="mt-4 text-sm text-red-700">
          {error}
        </p>
      )}
    </section>
  );
}