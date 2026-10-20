export const ui = {
  input:
    'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500',
  label: 'mb-1 block text-sm font-medium text-gray-700',
  btn: 'inline-flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50',
  btnGhost:
    'inline-flex items-center gap-2 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50',
  iconBtn: 'rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800',
  iconBtnDanger: 'rounded p-1.5 text-red-500 hover:bg-red-50 hover:text-red-700',
  card: 'rounded-lg border border-gray-200 bg-white p-5 shadow-sm',
  error: 'rounded-md bg-red-50 px-3 py-2 text-sm text-red-700',
  success: 'rounded-md bg-green-50 px-3 py-2 text-sm text-green-700',
};

export const statusBadge = {
  DRAFT: 'bg-gray-100 text-gray-700',
  ACTIVE: 'bg-green-100 text-green-700',
  ARCHIVED: 'bg-amber-100 text-amber-700',
};

// Backend errors are RFC 9457 ProblemDetail: use `detail` first.
// apiClient already converts backend ProblemDetail errors into Error(message = detail)
export const errorMessage = (err) => err?.message || 'Something went wrong';