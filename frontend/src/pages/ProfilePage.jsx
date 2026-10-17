import { useState } from 'react';
import ErrorMessage from '../components/common/ErrorMessage';
import FormField from '../components/form/FormField';
import * as authApi from '../features/auth/authApi';
import { useAuth } from '../features/auth/useAuth';

function ProfileForm() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({
    firstName: user.firstName,
    lastName: user.lastName,
    phone: user.phone ?? '',
  });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [saved, setSaved] = useState(false);

  const handleChange = (event) => {
    setSaved(false);
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    try {
      const updated = await authApi.updateProfile({ ...form, phone: form.phone.trim() || null });
      setUser(updated);
      setSaved(true);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h2>Profile</h2>
      <p className="muted">
        {user.email} &middot; {user.role}
      </p>
      <FormField
        label="First name"
        name="firstName"
        value={form.firstName}
        onChange={handleChange}
        error={fieldErrors.firstName}
        required
      />
      <FormField
        label="Last name"
        name="lastName"
        value={form.lastName}
        onChange={handleChange}
        error={fieldErrors.lastName}
        required
      />
      <FormField
        label="Phone"
        name="phone"
        type="tel"
        value={form.phone}
        onChange={handleChange}
        error={fieldErrors.phone}
      />
      {error && !Object.keys(fieldErrors).length && <ErrorMessage message={error} />}
      {saved && <p role="status">Profile saved.</p>}
      <button type="submit" className="button">
        Save changes
      </button>
    </form>
  );
}

function ChangePasswordForm() {
  const { endSession } = useAuth();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const handleChange = (event) =>
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    try {
      await authApi.changePassword(form);
      endSession(); // the backend revoked all sessions, so sign in again
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h2>Change password</h2>
      <p className="muted">You will be signed out on all devices afterwards.</p>
      <FormField
        label="Current password"
        name="currentPassword"
        type="password"
        autoComplete="current-password"
        value={form.currentPassword}
        onChange={handleChange}
        error={fieldErrors.currentPassword}
        required
      />
      <FormField
        label="New password"
        name="newPassword"
        type="password"
        autoComplete="new-password"
        value={form.newPassword}
        onChange={handleChange}
        error={fieldErrors.newPassword}
        required
      />
      {error && !Object.keys(fieldErrors).length && <ErrorMessage message={error} />}
      <button type="submit" className="button">
        Change password
      </button>
    </form>
  );
}

export default function ProfilePage() {
  return (
    <section className="card card--narrow">
      <h1>My account</h1>
      <ProfileForm />
      <hr />
      <ChangePasswordForm />
    </section>
  );
}
