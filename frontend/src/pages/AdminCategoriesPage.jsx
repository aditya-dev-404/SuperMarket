import { useEffect, useState } from 'react';
import { FiEdit2, FiPlus, FiTrash2 } from 'react-icons/fi';
import {
  createCategory,
  deactivateCategory,
  getCategories,
  updateCategory,
} from '../features/catalog/catalogApi';
import { errorMessage, ui } from '../features/catalog/catalogUi';

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [name, setName] = useState('');
  const [parentId, setParentId] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

    const [reloadKey, setReloadKey] = useState(0);
  const reload = () => setReloadKey((k) => k + 1);

  useEffect(() => {
    let ignore = false;
    getCategories()
      .then((data) => {
        if (!ignore) setCategories(data);
      })
      .catch((err) => {
        if (!ignore) setError(errorMessage(err));
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const nameById = Object.fromEntries(categories.map((c) => [c.id, c.name]));

  const reset = () => {
    setName('');
    setParentId('');
    setEditingId(null);
  };

  const startEdit = (c) => {
    setEditingId(c.id);
    setName(c.name);
    setParentId(c.parentId ?? '');
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    const body = { name: name.trim(), parentId: parentId === '' ? null : Number(parentId) };
    try {
      if (editingId) {
        await updateCategory(editingId, body);
      } else {
        await createCategory(body);
      }
      reset();
      reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (c) => {
    if (!window.confirm(`Deactivate "${c.name}"? Existing products keep it, but vendors cannot pick it.`)) return;
    setError('');
    try {
      await deactivateCategory(c.id);
      reload();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900">Categories</h1>

      {error && <div className={ui.error}>{error}</div>}

      <form onSubmit={submit} className={`${ui.card} grid gap-3 sm:grid-cols-[1fr_1fr_auto]`}>
        <div>
          <label className={ui.label}>Name *</label>
          <input className={ui.input} value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
        </div>
        <div>
          <label className={ui.label}>Parent category</label>
          <select className={ui.input} value={parentId} onChange={(e) => setParentId(e.target.value)}>
            <option value="">None (top level)</option>
            {categories
              .filter((c) => c.id !== editingId)
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
          </select>
        </div>
        <div className="flex items-end gap-2">
          {editingId && (
            <button type="button" className={ui.btnGhost} onClick={reset}>
              Cancel
            </button>
          )}
          <button type="submit" className={ui.btn} disabled={saving}>
            <FiPlus /> {editingId ? 'Update' : 'Add'}
          </button>
        </div>
      </form>

      <div className={ui.card}>
        {loading ? (
          <p className="text-sm text-gray-500">Loading...</p>
        ) : categories.length === 0 ? (
          <p className="text-sm text-gray-500">No categories yet. Add the first one above.</p>
        ) : (
          <ul className="divide-y">
            {categories.map((c) => (
              <li key={c.id} className="flex items-center justify-between py-2">
                <div>
                  <span className="font-medium text-gray-900">{c.name}</span>
                  {c.parentId && (
                    <span className="ml-2 text-sm text-gray-500">in {nameById[c.parentId] ?? '...'}</span>
                  )}
                </div>
                <div>
                  <button type="button" className={ui.iconBtn} onClick={() => startEdit(c)} aria-label="Edit">
                    <FiEdit2 />
                  </button>
                  <button type="button" className={ui.iconBtnDanger} onClick={() => remove(c)} aria-label="Deactivate">
                    <FiTrash2 />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}