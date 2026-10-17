import { env } from '../../config/env';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        &copy; {new Date().getFullYear()} {env.appName}
      </div>
    </footer>
  );
}
