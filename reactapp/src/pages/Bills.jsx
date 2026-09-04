import React, { useEffect, useState } from 'react';
import api from '../api/api';
import './Bills.css';

const CATEGORIES = ['Electricity', 'Water', 'Internet', 'Mobile', 'Rent', 'Insurance', 'Subscription', 'EMI', 'Other'];

export default function Bills() {
  const [bills, setBills] = useState([]);
  const [upcoming, setUpcoming] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editBill, setEditBill] = useState(null);
  const [form, setForm] = useState({ name: '', category: 'Electricity', amount: '', dueDayOfMonth: '', recurrence: 'MONTHLY' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('all');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [billsRes, upcomingRes] = await Promise.all([
        api.get('/bills'),
        api.get('/bills/upcoming'),
      ]);
      setBills(billsRes.data);
      setUpcoming(upcomingRes.data);
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = 'Bill name is required';
    if (!form.amount || parseFloat(form.amount) <= 0) e.amount = 'Bill amount must be a positive number';
    if (!form.dueDayOfMonth || form.dueDayOfMonth < 1 || form.dueDayOfMonth > 31) e.dueDayOfMonth = 'Enter a valid day (1-31)';
    return e;
  };

  const resetForm = () => {
    setForm({ name: '', category: 'Electricity', amount: '', dueDayOfMonth: '', recurrence: 'MONTHLY' });
    setErrors({});
    setEditBill(null);
    setShowForm(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    try {
      const payload = { ...form, amount: parseFloat(form.amount), dueDayOfMonth: parseInt(form.dueDayOfMonth) };
      if (editBill) {
        const res = await api.put(`/bills/${editBill.id}`, payload);
        setBills(bills.map(b => b.id === editBill.id ? res.data : b));
      } else {
        const res = await api.post('/bills', payload);
        setBills([...bills, res.data]);
      }
      resetForm();
      fetchData();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to save bill');
    }
  };

  const handleEdit = (bill) => {
    setForm({ name: bill.name, category: bill.category || 'Other', amount: bill.amount, dueDayOfMonth: bill.dueDayOfMonth, recurrence: bill.recurrence });
    setEditBill(bill);
    setShowForm(true);
  };

  const handlePay = async (id) => {
    try {
      const res = await api.put(`/bills/${id}/pay`);
      setBills(bills.map(b => b.id === id ? res.data : b));
      fetchData();
    } catch (err) {
      alert('Failed to mark as paid');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this bill?')) return;
    await api.delete(`/bills/${id}`);
    setBills(bills.filter(b => b.id !== id));
    fetchData();
  };

  const getDaysUntilDue = (nextDueDate) => {
    if (!nextDueDate) return null;
    const today = new Date();
    const due = new Date(nextDueDate);
    const diff = Math.ceil((due - today) / (1000 * 60 * 60 * 24));
    return diff;
  };

  const getStatusBadge = (bill) => {
    const days = getDaysUntilDue(bill.nextDueDate);
    if (bill.status === 'PAID') return <span className="badge badge-paid">✓ Paid</span>;
    if (days !== null && days < 0) return <span className="badge badge-overdue">Overdue</span>;
    if (days !== null && days <= 3) return <span className="badge badge-urgent">Due in {days}d</span>;
    if (days !== null && days <= 7) return <span className="badge badge-soon">Due in {days}d</span>;
    return <span className="badge badge-pending">Pending</span>;
  };

  const displayBills = activeTab === 'upcoming' ? upcoming : bills;

  if (loading) return <div className="loading">Loading bills...</div>;

  return (
    <div className="bills-page">
      <div className="page-header">
        <h2>Bill Manager</h2>
        <button className="btn-add" onClick={() => { resetForm(); setShowForm(!showForm); }}>+ Add Bill</button>
      </div>

      {/* Summary Cards */}
      <div className="bills-summary">
        <div className="summary-card">
          <span className="summary-icon">📋</span>
          <div>
            <span className="summary-value">{bills.length}</span>
            <span className="summary-label">Total Bills</span>
          </div>
        </div>
        <div className="summary-card">
          <span className="summary-icon">⏰</span>
          <div>
            <span className="summary-value">{upcoming.length}</span>
            <span className="summary-label">Due This Week</span>
          </div>
        </div>
        <div className="summary-card">
          <span className="summary-icon">✅</span>
          <div>
            <span className="summary-value">{bills.filter(b => b.status === 'PAID').length}</span>
            <span className="summary-label">Paid This Month</span>
          </div>
        </div>
        <div className="summary-card">
          <span className="summary-icon">💰</span>
          <div>
            <span className="summary-value">₹{bills.reduce((s, b) => s + parseFloat(b.amount || 0), 0).toLocaleString('en-IN')}</span>
            <span className="summary-label">Monthly Total</span>
          </div>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bill-form-card">
          <h3>{editBill ? 'Edit Bill' : 'Add New Bill'}</h3>
          <form onSubmit={handleSubmit} className="bill-form">
            <div className="form-row">
              <div className="form-group">
                <label>Bill Name</label>
                <input type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Electricity Bill" />
                {errors.name && <span className="field-error">{errors.name}</span>}
              </div>
              <div className="form-group">
                <label>Category</label>
                <select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })}>
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Amount (₹)</label>
                <input type="number" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} placeholder="e.g. 1500" min="1" />
                {errors.amount && <span className="field-error">{errors.amount}</span>}
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Due Day of Month</label>
                <input type="number" value={form.dueDayOfMonth} onChange={e => setForm({ ...form, dueDayOfMonth: e.target.value })} placeholder="e.g. 15" min="1" max="31" />
                {errors.dueDayOfMonth && <span className="field-error">{errors.dueDayOfMonth}</span>}
              </div>
              <div className="form-group">
                <label>Recurrence</label>
                <select value={form.recurrence} onChange={e => setForm({ ...form, recurrence: e.target.value })}>
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="ANNUAL">Annual</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-save">{editBill ? 'Update Bill' : 'Add Bill'}</button>
              <button type="button" className="btn-cancel" onClick={resetForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* Tabs */}
      <div className="bills-tabs">
        <button className={`tab-btn ${activeTab === 'all' ? 'tab-active' : ''}`} onClick={() => setActiveTab('all')}>All Bills ({bills.length})</button>
        <button className={`tab-btn ${activeTab === 'upcoming' ? 'tab-active' : ''}`} onClick={() => setActiveTab('upcoming')}>
          Due This Week {upcoming.length > 0 && <span className="tab-badge">{upcoming.length}</span>}
        </button>
      </div>

      {/* Bills List */}
      {displayBills.length === 0 ? (
        <div className="empty-state"><p>{activeTab === 'upcoming' ? 'No bills due this week.' : 'No bills added yet.'}</p></div>
      ) : (
        <div className="bills-list">
          {displayBills.map(bill => {
            const days = getDaysUntilDue(bill.nextDueDate);
            return (
              <div className={`bill-card ${days !== null && days <= 3 && bill.status !== 'PAID' ? 'bill-urgent' : ''}`} key={bill.id}>
                <div className="bill-left">
                  <div className="bill-icon">{getCategoryIcon(bill.category)}</div>
                  <div className="bill-info">
                    <span className="bill-name">{bill.name}</span>
                    <span className="bill-meta">{bill.category} • {bill.recurrence} • Due on {bill.dueDayOfMonth}th</span>
                    {bill.nextDueDate && <span className="bill-due">Next due: {new Date(bill.nextDueDate).toLocaleDateString('en-IN')}</span>}
                  </div>
                </div>
                <div className="bill-right">
                  <span className="bill-amount">₹{parseFloat(bill.amount).toLocaleString('en-IN')}</span>
                  {getStatusBadge(bill)}
                  <div className="bill-actions">
                    {bill.status !== 'PAID' && (
                      <button className="btn-pay" onClick={() => handlePay(bill.id)}>Mark Paid</button>
                    )}
                    <button className="btn-edit-sm" onClick={() => handleEdit(bill)}>Edit</button>
                    <button className="btn-del-sm" onClick={() => handleDelete(bill.id)}>✕</button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function getCategoryIcon(category) {
  const icons = { Electricity: '⚡', Water: '💧', Internet: '🌐', Mobile: '📱', Rent: '🏠', Insurance: '🛡️', Subscription: '📺', EMI: '🏦', Other: '📄' };
  return icons[category] || '📄';
}
