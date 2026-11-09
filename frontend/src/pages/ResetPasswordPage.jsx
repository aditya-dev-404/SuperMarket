import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ActionButton from '../components/common/ActionButton';
import Alert from '../components/common/Alert';
import AuthCard from '../components/common/AuthCard';
import PasswordField from '../components/form/PasswordField';
import { resetPassword } from '../features/auth/authApi';

const linkButton =
  'inline-block rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700';

/** Opened from the link in the reset email: /reset-password?token=... */
export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');

  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleChange = (event) =>
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    if (form.newPassword !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword({ token, newPassword: form.newPassword });
      setDone(true);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  };

  if (!token) {
    return (
      <AuthCard title="Reset link incomplete">
        <Alert message="This reset link is incomplete. Please use the link from your email." />
        <Link to="/forgot-password" className={linkButton}>
          Request a new link
        </Link>
      </AuthCard>
    );
  }

  if (done) {
    return (
      <AuthCard title="Password updated">
        <p className="text-sm text-gray-700">
          Your password was changed and you were signed out everywhere. Log in with your new password.
        </p>
        <Link to="/login" className={linkButton}>
          Go to log in
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="Choose a new password">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <PasswordField
          label="New password"
          name="newPassword"
          autoComplete="new-password"
          value={form.newPassword}
          onChange={handleChange}
          error={fieldErrors.newPassword}
          required
        />
        <PasswordField
          label="Confirm new password"
          name="confirmPassword"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={handleChange}
          error={fieldErrors.confirmPassword}
          required
        />
        <p className="text-xs text-gray-500">At least 8 characters, with a letter and a digit.</p>
        {!Object.keys(fieldErrors).length && <Alert message={error} />}
        <ActionButton type="submit" className="w-full" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save new password'}
        </ActionButton>
      </form>
      {error && (
        <p className="text-sm text-gray-600">
          Link expired?{' '}
          <Link to="/forgot-password" className="text-indigo-600 hover:underline">
            Request a new one
          </Link>
        </p>
      )}
    </AuthCard>
  );
}