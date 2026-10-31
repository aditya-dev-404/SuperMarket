import { useState } from 'react';
import Alert from '../components/common/Alert';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import AddressForm from '../features/address/AddressForm';
import AddressSummary from '../features/address/AddressSummary';
import { toFormValues } from '../features/address/addressFields';
import {
  useAddresses,
  useCreateAddress,
  useDeleteAddress,
  useSetDefaultAddress,
  useUpdateAddress,
} from '../features/address/useAddresses';

const linkButton = 'text-sm font-medium text-indigo-600 hover:underline disabled:opacity-40';

export default function AddressesPage() {
  const { data: addresses, isPending, isError, error, refetch } = useAddresses();
  const create = useCreateAddress();
  const update = useUpdateAddress();
  const makeDefault = useSetDefaultAddress();
  const remove = useDeleteAddress();
  // null = form closed, 'new' = adding, otherwise the id of the address being edited
  const [editing, setEditing] = useState(null);

  if (isPending) return <PageSkeleton />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  const busy = makeDefault.isPending || remove.isPending;
  const actionError = makeDefault.error ?? remove.error;
  const editedAddress = addresses.find((address) => address.id === editing);

  const handleDelete = (address) => {
    if (window.confirm(`Delete the address for ${address.fullName}?`)) remove.mutate(address.id);
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">My addresses</h1>
        {editing === null && (
          <button
            type="button"
            onClick={() => setEditing('new')}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            Add address
          </button>
        )}
      </div>

      <Alert message={actionError?.message} className="mt-4" />

      {editing === 'new' && (
        <section className="mt-6 rounded-lg border border-gray-200 p-4">
          <h2 className="mb-4 text-sm font-semibold text-gray-900">New address</h2>
          <AddressForm
            onSubmit={async (request) => {
              await create.mutateAsync(request);
              setEditing(null);
            }}
            onCancel={() => setEditing(null)}
          />
        </section>
      )}

      {editedAddress && (
        <section className="mt-6 rounded-lg border border-gray-200 p-4">
          <h2 className="mb-4 text-sm font-semibold text-gray-900">Edit address</h2>
          <AddressForm
            key={editedAddress.id}
            initialValues={toFormValues(editedAddress)}
            submitLabel="Save changes"
            onSubmit={async (request) => {
              await update.mutateAsync({ id: editedAddress.id, ...request });
              setEditing(null);
            }}
            onCancel={() => setEditing(null)}
          />
        </section>
      )}

      {addresses.length === 0 && editing !== 'new' ? (
        <div className="mt-10 text-center">
          <p className="text-sm text-gray-500">You have not saved any address yet.</p>
        </div>
      ) : (
        <ul className="mt-6 space-y-4">
          {addresses.map((address) => (
            <li key={address.id} className="rounded-lg border border-gray-200 p-4">
              <div className="flex items-start justify-between gap-4">
                <AddressSummary address={address} />
                {address.defaultAddress && (
                  <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-medium text-indigo-700">
                    Default
                  </span>
                )}
              </div>
              <div className="mt-3 flex gap-4">
                <button type="button" className={linkButton} onClick={() => setEditing(address.id)}>
                  Edit
                </button>
                {!address.defaultAddress && (
                  <button
                    type="button"
                    className={linkButton}
                    disabled={busy}
                    onClick={() => makeDefault.mutate(address.id)}
                  >
                    Make default
                  </button>
                )}
                <button
                  type="button"
                  className="text-sm font-medium text-red-600 hover:underline disabled:opacity-40"
                  disabled={busy}
                  onClick={() => handleDelete(address)}
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}