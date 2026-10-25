import ErrorMessage from '../components/common/ErrorMessage';
import Loader from '../components/common/Loader';
import HomeCategories from '../features/catalog/storefront/HomeCategories';
import LatestProducts from '../features/catalog/storefront/LatestProducts';
import { usePing } from '../hooks/usePing';

export default function HomePage() {
  const { data, isPending, isError, error } = usePing();

  return (
    <div className="space-y-10">
      <section>
        <h1>Welcome to SuperMarket</h1>
        <p className="muted">Browse products from our vendors.</p>
      </section>

      <HomeCategories />
      <LatestProducts />

      <section>
        <h2>Backend status</h2>
        {isPending && <Loader label="Checking backend..." />}
        {isError && <ErrorMessage message={`Backend not reachable: ${error.message}`} />}
        {data && (
          <p>
            Backend is <strong>{data.status}</strong> ({data.application})
          </p>
        )}
      </section>
    </div>
  );
}