import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import ActionButton from '../components/common/ActionButton';
import Alert from '../components/common/Alert';
import AuthCard from '../components/common/AuthCard';
import PasswordField from '../components/form/PasswordField';
import TextField from '../components/form/TextField';
import { resendVerification } from '../features/auth/authApi';
import { useAuth } from '../features/auth/useAuth';

const INITIAL_FORM = { firstName: '', lastName: '', email: '', password: '' };

export default function RegisterPage() {
  const { register, status } = useAuth();

  const [form, setForm] = useState(INITIAL_FORM);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [resendNotice, setResendNotice] = useState('');
  const [resending, setResending] = useState(false);

  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }

  const handleChange = (event) =>
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    setSubmitting(true);
    try {
      await register(form);
      setRegisteredEmail(form.email);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      const data = await resendVerification(registeredEmail);
      setResendNotice(data.message);
    } catch (err) {
      setResendNotice(err.message);
    } finally {
      setResending(false);
    }
  };

  if (registeredEmail) {
    return (
      <AuthCard title="Check your inbox">
        <p className="text-sm text-gray-700">
          We sent a verification link to <span className="font-medium">{registeredEmail}</span>. Click it to
          activate your account, then log in. The link works for 24 hours.
        </p>
        <p className="text-sm text-gray-500">Nothing there? Check your spam folder or send it again.</p>
        <ActionButton variant="secondary" onClick={handleResend} disabled={resending}>
          {resending ? 'Sending...' : 'Resend verification email'}
        </ActionButton>
        <Alert tone="info" message={resendNotice} />
        <p className="text-sm text-gray-600">
          Already verified?{' '}
          <Link to="/login" className="text-indigo-600 hover:underline">
            Log in
          </Link>
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="Create your account">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <TextField
          label="First name"
          name="firstName"
          autoComplete="given-name"
          value={form.firstName}
          onChange={handleChange}
          error={fieldErrors.firstName}
          required
        />
        <TextField
          label="Last name"
          name="lastName"
          autoComplete="family-name"
          value={form.lastName}
          onChange={handleChange}
          error={fieldErrors.lastName}
          required
        />
        <TextField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={handleChange}
          error={fieldErrors.email}
          required
        />
        <PasswordField
          label="Password"
          name="password"
          autoComplete="new-password"
          value={form.password}
          onChange={handleChange}
          error={fieldErrors.password}
          required
        />
        <p className="text-xs text-gray-500">At least 8 characters, with a letter and a digit.</p>
        {!Object.keys(fieldErrors).length && <Alert message={error} />}
        <ActionButton type="submit" className="w-full" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </ActionButton>
      </form>
      <p className="text-sm text-gray-600">
        Already have an account?{' '}
        <Link to="/login" className="text-indigo-600 hover:underline">
          Log in
        </Link>
      </p>
    </AuthCard>
  );
}