import React, { useEffect, useState } from 'react';
import api from '../api/api';
import './Tax.css';

const CURRENT_FY = new Date().getMonth() >= 3 ? new Date().getFullYear() : new Date().getFullYear() - 1;
const FY_OPTIONS = [CURRENT_FY, CURRENT_FY - 1, CURRENT_FY - 2];

const ADVANCE_TAX_DATES = [
  { label: '1st Instalment', date: 'June 15', percent: '15%' },
  { label: '2nd Instalment', date: 'September 15', percent: '45%' },
  { label: '3rd Instalment', date: 'December 15', percent: '75%' },
  { label: '4th Instalment', date: 'March 15', percent: '100%' },
];

export default function Tax() {
  const [summary, setSummary] = useState(null);
  const [selectedFY, setSelectedFY] = useState(CURRENT_FY);
  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchSummary(selectedFY);
  }, [selectedFY]);

  const fetchSummary = async (fy) => {
    setLoading(true);
    try {
      const res = await api.get(`/tax/summary?year=${fy}`);
      setSummary(res.data);
      setForm(res.data);
    } catch (err) {
      alert('Failed to load tax summary');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      let res;
      if (summary?.id) {
        res = await api.put(`/tax/${summary.id}`, form);
      } else {
        res = await api.post('/tax', { ...form, financialYear: selectedFY });
      }
      setSummary(res.data);
      setForm(res.data);
      setEditMode(false);
    } catch (err) {
      alert('Failed to save tax summary');
    } finally {
      setSaving(false);
    }
  };

  const fmt = (val) => parseFloat(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  const taxBalance = parseFloat(summary?.estimatedTax || 0) - parseFloat(summary?.advanceTaxPaid || 0);

  if (loading) return <div className="loading">Loading tax summary...</div>;

  return (
    <div className="tax-page">
      <div className="page-header">
        <h2>Tax Summary</h2>
        <div className="tax-header-right">
          <select className="fy-select" value={selectedFY} onChange={e => setSelectedFY(parseInt(e.target.value))}>
            {FY_OPTIONS.map(fy => (
              <option key={fy} value={fy}>FY {fy}-{(fy + 1).toString().slice(2)}</option>
            ))}
          </select>
          {!editMode ? (
            <button className="btn-edit-tax" onClick={() => setEditMode(true)}>✏️ Edit</button>
          ) : (
            <>
              <button className="btn-save-tax" onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : '💾 Save'}</button>
              <button className="btn-cancel-tax" onClick={() => { setEditMode(false); setForm(summary); }}>Cancel</button>
            </>
          )}
        </div>
      </div>

      <div className="tax-grid">
        {/* Income Section */}
        <div className="tax-card">
          <h3>📥 Income Details</h3>
          <div className="tax-rows">
            <TaxRow label="Total Income" value={summary?.totalIncome} editMode={editMode} field="totalIncome" form={form} setForm={setForm} />
            <TaxRow label="Interest Income" value={summary?.interestIncome} editMode={editMode} field="interestIncome" form={form} setForm={setForm} />
            <TaxRow label="LTCG (Long Term Capital Gains)" value={summary?.ltcg} editMode={editMode} field="ltcg" form={form} setForm={setForm} />
            <TaxRow label="STCG (Short Term Capital Gains)" value={summary?.stcg} editMode={editMode} field="stcg" form={form} setForm={setForm} />
          </div>
          <div className="tax-total">
            <span>Gross Total Income</span>
            <span>₹{fmt((parseFloat(summary?.totalIncome || 0) + parseFloat(summary?.interestIncome || 0) + parseFloat(summary?.ltcg || 0) + parseFloat(summary?.stcg || 0)))}</span>
          </div>
        </div>

        {/* Deductions Section */}
        <div className="tax-card">
          <h3>📉 Deductions</h3>
          <div className="tax-rows">
            <TaxRow label="Section 80C (ELSS, PPF, LIC, etc.)" value={summary?.section80c} editMode={editMode} field="section80c" form={form} setForm={setForm} note="Max ₹1,50,000" />
            <TaxRow label="Section 80D (Health Insurance)" value={summary?.section80d} editMode={editMode} field="section80d" form={form} setForm={setForm} note="Max ₹25,000" />
            <TaxRow label="HRA Exemption" value={summary?.hraExemption} editMode={editMode} field="hraExemption" form={form} setForm={setForm} />
          </div>
          <div className="tax-total">
            <span>Total Deductions</span>
            <span>₹{fmt(Math.min(parseFloat(summary?.section80c || 0), 150000) + Math.min(parseFloat(summary?.section80d || 0), 25000) + parseFloat(summary?.hraExemption || 0))}</span>
          </div>
        </div>

        {/* Tax Computation */}
        <div className="tax-card tax-computation">
          <h3>🧮 Tax Computation (New Regime)</h3>
          <div className="tax-rows">
            <div className="tax-row">
              <span>Taxable Income</span>
              <span className="tax-val">₹{fmt(summary?.taxableIncome)}</span>
            </div>
            <div className="tax-row">
              <span>Estimated Tax (incl. 4% cess)</span>
              <span className="tax-val tax-highlight">₹{fmt(summary?.estimatedTax)}</span>
            </div>
            <TaxRow label="Advance Tax Paid" value={summary?.advanceTaxPaid} editMode={editMode} field="advanceTaxPaid" form={form} setForm={setForm} />
            <div className="tax-row tax-balance-row">
              <span>{taxBalance >= 0 ? 'Tax Payable' : 'Tax Refund'}</span>
              <span className={`tax-val ${taxBalance >= 0 ? 'tax-payable' : 'tax-refund'}`}>
                ₹{fmt(Math.abs(taxBalance))}
              </span>
            </div>
          </div>
        </div>

        {/* Tax Slabs */}
        <div className="tax-card">
          <h3>📊 New Tax Regime Slabs (FY{selectedFY}-{(selectedFY + 1).toString().slice(2)})</h3>
          <table className="slab-table">
            <thead><tr><th>Income Range</th><th>Tax Rate</th></tr></thead>
            <tbody>
              <tr><td>Up to ₹3,00,000</td><td>Nil</td></tr>
              <tr><td>₹3,00,001 – ₹6,00,000</td><td>5%</td></tr>
              <tr><td>₹6,00,001 – ₹9,00,000</td><td>10%</td></tr>
              <tr><td>₹9,00,001 – ₹12,00,000</td><td>15%</td></tr>
              <tr><td>₹12,00,001 – ₹15,00,000</td><td>20%</td></tr>
              <tr><td>Above ₹15,00,000</td><td>30%</td></tr>
            </tbody>
          </table>
          <p className="slab-note">+ 4% Health & Education Cess on tax amount</p>
        </div>

        {/* Advance Tax Dates */}
        <div className="tax-card">
          <h3>📅 Advance Tax Due Dates</h3>
          <div className="advance-tax-list">
            {ADVANCE_TAX_DATES.map((d, i) => (
              <div className="advance-tax-row" key={i}>
                <div className="advance-tax-info">
                  <span className="advance-label">{d.label}</span>
                  <span className="advance-date">{d.date}</span>
                </div>
                <span className="advance-pct">{d.percent} of total tax</span>
              </div>
            ))}
          </div>
        </div>

        {/* 80C Tracker */}
        <div className="tax-card">
          <h3>💡 Deduction Headroom</h3>
          <div className="headroom-list">
            <HeadroomBar label="Section 80C" used={parseFloat(summary?.section80c || 0)} max={150000} />
            <HeadroomBar label="Section 80D" used={parseFloat(summary?.section80d || 0)} max={25000} />
          </div>
          <p className="headroom-note">Maximise deductions to reduce your tax liability</p>
        </div>
      </div>
    </div>
  );
}

function TaxRow({ label, value, editMode, field, form, setForm, note }) {
  return (
    <div className="tax-row">
      <span>{label}{note && <span className="tax-note"> ({note})</span>}</span>
      {editMode ? (
        <input
          type="number"
          className="tax-input"
          value={form[field] || ''}
          onChange={e => setForm({ ...form, [field]: e.target.value })}
          min="0"
          placeholder="0"
        />
      ) : (
        <span className="tax-val">₹{parseFloat(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
      )}
    </div>
  );
}

function HeadroomBar({ label, used, max }) {
  const pct = Math.min((used / max) * 100, 100);
  const remaining = Math.max(max - used, 0);
  return (
    <div className="headroom-item">
      <div className="headroom-header">
        <span>{label}</span>
        <span className="headroom-remaining">₹{remaining.toLocaleString('en-IN')} remaining</span>
      </div>
      <div className="headroom-bar-bg">
        <div className="headroom-bar-fill" style={{ width: `${pct}%`, background: pct >= 100 ? '#43a047' : '#1a237e' }} />
      </div>
      <div className="headroom-footer">
        <span>₹{used.toLocaleString('en-IN')} used</span>
        <span>of ₹{max.toLocaleString('en-IN')}</span>
      </div>
    </div>
  );
}
