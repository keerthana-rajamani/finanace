import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './NavBar.css';

export default function NavBar() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
    setMenuOpen(false);
  };

  const isActive = (path) => location.pathname === path ? 'nav-link nav-active' : 'nav-link';
  const close = () => setMenuOpen(false);

  return (
    <nav className="navbar">
      <span className="nav-brand">Personal Finance and Budget Management Application</span>
      <button className="nav-hamburger" onClick={() => setMenuOpen(!menuOpen)} aria-label="Toggle menu">
        {menuOpen ? '✕' : '☰'}
      </button>
      <div className={`nav-links ${menuOpen ? 'nav-open' : ''}`}>
        <Link className={isActive('/')} to="/" onClick={close}>Home</Link>
        {!auth ? (
          <>
            <Link className={isActive('/login')} to="/login" onClick={close}>Login</Link>
            <Link className={isActive('/register')} to="/register" onClick={close}>Register</Link>
          </>
        ) : (
          <>
            <Link className={isActive('/dashboard')} to="/dashboard" onClick={close}>Dashboard</Link>
            <Link className={isActive('/budget')} to="/budget" onClick={close}>Budget</Link>
            <Link className={isActive('/goals')} to="/goals" onClick={close}>Goals</Link>
            <Link className={isActive('/ai')} to="/ai" onClick={close}>AI Insights</Link>
            <Link className={isActive('/bills')} to="/bills" onClick={close}>Bills</Link>
            <Link className={isActive('/tax')} to="/tax" onClick={close}>Tax</Link>
            <span className="nav-user">{auth.user?.name} {auth.user?.role && `(${auth.user.role})`}</span>
            <button className="nav-logout" onClick={handleLogout}>Logout</button>
          </>
        )}
      </div>
    </nav>
  );
}
