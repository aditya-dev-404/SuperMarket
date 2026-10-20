import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ImageManager from '../features/catalog/ImageManager';
import VariantManager from '../features/catalog/VariantManager';
import {
  changeProductStatus,
  createProduct,
  getCategories,
  getMyProduct,
  updateProduct,
} from '../features/catalog/catalogApi';
import { errorMessage, statusBadge, ui } from '../features/catalog/catalogUi';

const EMPTY = { name: '', brand: '', description: '', basePrice: '', categoryId: '' };

export default function VendorProductFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [product, setProduct] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    let ignore = false;
    (async () => {
      try {
        const cats = await getCategories();
        if (ignore) return;
        setCategories(cats);
        if (isEdit) {
          const p = await getMyProduct(id);
          if (ignore) return;
          setProduct(p);
          setForm({
            name: p.name,
            brand: p.brand ?? '',
            description: p.description ?? '',
            basePrice: String(p.basePrice),
            categoryId: String(p.categoryId),
          });
        }
      } catch (err) {
        if (!ignore) setError(errorMessage(err));
      } finally {
        if (!ignore) setLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [id, isEdit]);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setSaving(true);
    const body = {
      name: form.name.trim(),
      brand: form.brand.trim(),
      description: form.description.trim(),
      basePrice: Number(form.basePrice),
      categoryId: Number(form.categoryId),
    };
    try {
      if (isEdit) {
        setProduct(await updateProduct(id, body));
        setMessage('Product saved');
      } else {
        const created = await createProduct(body);
        // go straight to edit mode so variants and images can be added
        navigate(`/vendor/products/${created.id}/edit`, { replace: true });
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const setStatus = async (status) => {
    setError('');
    setMessage('');
    try {
      setProduct(await changeProductStatus(id, status));
      setMessage(`Status changed to ${status}`);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (loading) {
    return <p className="p-8 text-sm text-gray-500">Loading...</p>;
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to="/vendor/products" className="text-sm text-indigo-600 hover:underline">
            ← Back to products
          </Link>
          <h1 className="mt-1 text-2xl font-bold text-gray-900">{isEdit ? 'Edit product' : 'New product'}</h1>
        </div>

        {product && (
          <div className="flex items-center gap-2">
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusBadge[product.status]}`}>
              {product.status}
            </span>
            {product.status !== 'ACTIVE' && (
              <button className={ui.btn} onClick={() => setStatus('ACTIVE')}>
                Publish
              </button>
            )}
            {product.status === 'ACTIVE' && (
              <button className={ui.btnGhost} onClick={() => setStatus('DRAFT')}>
                Move to draft
              </button>
            )}
            {product.status !== 'ARCHIVED' && (
              <button className={ui.btnGhost} onClick={() => setStatus('ARCHIVED')}>
                Archive
              </button>
            )}
          </div>
        )}
      </div>

      {error && <div className={ui.error}>{error}</div>}
      {message && <div className={ui.success}>{message}</div>}

      <form onSubmit={submit} className={`${ui.card} grid gap-4 md:grid-cols-2`}>
        <div className="md:col-span-2">
          <label className={ui.label}>Name *</label>
          <input className={ui.input} value={form.name} onChange={set('name')} required maxLength={200} />
        </div>
        <div>
          <label className={ui.label}>Category *</label>
          <select className={ui.input} value={form.categoryId} onChange={set('categoryId')} required>
            <option value="">Select a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className={ui.label}>Brand</label>
          <input className={ui.input} value={form.brand} onChange={set('brand')} maxLength={100} />
        </div>
        <div>
          <label className={ui.label}>Base price (₹) *</label>
          <input
            className={ui.input}
            type="number"
            min="0.01"
            step="0.01"
            value={form.basePrice}
            onChange={set('basePrice')}
            required
          />
        </div>
        <div className="md:col-span-2">
          <label className={ui.label}>Description</label>
          <textarea
            className={ui.input}
            rows={5}
            value={form.description}
            onChange={set('description')}
            maxLength={5000}
          />
        </div>
        <div className="md:col-span-2">
          <button type="submit" className={ui.btn} disabled={saving}>
            {isEdit ? 'Save changes' : 'Create and continue'}
          </button>
          {!isEdit && (
            <p className="mt-2 text-sm text-gray-500">
              After creating, you can add variants and images, then publish.
            </p>
          )}
        </div>
      </form>

      {product && (
        <>
          <VariantManager product={product} onChange={setProduct} />
          <ImageManager product={product} onChange={setProduct} />
        </>
      )}
    </div>
  );
}