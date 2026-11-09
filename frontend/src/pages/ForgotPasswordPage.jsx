import { useState } from 'react';
import { Link } from 'react-router-dom';
import ActionButton from '../components/common/ActionButton';
import Alert from '../components/common/Alert';
import AuthCard from '../components/common/AuthCard';
import TextField from '../components/form/TextField';
import { forgotPassword } from '../features/auth/authApi';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const data = await forgotPassword(email);
      setNotice(data.message);
    } catch (err) {
      setError(err.fieldErrors?.email ?? err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard title="Forgot your password?">
      {notice ? (
        <Alert tone="info" message={notice} />
      ) : (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <p className="text-sm text-gray-600">
            Enter your email and we will send you a link to choose a new password.
          </p>
          <TextField
            label="Email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
          <Alert message={error} />
          <ActionButton type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Sending...' : 'Send reset link'}
          </ActionButton>
        </form>
      )}
      <p className="text-sm text-gray-600">
        <Link to="/login" className="text-indigo-600 hover:underline">
          Back to log in
        </Link>
      </p>
    </AuthCard>
  );
}