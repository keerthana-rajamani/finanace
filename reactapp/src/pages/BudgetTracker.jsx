import React, { useEffect, useState } from 'react';
import api from '../api/api';
import './BudgetTracker.css';

const CATEGORIES = ['Food', 'Transport', 'Utilities', 'Shopping', 'Healthcare', 'Entertainment', 'Education', 'Investment', 'Other'];

export default function BudgetTracker() {
  const [budgets, setBudgets] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editBudget, setEditBudget] = useState(null);
  const [form, setForm] = useState({ category: 'Food', budgetAmount: '', alertAtPercent: 80, carryForward: false });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/budgets/summary').then(res => setBudgets(res.data)).finally(() => setLoading(false));
  }, []);

  const validate = () => {
    const e = {};
    if (!form.budgetAmount || parseFloat(form.budgetAmount) <= 0) e.budgetAmount = 'Budget must be a positive number';
    return e;
  };

  const resetForm = () => {
    setForm({ category: 'Food', budgetAmount: '', alertAtPercent: 80, carryForward: false });
    setErrors({});
    setEditBudget(null);
    setShowForm(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    try {
      if (editBudget) {
        const res = await api.put(`/budgets/${editBudget.id}`, {
          budgetAmount: parseFloat(form.budgetAmount),
          alertAtPercent: parseInt(form.alertAtPercent),
          carryForward: form.carryForward,
        });
        setBudgets(budgets.map(b => b.id === editBudget.id ? res.data : b));
      } else {
        const res = await api.post('/budgets', { ...form, budgetAmount: parseFloat(form.budgetAmount) });
        setBudgets([...budgets, res.data]);
      }
      resetForm();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to save budget');
    }
  };

  const handleEdit = (b) => {
    setForm({ category: b.category, budgetAmount: b.budgetAmount, alertAtPercent: b.alertAtPercent, carryForward: b.carryForward });
    setEditBudget(b);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this budget?')) return;
    await api.delete(`/budgets/${id}`);
    setBudgets(budgets.filter(b => b.id !== id));
  };

  const getProgress = (spent, total) => Math.min((spent / total) * 100, 100);
  const getAlertClass = (spent, total, alertAt) => {
    const pct = (spent / total) * 100;
    if (pct >= 100) return 'bar-over';
    if (pct >= alertAt) return 'bar-alert';
    return 'bar-ok';
  };

  if (loading) return <div className="loading">Loading budgets...</div>;

  return (
    <div className="budget-page">
      <div className="page-header">
        <h2>Budget Tracker</h2>
        <button className="btn-add" onClick={() => { resetForm(); setShowForm(!showForm); }}>+ New Budget</button>
      </div>

      {showForm && (
        <div className="budget-form-card">
          <h3>{editBudget ? 'Edit Budget' : 'Create Budget'}</h3>
          <form onSubmit={handleSubmit} className="budget-form">
            <div className="form-row">
              <div className="form-group">
                <label>Category</label>
                <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} disabled={!!editBudget}>
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Budget Amount (₹)</label>
                <input type="number" value={form.budgetAmount} onChange={e => setForm({ ...form, budgetAmount: e.target.value })} placeholder="e.g. 5000" min="1" />
                {errors.budgetAmount && <span className="field-error">{errors.budgetAmount}</span>}
              </div>
              <div className="form-group">
                <label>Alert at (%)</label>
                <input type="number" value={form.alertAtPercent} onChange={e => setForm({ ...form, alertAtPercent: parseInt(e.target.value) })} min="1" max="100" />
              </div>
            </div>
            <label className="checkbox-label">
              <input type="checkbox" checked={form.carryForward} onChange={e => setForm({ ...form, carryForward: e.target.checked })} />
              Carry forward unused budget to next month
            </label>
            <div className="form-actions">
              <button type="submit" className="btn-save">{editBudget ? 'Update Budget' : 'Create Budget'}</button>
              <button type="button" className="btn-cancel" onClick={resetForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {budgets.length === 0 ? (
        <div className="empty-state">
          <p>No budgets set for this month. Create your first budget above.</p>
        </div>
      ) : (
        <div className="budget-grid">
          {budgets.map(b => {
            const spent = parseFloat(b.spentAmount || 0);
            const total = parseFloat(b.budgetAmount);
            const pct = getProgress(spent, total);
            const alertClass = getAlertClass(spent, total, b.alertAtPercent);
            return (
              <div className="budget-card" key={b.id}>
                <div className="budget-card-header">
                  <span className="budget-category">{b.category}</span>
                  <div className="budget-card-actions">
                    <button className="btn-edit-budget" onClick={() => handleEdit(b)} title="Edit">✏️</button>
                    <button className="btn-delete-sm" onClick={() => handleDelete(b.id)}>✕</button>
                  </div>
                </div>
                <div className="budget-amounts">
                  <span className="spent">₹{spent.toLocaleString('en-IN')}</span>
                  <span className="separator"> / </span>
                  <span className="total">₹{total.toLocaleString('en-IN')}</span>
                </div>
                <div className="progress-bar-bg">
                  <div className={`progress-bar-fill ${alertClass}`} style={{ width: `${pct}%` }} />
                </div>
                <div className="budget-footer">
                  <span className="pct-label">{pct.toFixed(0)}% used</span>
                  {pct >= 100 && <span className="alert-badge over">Over Budget!</span>}
                  {pct >= b.alertAtPercent && pct < 100 && <span className="alert-badge warn">Alert: {b.alertAtPercent}% reached</span>}
                  {b.carryForward && <span className="carry-badge">Carry Forward</span>}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
