import { useState } from 'react';
import Alert from '../../components/common/Alert';
import TextField from '../../components/form/TextField';
import { EMPTY_ADDRESS, toAddressRequest, validateAddress } from './addressFields';

/**
 * Add / edit address form. `onSubmit(request)` must return a promise; a rejected promise shows its
 * message and any per-field errors from the API.
 */
export default function AddressForm({
  initialValues = EMPTY_ADDRESS,
  submitLabel = 'Save address',
  onSubmit,
  onCancel,
}) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const bind = (name) => ({
    value: values[name],
    error: errors[name],
    onChange: (event) => setValues((current) => ({ ...current, [name]: event.target.value })),
  });

  const handleSubmit = async (event) => {
    event.preventDefault();
    const found = validateAddress(values);
    setErrors(found);
    setSubmitError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await onSubmit(toAddressRequest(values));
    } catch (error) {
      setErrors(error.fieldErrors ?? {});
      setSubmitError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4" aria-label="Address form">
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Full name" autoComplete="name" {...bind('fullName')} />
        <TextField
          label="Mobile number"
          type="tel"
          inputMode="numeric"
          maxLength={10}
          autoComplete="tel-national"
          {...bind('phone')}
        />
      </div>
      <TextField label="Address line 1" autoComplete="address-line1" {...bind('line1')} />
      <TextField label="Address line 2 (optional)" autoComplete="address-line2" {...bind('line2')} />
      <div className="grid gap-4 sm:grid-cols-3">
        <TextField label="City" autoComplete="address-level2" {...bind('city')} />
        <TextField label="State" autoComplete="address-level1" {...bind('state')} />
        <TextField
          label="PIN code"
          inputMode="numeric"
          maxLength={6}
          autoComplete="postal-code"
          {...bind('pincode')}
        />
      </div>

      <label className="flex items-center gap-2 text-sm text-gray-700">
        <input
          type="checkbox"
          checked={values.defaultAddress}
          onChange={(event) => setValues((current) => ({ ...current, defaultAddress: event.target.checked }))}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600"
        />
        Make this my default address
      </label>

      <Alert message={submitError} />

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {submitting ? 'Saving...' : submitLabel}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}