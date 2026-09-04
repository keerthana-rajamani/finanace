import React, { useState, useEffect } from 'react';
import api from '../api/api';
import './AIInsights.css';

const TABS = [
  { key: 'insights', label: '💡 Financial Insights', endpoint: '/ai/insights', field: 'insights' },
  { key: 'spending', label: '📊 Spending Analysis', endpoint: '/ai/spending-analysis', field: 'analysis' },
  { key: 'budget', label: '💰 Budget Recommendations', endpoint: '/ai/budget-recommendations', field: 'recommendations' },
  { key: 'goals', label: '🎯 Goal Advice', endpoint: '/ai/goal-advice', field: 'advice' },
];

export default function AIInsights() {
  const [activeTab, setActiveTab] = useState('insights');
  const [results, setResults] = useState({});
  const [loading, setLoading] = useState({});
  const [question, setQuestion] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const [chatLoading, setChatLoading] = useState(false);
  const [status, setStatus] = useState({ isGeminiLive: false, engine: 'Smart Financial Intelligence Engine' });
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [keyInput, setKeyInput] = useState('');
  const [keyMessage, setKeyMessage] = useState('');

  useEffect(() => {
    fetchStatus();
  }, []);

  const fetchStatus = async () => {
    try {
      const res = await api.get('/ai/status');
      if (res.data) setStatus(res.data);
    } catch {
      // Keep default local engine status
    }
  };

  const handleSaveKey = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/ai/key', { apiKey: keyInput });
      if (res.data) {
        setStatus(res.data);
        setKeyMessage(res.data.isGeminiLive ? 'Connected to Google Gemini 2.0 successfully!' : 'Switched to Smart Financial Intelligence Engine.');
        setTimeout(() => {
          setShowKeyModal(false);
          setKeyMessage('');
        }, 1200);
      }
    } catch {
      setKeyMessage('Failed to update API key.');
    }
  };

  const fetchInsight = async (tab) => {
    if (results[tab.key]) return;
    setLoading(prev => ({ ...prev, [tab.key]: true }));
    try {
      const res = await api.get(tab.endpoint);
      setResults(prev => ({ ...prev, [tab.key]: res.data[tab.field] }));
    } catch (err) {
      setResults(prev => ({ ...prev, [tab.key]: 'Unable to load insights at this moment. Please try again.' }));
    } finally {
      setLoading(prev => ({ ...prev, [tab.key]: false }));
    }
  };

  const handleTabClick = (tab) => {
    setActiveTab(tab.key);
    fetchInsight(tab);
  };

  const handleRefresh = (tab) => {
    setResults(prev => ({ ...prev, [tab.key]: null }));
    setTimeout(() => fetchInsight(tab), 100);
  };

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;
    const userMsg = question.trim();
    setQuestion('');
    setChatHistory(prev => [...prev, { role: 'user', text: userMsg }]);
    setChatLoading(true);
    try {
      const res = await api.post('/ai/ask', { question: userMsg });
      setChatHistory(prev => [...prev, { role: 'ai', text: res.data.answer }]);
    } catch {
      setChatHistory(prev => [...prev, { role: 'ai', text: 'Sorry, I could not process your question. Please try again.' }]);
    } finally {
      setChatLoading(false);
    }
  };

  const formatText = (text) => {
    if (!text) return null;
    return text.split('\n').map((line, i) => {
      const trimmed = line.trim();
      if (!trimmed) return <br key={i} />;
      if (/^\d+\./.test(trimmed)) {
        return <p key={i} className="insight-point">{line}</p>;
      }
      if (trimmed.startsWith('===') || (trimmed.startsWith('**') && trimmed.endsWith('**'))) {
        return <p key={i} className="insight-heading">{line.replace(/\*\*/g, '').replace(/===/g, '').trim()}</p>;
      }
      if (trimmed.startsWith('•') || trimmed.startsWith('-')) {
        return <p key={i} className="insight-bullet">{line}</p>;
      }
      return <p key={i} className="insight-line">{line}</p>;
    });
  };

  const activeTabObj = TABS.find(t => t.key === activeTab);
  const botName = status?.isGeminiLive ? '🤖 Gemini 2.0' : '🤖 AI Advisor';

  return (
    <div className="ai-page">
      <div className="ai-header">
        <div className="ai-header-main">
          <div>
            <h2>🤖 AI Financial Advisor</h2>
            <p>Personalised financial intelligence and spending insights based on your actual account data</p>
          </div>
          <div className="ai-badge-group">
            <span className={`ai-engine-badge ${status?.isGeminiLive ? 'badge-live' : 'badge-local'}`}>
              <span className="badge-dot"></span>
              {status?.engine || 'Smart Financial Intelligence Engine'}
            </span>
            <button className="btn-key-config" onClick={() => setShowKeyModal(true)} title="Configure Gemini API Key">
              ⚙️ Key
            </button>
          </div>
        </div>
      </div>

      {showKeyModal && (
        <div className="modal-backdrop" onClick={() => setShowKeyModal(false)}>
          <div className="key-modal-card" onClick={e => e.stopPropagation()}>
            <h3>⚙️ Configure Google Gemini API Key</h3>
            <p className="modal-subtitle">
              The application operates seamlessly with the built-in <strong>Smart Financial Intelligence Engine</strong>.
              If you have your own personal Google Gemini API Key, you can optionally connect it here.
            </p>
            <form onSubmit={handleSaveKey}>
              <input
                type="password"
                className="key-input"
                placeholder="Paste Gemini API key (starts with AIzaSy...)"
                value={keyInput}
                onChange={e => setKeyInput(e.target.value)}
              />
              {keyMessage && <p className="key-feedback">{keyMessage}</p>}
              <div className="modal-actions">
                <button type="submit" className="btn-save-key">
                  Save & Connect
                </button>
                <button
                  type="button"
                  className="btn-default-key"
                  onClick={() => {
                    setKeyInput('');
                    handleSaveKey({ preventDefault: () => {} });
                  }}
                >
                  Use Smart Engine
                </button>
                <button type="button" className="btn-cancel-modal" onClick={() => setShowKeyModal(false)}>
                  Close
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="ai-layout">
        {/* Left: Tabs */}
        <div className="ai-tabs-panel">
          <div className="ai-tabs">
            {TABS.map(tab => (
              <button
                key={tab.key}
                className={`ai-tab ${activeTab === tab.key ? 'ai-tab-active' : ''}`}
                onClick={() => handleTabClick(tab)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="ai-content-card">
            <div className="ai-content-header">
              <span>{activeTabObj?.label}</span>
              <button
                className="btn-refresh"
                onClick={() => handleRefresh(activeTabObj)}
                disabled={loading[activeTab]}
              >
                🔄 Refresh
              </button>
            </div>

            {loading[activeTab] ? (
              <div className="ai-loading">
                <div className="ai-spinner"></div>
                <p>Analysing your financial records...</p>
              </div>
            ) : results[activeTab] ? (
              <div className="ai-result">
                {formatText(results[activeTab])}
              </div>
            ) : (
              <div className="ai-empty">
                <p>Click below or select a tab to load real-time financial insights.</p>
                <button
                  className="btn-generate"
                  onClick={() => fetchInsight(activeTabObj)}
                >
                  ✨ Generate Insights
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Right: Chat */}
        <div className="ai-chat-panel">
          <div className="ai-chat-header">
            <h3>💬 Ask Your AI Advisor</h3>
            <p>Ask anything about your transactions, budgets, goals, and savings</p>
          </div>

          <div className="ai-chat-messages">
            {chatHistory.length === 0 && (
              <div className="chat-suggestions">
                <p className="suggestions-title">Try asking:</p>
                {[
                  'How much did I spend on food this month?',
                  'Am I on track with my savings goals?',
                  'Which category should I cut back on?',
                  'How can I save more money?',
                ].map((s, i) => (
                  <button key={i} className="suggestion-chip" onClick={() => setQuestion(s)}>
                    {s}
                  </button>
                ))}
              </div>
            )}
            {chatHistory.map((msg, i) => (
              <div key={i} className={`chat-msg ${msg.role === 'user' ? 'chat-user' : 'chat-ai'}`}>
                <span className="chat-role">{msg.role === 'user' ? '👤 You' : botName}</span>
                <div className="chat-text">{formatText(msg.text)}</div>
              </div>
            ))}
            {chatLoading && (
              <div className="chat-msg chat-ai">
                <span className="chat-role">{botName}</span>
                <div className="chat-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            )}
          </div>

          <form className="ai-chat-form" onSubmit={handleAsk}>
            <input
              type="text"
              value={question}
              onChange={e => setQuestion(e.target.value)}
              placeholder="Ask about your finances..."
              disabled={chatLoading}
            />
            <button type="submit" disabled={chatLoading || !question.trim()}>
              Send
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

