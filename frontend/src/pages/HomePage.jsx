import Loader from '../components/common/Loader';
import ErrorMessage from '../components/common/ErrorMessage';
import { usePing } from '../hooks/usePing';

export default function HomePage() {
  const { data, isPending, isError, error } = usePing();

  return (
    <section>
      <h1>Welcome to SuperMarket</h1>
      <p className="muted">Phase 0 foundation: the storefront arrives in later phases.</p>

      <h2>Backend status</h2>
      {isPending && <Loader label="Checking backend..." />}
      {isError && <ErrorMessage message={`Backend not reachable: ${error.message}`} />}
      {data && (
        <p>
          Backend is <strong>{data.status}</strong> ({data.application})
        </p>
      )}
    </section>
  );
}
