import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import ActionButton from '../components/common/ActionButton';
import Alert from '../components/common/Alert';
import AuthCard from '../components/common/AuthCard';
import PasswordField from '../components/form/PasswordField';
import TextField from '../components/form/TextField';
import { resendVerification } from '../features/auth/authApi';
import { useAuth } from '../features/auth/useAuth';

const EMAIL_NOT_VERIFIED = 'EMAIL_NOT_VERIFIED';

export default function LoginPage() {
  const { login, status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname ?? '/';

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [needsVerification, setNeedsVerification] = useState(false);
  const [resendNotice, setResendNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);

  if (status === 'authenticated') {
    return <Navigate to={redirectTo} replace />;
  }

  const handleChange = (event) =>
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setNeedsVerification(false);
    setResendNotice('');
    setSubmitting(true);
    try {
      await login(form);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err.message);
      setNeedsVerification(err.code === EMAIL_NOT_VERIFIED);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      const data = await resendVerification(form.email);
      setResendNotice(data.message);
    } catch (err) {
      setResendNotice(err.message);
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthCard title="Log in">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <TextField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={handleChange}
          required
        />
        <PasswordField
          label="Password"
          name="password"
          autoComplete="current-password"
          value={form.password}
          onChange={handleChange}
          required
        />
        <p className="text-right text-sm">
          <Link to="/forgot-password" className="text-indigo-600 hover:underline">
            Forgot password?
          </Link>
        </p>
        <Alert message={error} />
        {needsVerification && (
          <div className="space-y-2">
            <ActionButton variant="secondary" onClick={handleResend} disabled={resending}>
              {resending ? 'Sending...' : 'Resend verification email'}
            </ActionButton>
            <Alert tone="info" message={resendNotice} />
          </div>
        )}
        <ActionButton type="submit" className="w-full" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </ActionButton>
      </form>
      <p className="text-sm text-gray-600">
        New here?{' '}
        <Link to="/register" className="text-indigo-600 hover:underline">
          Create an account
        </Link>
      </p>
    </AuthCard>
  );
}