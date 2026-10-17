export default function ErrorMessage({ message }) {
  return (
    <p role="alert" className="error">
      {message}
    </p>
  );
}
