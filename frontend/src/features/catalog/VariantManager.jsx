import { useState } from 'react';
import { FiEdit2, FiPlus, FiTrash2 } from 'react-icons/fi';
import { addVariant, deleteVariant, updateVariant } from './catalogApi';
import { errorMessage, ui } from './catalogUi';

const EMPTY = { sku: '', size: '', colour: '', price: '', stockQuantity: '0', active: true };

export default function VariantManager({ product, onChange }) {
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) =>
    setForm((f) => ({ ...f, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const reset = () => {
    setForm(EMPTY);
    setEditingId(null);
  };

  const startEdit = (v) => {
    setEditingId(v.id);
    setForm({
      sku: v.sku,
      size: v.size ?? '',
      colour: v.colour ?? '',
      price: v.price ?? '',
      stockQuantity: String(v.stockQuantity),
      active: v.active,
    });
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    const body = {
      sku: form.sku.trim(),
      size: form.size.trim(),
      colour: form.colour.trim(),
      price: form.price === '' ? null : Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      active: form.active,
    };
    try {
      const updated = editingId
        ? await updateVariant(product.id, editingId, body)
        : await addVariant(product.id, body);
      onChange(updated);
      reset();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (v) => {
    if (!window.confirm(`Delete variant ${v.sku}?`)) return;
    setError('');
    try {
      onChange(await deleteVariant(product.id, v.id));
      if (editingId === v.id) reset();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <section className={ui.card}>
      <h2 className="text-lg font-semibold text-gray-900">Variants</h2>
      <p className="mb-4 text-sm text-gray-500">
        Size and colour are optional. Leave price empty to use the product price.
      </p>

      {product.variants.length > 0 && (
        <div className="mb-5 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b text-gray-500">
              <tr>
                <th className="py-2 pr-4">SKU</th>
                <th className="py-2 pr-4">Size</th>
                <th className="py-2 pr-4">Colour</th>
                <th className="py-2 pr-4">Price</th>
                <th className="py-2 pr-4">Stock</th>
                <th className="py-2 pr-4">Active</th>
                <th className="py-2" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {product.variants.map((v) => (
                <tr key={v.id}>
                  <td className="py-2 pr-4 font-mono">{v.sku}</td>
                  <td className="py-2 pr-4">{v.size ?? '-'}</td>
                  <td className="py-2 pr-4">{v.colour ?? '-'}</td>
                  <td className="py-2 pr-4">₹{v.price ?? product.basePrice}</td>
                  <td className="py-2 pr-4">{v.stockQuantity}</td>
                  <td className="py-2 pr-4">{v.active ? 'Yes' : 'No'}</td>
                  <td className="py-2 text-right">
                    <button type="button" className={ui.iconBtn} onClick={() => startEdit(v)} aria-label="Edit variant">
                      <FiEdit2 />
                    </button>
                    <button type="button" className={ui.iconBtnDanger} onClick={() => remove(v)} aria-label="Delete variant">
                      <FiTrash2 />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {error && <div className={`${ui.error} mb-3`}>{error}</div>}

      <form onSubmit={submit} className="grid grid-cols-2 gap-3 md:grid-cols-6">
        <div className="col-span-2">
          <label className={ui.label}>SKU *</label>
          <input className={ui.input} value={form.sku} onChange={set('sku')} required maxLength={64} />
        </div>
        <div>
          <label className={ui.label}>Size</label>
          <input className={ui.input} value={form.size} onChange={set('size')} maxLength={30} />
        </div>
        <div>
          <label className={ui.label}>Colour</label>
          <input className={ui.input} value={form.colour} onChange={set('colour')} maxLength={30} />
        </div>
        <div>
          <label className={ui.label}>Price (₹)</label>
          <input className={ui.input} type="number" min="0.01" step="0.01" value={form.price} onChange={set('price')} />
        </div>
        <div>
          <label className={ui.label}>Stock *</label>
          <input className={ui.input} type="number" min="0" step="1" value={form.stockQuantity} onChange={set('stockQuantity')} required />
        </div>
        <label className="col-span-2 flex items-center gap-2 text-sm text-gray-700 md:col-span-3">
          <input type="checkbox" checked={form.active} onChange={set('active')} />
          Active (can be bought)
        </label>
        <div className="col-span-2 flex justify-end gap-2 md:col-span-3">
          {editingId && (
            <button type="button" className={ui.btnGhost} onClick={reset}>
              Cancel
            </button>
          )}
          <button type="submit" className={ui.btn} disabled={saving}>
            <FiPlus /> {editingId ? 'Update variant' : 'Add variant'}
          </button>
        </div>
      </form>
    </section>
  );
}