import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Alert from '../components/common/Alert';
import AuthCard from '../components/common/AuthCard';
import { verifyEmail } from '../features/auth/authApi';

const MISSING_TOKEN_MESSAGE = 'This verification link is incomplete. Please use the link from your email.';
const linkButton =
  'inline-block rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700';

/** Opened from the link in the verification email: /verify-email?token=... */
export default function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [state, setState] = useState(token ? 'verifying' : 'error');
  const [message, setMessage] = useState('');
  // A token works only once and React StrictMode runs effects twice in development: guard the call.
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    verifyEmail(token)
      .then(() => setState('success'))
      .catch((err) => {
        setMessage(err.message);
        setState('error');
      });
  }, [token]);

  if (state === 'verifying') {
    return (
      <AuthCard title="Verifying your email">
        <p role="status" className="text-sm text-gray-600">
          Please wait...
        </p>
      </AuthCard>
    );
  }

  if (state === 'success') {
    return (
      <AuthCard title="Email verified">
        <p className="text-sm text-gray-700">Your account is ready. You can log in now.</p>
        <Link to="/login" className={linkButton}>
          Go to log in
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="Verification failed">
      <Alert message={token ? message : MISSING_TOKEN_MESSAGE} />
      <p className="text-sm text-gray-600">
        Log in with your email and password to get a new verification link.
      </p>
      <Link to="/login" className={linkButton}>
        Go to log in
      </Link>
    </AuthCard>
  );
}