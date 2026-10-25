/** Flat [{name, slug, parentSlug}] → nested tree. Orphans (inactive parent) become roots. */
export function buildCategoryTree(flat = []) {
  const nodes = new Map(flat.map((c) => [c.slug, { ...c, children: [] }]));
  const roots = [];
  nodes.forEach((node) => {
    const parent = node.parentSlug ? nodes.get(node.parentSlug) : null;
    (parent ? parent.children : roots).push(node);
  });
  return roots;
}