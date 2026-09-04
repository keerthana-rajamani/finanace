import React, { useEffect, useState } from 'react';
import api from '../api/api';
import './Goals.css';

export default function Goals() {
  const [goals, setGoals] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editGoal, setEditGoal] = useState(null);
  const [form, setForm] = useState({ name: '', targetAmount: '', targetDate: '', currentAmount: '0', priority: 'MEDIUM' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/goals').then(res => setGoals(res.data)).finally(() => setLoading(false));
  }, []);

  const validate = () => {
    const e = {};
    if (!form.name) e.name = 'Goal name is required';
    if (!form.targetAmount || parseFloat(form.targetAmount) <= 0) e.targetAmount = 'Target amount must be a positive number';
    else if (parseFloat(form.targetAmount) <= parseFloat(form.currentAmount || 0)) e.targetAmount = 'Target amount must exceed current savings';
    if (!form.targetDate) e.targetDate = 'Target date is required';
    return e;
  };

  const resetForm = () => {
    setForm({ name: '', targetAmount: '', targetDate: '', currentAmount: '0', priority: 'MEDIUM' });
    setErrors({});
    setEditGoal(null);
    setShowForm(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    try {
      const payload = { ...form, targetAmount: parseFloat(form.targetAmount), currentAmount: parseFloat(form.currentAmount || 0) };
      if (editGoal) {
        const res = await api.put(`/goals/${editGoal.id}`, payload);
        setGoals(goals.map(g => g.id === editGoal.id ? res.data : g));
      } else {
        const res = await api.post('/goals', payload);
        setGoals([...goals, res.data]);
      }
      resetForm();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to save goal');
    }
  };

  const handleEdit = (goal) => {
    setForm({
      name: goal.name,
      targetAmount: goal.targetAmount,
      targetDate: goal.targetDate,
      currentAmount: goal.currentAmount,
      priority: goal.priority,
      status: goal.status,
    });
    setEditGoal(goal);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this goal?')) return;
    await api.delete(`/goals/${id}`);
    setGoals(goals.filter(g => g.id !== id));
  };

  const getProgress = (current, target) => Math.min((current / target) * 100, 100);

  const getMonthsRemaining = (targetDate) => {
    const now = new Date();
    const target = new Date(targetDate);
    return Math.max(0, (target.getFullYear() - now.getFullYear()) * 12 + (target.getMonth() - now.getMonth()));
  };

  const getMonthlySavingsNeeded = (goal) => {
    const months = getMonthsRemaining(goal.targetDate);
    if (months <= 0) return 0;
    return ((parseFloat(goal.targetAmount) - parseFloat(goal.currentAmount)) / months).toFixed(0);
  };

  const priorityColor = { HIGH: '#e53935', MEDIUM: '#fb8c00', LOW: '#43a047' };

  if (loading) return <div className="loading">Loading goals...</div>;

  return (
    <div className="goals-page">
      <div className="page-header">
        <h2>Financial Goals</h2>
        <button className="btn-add" onClick={() => { resetForm(); setShowForm(!showForm); }}>+ New Goal</button>
      </div>

      {showForm && (
        <div className="goal-form-card">
          <h3>{editGoal ? 'Edit Goal' : 'Create Goal'}</h3>
          <form onSubmit={handleSubmit} className="goal-form">
            <div className="form-row">
              <div className="form-group">
                <label>Goal Name</label>
                <input type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Emergency Fund" />
                {errors.name && <span className="field-error">{errors.name}</span>}
              </div>
              <div className="form-group">
                <label>Target Amount (₹)</label>
                <input type="number" value={form.targetAmount} onChange={e => setForm({ ...form, targetAmount: e.target.value })} placeholder="e.g. 100000" min="1" />
                {errors.targetAmount && <span className="field-error">{errors.targetAmount}</span>}
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Current Savings (₹)</label>
                <input type="number" value={form.currentAmount} onChange={e => setForm({ ...form, currentAmount: e.target.value })} placeholder="0" min="0" />
              </div>
              <div className="form-group">
                <label>Target Date</label>
                <input type="date" value={form.targetDate} onChange={e => setForm({ ...form, targetDate: e.target.value })} min={new Date().toISOString().split('T')[0]} />
                {errors.targetDate && <span className="field-error">{errors.targetDate}</span>}
              </div>
              <div className="form-group">
                <label>Priority</label>
                <select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-save">{editGoal ? 'Update Goal' : 'Create Goal'}</button>
              <button type="button" className="btn-cancel" onClick={resetForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {goals.length === 0 ? (
        <div className="empty-state"><p>No goals yet. Create your first financial goal above.</p></div>
      ) : (
        <div className="goals-grid">
          {goals.map(goal => {
            const pct = getProgress(parseFloat(goal.currentAmount), parseFloat(goal.targetAmount));
            const monthly = getMonthlySavingsNeeded(goal);
            const months = getMonthsRemaining(goal.targetDate);
            const circumference = 2 * Math.PI * 40;
            const strokeDash = (pct / 100) * circumference;
            return (
              <div className="goal-card" key={goal.id}>
                <div className="goal-card-header">
                  <span className="goal-name">{goal.name}</span>
                  <span className="priority-dot" style={{ background: priorityColor[goal.priority] }} title={goal.priority}></span>
                </div>
                <div className="goal-ring-wrap">
                  <svg width="100" height="100" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="40" fill="none" stroke="#e8eaf6" strokeWidth="10" />
                    <circle cx="50" cy="50" r="40" fill="none" stroke={pct >= 100 ? '#43a047' : '#1a237e'}
                      strokeWidth="10" strokeDasharray={`${strokeDash} ${circumference}`}
                      strokeLinecap="round" transform="rotate(-90 50 50)" />
                    <text x="50" y="55" textAnchor="middle" fontSize="16" fontWeight="bold" fill="#1a237e">{pct.toFixed(0)}%</text>
                  </svg>
                </div>
                <div className="goal-amounts">
                  <span className="goal-current">₹{parseFloat(goal.currentAmount).toLocaleString('en-IN')}</span>
                  <span className="goal-sep"> / </span>
                  <span className="goal-target">₹{parseFloat(goal.targetAmount).toLocaleString('en-IN')}</span>
                </div>
                <div className="goal-meta">
                  <span>📅 {months} months left</span>
                  <span>💰 ₹{parseInt(monthly).toLocaleString('en-IN')}/mo needed</span>
                </div>
                <div className="goal-status">
                  <span className={`status-badge status-${goal.status?.toLowerCase()}`}>{goal.status}</span>
                </div>
                <div className="goal-actions">
                  <button className="btn-edit" onClick={() => handleEdit(goal)}>Edit</button>
                  <button className="btn-delete-sm" onClick={() => handleDelete(goal.id)}>Delete</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
