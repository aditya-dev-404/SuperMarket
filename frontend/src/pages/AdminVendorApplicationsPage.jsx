import { useEffect, useState } from 'react';
import { FiCheck, FiEye, FiX } from 'react-icons/fi';
import {
  approveApplication,
  listApplications,
  rejectApplication,
} from '../features/vendor/vendorApi';

const TABS = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
  { label: 'All', value: '' },
];

const BADGE = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
};

function Row({ label, value }) {
  return (
    <div className="flex gap-3">
      <dt className="w-32 shrink-0 text-gray-500">{label}</dt>
      <dd className="text-gray-900">{value}</dd>
    </div>
  );
}

export default function AdminVendorApplicationsPage() {
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [result, setResult] = useState({ content: [], page: { totalPages: 0 } });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  // Fetches whenever the filter, page or reloadKey changes. State is only set after the
  // request finishes; handlers below switch `loading` on before changing those inputs.
  useEffect(() => {
    let active = true;
    listApplications({ status, page })
      .then((data) => {
        if (active) {
          setResult(data);
          setError('');
        }
      })
      .catch((err) => {
        if (active) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false; // ignore a slow response that was overtaken by a newer request
    };
  }, [status, page, reloadKey]);

  const changeStatus = (value) => {
    setLoading(true);
    setStatus(value);
    setPage(0);
  };

  const changePage = (nextPage) => {
    setLoading(true);
    setPage(nextPage);
  };

  const closeModal = () => {
    setSelected(null);
    setRejecting(false);
    setReason('');
  };

  const review = async (action) => {
    setBusy(true);
    try {
      await action();
      setLoading(true);
      setReloadKey((key) => key + 1); // refetch the list
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      closeModal();
    }
  };

  const totalPages = result.page?.totalPages ?? 0;

  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Vendor applications</h1>

      <div className="mb-4 flex flex-wrap gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            onClick={() => changeStatus(tab.value)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium ${
              status === tab.value
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th className="px-4 py-3">Store</th>
              <th className="px-4 py-3">Applicant</th>
              <th className="px-4 py-3">Applied on</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-500">Loading...</td>
              </tr>
            )}
            {!loading && result.content.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-500">No applications found.</td>
              </tr>
            )}
            {!loading &&
              result.content.map((app) => (
                <tr key={app.id}>
                  <td className="px-4 py-3 font-medium text-gray-900">{app.storeName}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {app.applicant.name}
                    <span className="block text-xs text-gray-400">{app.applicant.email}</span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{new Date(app.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${BADGE[app.status]}`}>
                      {app.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => setSelected(app)}
                      className="inline-flex items-center gap-1 text-indigo-600 hover:underline"
                    >
                      <FiEye /> View
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => changePage(page - 1)} className="rounded border px-3 py-1 disabled:opacity-40">
            Prev
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => changePage(page + 1)} className="rounded border px-3 py-1 disabled:opacity-40">
            Next
          </button>
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={closeModal}>
          <div className="w-full max-w-lg rounded-xl bg-white p-6" onClick={(event) => event.stopPropagation()}>
            <h2 className="text-lg font-bold text-gray-900">{selected.storeName}</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <Row label="Applicant" value={`${selected.applicant.name} (${selected.applicant.email})`} />
              <Row label="Phone" value={selected.contactPhone} />
              <Row label="Address" value={selected.businessAddress} />
              <Row label="GSTIN" value={selected.gstNumber || '-'} />
              <Row label="Description" value={selected.storeDescription || '-'} />
              {selected.rejectionReason && <Row label="Rejection reason" value={selected.rejectionReason} />}
            </dl>

            {selected.status === 'PENDING' && !rejecting && (
              <div className="mt-6 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setRejecting(true)}
                  className="inline-flex items-center gap-1 rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
                >
                  <FiX /> Reject
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => review(() => approveApplication(selected.id))}
                  className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-60"
                >
                  <FiCheck /> Approve
                </button>
              </div>
            )}

            {rejecting && (
              <div className="mt-6">
                <textarea
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  rows={3}
                  maxLength={500}
                  placeholder="Reason for rejection (shown to the applicant)"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-red-400 focus:outline-none focus:ring-2 focus:ring-red-100"
                />
                <div className="mt-3 flex justify-end gap-3">
                  <button type="button" onClick={() => setRejecting(false)} className="rounded-lg px-4 py-2 text-sm text-gray-600 hover:bg-gray-100">
                    Back
                  </button>
                  <button
                    type="button"
                    disabled={busy || !reason.trim()}
                    onClick={() => review(() => rejectApplication(selected.id, reason))}
                    className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
                  >
                    Confirm reject
                  </button>
                </div>
              </div>
            )}

            {selected.status !== 'PENDING' && (
              <div className="mt-6 text-right">
                <button type="button" onClick={closeModal} className="rounded-lg px-4 py-2 text-sm text-gray-600 hover:bg-gray-100">
                  Close
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}