import { Link, useNavigate } from 'react-router-dom';
import { env } from '../../config/env';
import { useAuth } from '../../features/auth/useAuth';

export default function Header() {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/', { replace: true });
  };

  return (
    <header className="header">
      <div className="container header__inner">
        <Link to="/" className="header__brand">
          {env.appName}
        </Link>
        <nav aria-label="Main navigation" className="header__nav">
          <Link to="/">Home</Link>
          {status === 'authenticated' && (
            <>
              <Link to="/account">{user.firstName}</Link>
              <button type="button" className="button button--link" onClick={handleLogout}>
                Log out
              </button>
            </>
          )}
          {status === 'anonymous' && (
            <>
              <Link to="/login">Log in</Link>
              <Link to="/register">Sign up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
