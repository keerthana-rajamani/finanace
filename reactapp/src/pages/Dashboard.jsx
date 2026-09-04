import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import './Dashboard.css';

const TXN_CATEGORIES = ['Food', 'Transport', 'Utilities', 'Shopping', 'Healthcare', 'Entertainment', 'Education', 'Investment', 'Salary', 'Other'];

export default function Dashboard() {
  const { auth } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [showAddAccount, setShowAddAccount] = useState(false);
  const [showAddTxn, setShowAddTxn] = useState(false);
  const [accountForm, setAccountForm] = useState({ bankName: '', accountType: 'SAVINGS', maskedNumber: '', balance: '' });
  const [txnForm, setTxnForm] = useState({ accountId: '', amount: '', type: 'DEBIT', category: 'Food', merchant: '', description: '' });
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(() => {
    Promise.all([api.get('/accounts'), api.get('/transactions')])
      .then(([accRes, txnRes]) => {
        setAccounts(accRes.data);
        setTransactions(txnRes.data);
        if (accRes.data.length > 0) {
          setTxnForm(f => (f.accountId ? f : { ...f, accountId: accRes.data[0].id }));
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const totalBalance = accounts.reduce((sum, a) => sum + parseFloat(a.balance || 0), 0);

  const handleAddAccount = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/accounts', accountForm);
      const updated = [...accounts, res.data];
      setAccounts(updated);
      if (!txnForm.accountId) setTxnForm(f => ({ ...f, accountId: res.data.id }));
      setShowAddAccount(false);
      setAccountForm({ bankName: '', accountType: 'SAVINGS', maskedNumber: '', balance: '' });
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to add account');
    }
  };

  const handleDeleteAccount = async (id) => {
    if (!window.confirm('Unlink this account?')) return;
    await api.delete(`/accounts/${id}`);
    setAccounts(accounts.filter(a => a.id !== id));
    setTransactions(transactions.filter(t => t.account?.id !== id));
  };

  const handleAddTransaction = async (e) => {
    e.preventDefault();
    if (!txnForm.accountId) return alert('Please select an account');
    try {
      const payload = {
        amount: parseFloat(txnForm.amount),
        type: txnForm.type,
        category: txnForm.category,
        merchant: txnForm.merchant || null,
        description: txnForm.description || null,
      };
      const res = await api.post(`/transactions/account/${txnForm.accountId}`, payload);
      setTransactions([res.data, ...transactions]);
      setShowAddTxn(false);
      setTxnForm(f => ({ ...f, amount: '', merchant: '', description: '' }));
      // Refresh accounts to update balance display
      api.get('/accounts').then(r => setAccounts(r.data));
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to add transaction');
    }
  };

  const handleDeleteTransaction = async (id) => {
    if (!window.confirm('Delete this transaction?')) return;
    await api.delete(`/transactions/${id}`);
    setTransactions(transactions.filter(t => t.id !== id));
  };

  if (loading) return <div className="loading">Loading dashboard...</div>;

  return (
    <div className="dashboard">
      <div className="dash-header">
        <div>
          <h2>Welcome back, {auth.user?.name}!</h2>
          <p className="dash-role">Role: {auth.user?.role}</p>
        </div>
        <div className="kpi-card">
          <span className="kpi-label">Total Balance</span>
          <span className="kpi-value">₹{totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
        </div>
      </div>

      <div className="dash-grid">
        <div className="dash-section">
          <div className="section-header">
            <h3>Linked Accounts ({accounts.length})</h3>
            <button className="btn-add" onClick={() => setShowAddAccount(!showAddAccount)}>+ Add Account</button>
          </div>

          {showAddAccount && (
            <form className="inline-form" onSubmit={handleAddAccount}>
              <input placeholder="Bank Name" value={accountForm.bankName} onChange={e => setAccountForm({ ...accountForm, bankName: e.target.value })} required />
              <select value={accountForm.accountType} onChange={e => setAccountForm({ ...accountForm, accountType: e.target.value })}>
                <option value="SAVINGS">Savings</option>
                <option value="CURRENT">Current</option>
                <option value="CREDIT">Credit</option>
                <option value="DEMAT">DEMAT</option>
              </select>
              <input placeholder="Last 4 digits (e.g. XXXX1234)" value={accountForm.maskedNumber} onChange={e => setAccountForm({ ...accountForm, maskedNumber: e.target.value })} />
              <input type="number" placeholder="Balance" value={accountForm.balance} onChange={e => setAccountForm({ ...accountForm, balance: e.target.value })} required />
              <button type="submit" className="btn-save">Link Account</button>
              <button type="button" className="btn-cancel" onClick={() => setShowAddAccount(false)}>Cancel</button>
            </form>
          )}

          {accounts.length === 0 ? (
            <p className="empty-msg">No accounts linked yet. Add your first account above.</p>
          ) : (
            <div className="account-list">
              {accounts.map(acc => (
                <div className="account-card" key={acc.id}>
                  <div className="acc-info">
                    <span className="acc-bank">{acc.bankName}</span>
                    <span className="acc-type">{acc.accountType}</span>
                    {acc.maskedNumber && <span className="acc-num">{acc.maskedNumber}</span>}
                  </div>
                  <div className="acc-right">
                    <span className="acc-balance">₹{parseFloat(acc.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                    <button className="btn-delete" onClick={() => handleDeleteAccount(acc.id)}>Unlink</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="dash-section">
          <div className="section-header">
            <h3>Transactions ({transactions.length})</h3>
            {accounts.length > 0 && (
              <button className="btn-add" onClick={() => setShowAddTxn(!showAddTxn)}>+ Add</button>
            )}
          </div>

          {showAddTxn && (
            <form className="txn-form" onSubmit={handleAddTransaction}>
              <div className="txn-form-row">
                <select value={txnForm.accountId} onChange={e => setTxnForm({ ...txnForm, accountId: e.target.value })} required>
                  {accounts.map(a => (
                    <option key={a.id} value={a.id}>{a.bankName} ({a.accountType})</option>
                  ))}
                </select>
                <select value={txnForm.type} onChange={e => setTxnForm({ ...txnForm, type: e.target.value })}>
                  <option value="DEBIT">Debit</option>
                  <option value="CREDIT">Credit</option>
                </select>
              </div>
              <div className="txn-form-row">
                <input type="number" placeholder="Amount (₹)" value={txnForm.amount} onChange={e => setTxnForm({ ...txnForm, amount: e.target.value })} required min="0.01" step="0.01" />
                <select value={txnForm.category} onChange={e => setTxnForm({ ...txnForm, category: e.target.value })}>
                  {TXN_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className="txn-form-row">
                <input placeholder="Merchant (optional)" value={txnForm.merchant} onChange={e => setTxnForm({ ...txnForm, merchant: e.target.value })} />
                <input placeholder="Description (optional)" value={txnForm.description} onChange={e => setTxnForm({ ...txnForm, description: e.target.value })} />
              </div>
              <div className="form-actions">
                <button type="submit" className="btn-save">Add Transaction</button>
                <button type="button" className="btn-cancel" onClick={() => setShowAddTxn(false)}>Cancel</button>
              </div>
            </form>
          )}

          {transactions.length === 0 ? (
            <p className="empty-msg">No transactions found.</p>
          ) : (
            <div className="txn-scroll">
              <table className="txn-table">
                <thead>
                  <tr><th>Date</th><th>Merchant</th><th>Category</th><th>Type</th><th>Amount</th><th></th></tr>
                </thead>
                <tbody>
                  {transactions.slice(0, 20).map(t => (
                    <tr key={t.id}>
                      <td>{new Date(t.txnDate).toLocaleDateString('en-IN')}</td>
                      <td>{t.merchant || t.description || '—'}</td>
                      <td>{t.category || '—'}</td>
                      <td><span className={`badge ${t.type === 'CREDIT' ? 'badge-green' : 'badge-red'}`}>{t.type}</span></td>
                      <td className={t.type === 'CREDIT' ? 'amount-credit' : 'amount-debit'}>
                        {t.type === 'CREDIT' ? '+' : '-'}₹{parseFloat(t.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td>
                        <button className="btn-del-txn" onClick={() => handleDeleteTransaction(t.id)} title="Delete">✕</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
