import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import ErrorMessage from '../components/common/ErrorMessage';
import FormField from '../components/form/FormField';
import { useAuth } from '../features/auth/useAuth';

const INITIAL_FORM = { firstName: '', lastName: '', email: '', password: '' };

export default function RegisterPage() {
  const { register, status } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL_FORM);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

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
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card card--narrow">
      <h1>Create your account</h1>
      <form onSubmit={handleSubmit} noValidate>
        <FormField
          label="First name"
          name="firstName"
          autoComplete="given-name"
          value={form.firstName}
          onChange={handleChange}
          error={fieldErrors.firstName}
          required
        />
        <FormField
          label="Last name"
          name="lastName"
          autoComplete="family-name"
          value={form.lastName}
          onChange={handleChange}
          error={fieldErrors.lastName}
          required
        />
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={handleChange}
          error={fieldErrors.email}
          required
        />
        <FormField
          label="Password"
          name="password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={handleChange}
          error={fieldErrors.password}
          required
        />
        <p className="muted">At least 8 characters, with a letter and a digit.</p>
        {error && !Object.keys(fieldErrors).length && <ErrorMessage message={error} />}
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </button>
      </form>
      <p className="muted">
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </section>
  );
}
