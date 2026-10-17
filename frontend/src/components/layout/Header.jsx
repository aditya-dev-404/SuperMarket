import { Link } from 'react-router-dom';
import { env } from '../../config/env';

export default function Header() {
  return (
    <header className="header">
      <div className="container header__inner">
        <Link to="/" className="header__brand">
          {env.appName}
        </Link>
        <nav aria-label="Main navigation" className="header__nav">
          <Link to="/">Home</Link>
        </nav>
      </div>
    </header>
  );
}
