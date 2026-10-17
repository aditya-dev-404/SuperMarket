export default function Loader({ label = 'Loading...' }) {
  return (
    <p role="status" className="muted">
      {label}
    </p>
  );
}
