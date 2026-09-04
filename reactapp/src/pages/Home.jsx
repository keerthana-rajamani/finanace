import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Home.css';

export default function Home() {
  const { auth } = useAuth();

  return (
    <div className="home">
      <div className="hero">
        <h1>Take Control of Your Finances</h1>
        <p>Smart budgeting, goal tracking, and AI-powered insights — all in one place.</p>
        <div className="hero-actions">
          {auth ? (
            <>
              <Link to="/dashboard" className="btn-primary">📊 Dashboard</Link>
              <Link to="/budget" className="btn-secondary">💰 Budget Tracker</Link>
              <Link to="/goals" className="btn-secondary">🎯 My Goals</Link>
            </>
          ) : (
            <>
              <Link to="/register" className="btn-primary">Get Started Free</Link>
              <Link to="/login" className="btn-secondary">Sign In</Link>
            </>
          )}
        </div>
      </div>

      {auth && (
        <div className="quick-access">
          <h2>Quick Access</h2>
          <div className="quick-grid">
            <Link to="/dashboard" className="quick-card">
              <span className="quick-icon">🏠</span>
              <span className="quick-label">Dashboard</span>
            </Link>
            <Link to="/budget" className="quick-card">
              <span className="quick-icon">📊</span>
              <span className="quick-label">Budget Tracker</span>
            </Link>
            <Link to="/goals" className="quick-card">
              <span className="quick-icon">🎯</span>
              <span className="quick-label">Goals</span>
            </Link>
          </div>
        </div>
      )}

      <div className="features-section">
        <h2>Everything You Need</h2>
        <div className="features">

          <Link to="/dashboard" className="feature-card">
            <span className="feature-icon">🏦</span>
            <h3>Bank Account Aggregation</h3>
            <p>Link all your bank accounts and view balances in one place.</p>
            <span className="feature-btn">Go to Dashboard →</span>
          </Link>

          <Link to="/budget" className="feature-card">
            <span className="feature-icon">📊</span>
            <h3>Budget Tracker</h3>
            <p>Set category-wise budgets with real-time spend tracking and alerts.</p>
            <span className="feature-btn">Track Budget →</span>
          </Link>

          <Link to="/goals" className="feature-card">
            <span className="feature-icon">🎯</span>
            <h3>Financial Goals</h3>
            <p>Create and track financial goals with monthly savings recommendations.</p>
            <span className="feature-btn">View Goals →</span>
          </Link>

          <Link to="/dashboard" className="feature-card">
            <span className="feature-icon">💹</span>
            <h3>Investment Portfolio</h3>
            <p>Track DEMAT and mutual fund holdings with XIRR calculations.</p>
            <span className="feature-btn">View Portfolio →</span>
          </Link>

          <Link to="/ai" className="feature-card">
            <span className="feature-icon">🤖</span>
            <h3>AI Insights</h3>
            <p>Personalised financial health score and weekly money tips powered by Gemini.</p>
            <span className="feature-btn">Get Insights →</span>
          </Link>

          <Link to="/bills" className="feature-card">
            <span className="feature-icon">📋</span>
            <h3>Bill Manager</h3>
            <p>Track recurring bills with automated reminders 3 days and 1 day before due date.</p>
            <span className="feature-btn">Manage Bills →</span>
          </Link>

          <Link to="/tax" className="feature-card">
            <span className="feature-icon">🧾</span>
            <h3>Tax Summary</h3>
            <p>Capital gains, 80C deductions, advance tax tracker and estimated tax computation.</p>
            <span className="feature-btn">View Tax →</span>
          </Link>

          <Link to={auth ? "/dashboard" : "/register"} className="feature-card">
            <span className="feature-icon">🔒</span>
            <h3>Bank-Grade Security</h3>
            <p>AES-256 encryption and JWT authentication for all your data.</p>
            <span className="feature-btn">{auth ? 'My Account →' : 'Get Started →'}</span>
          </Link>

        </div>
      </div>
    </div>
  );
}