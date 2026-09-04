import React, { useState } from 'react';
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

  const fetchInsight = async (tab) => {
    if (results[tab.key]) return;
    setLoading(prev => ({ ...prev, [tab.key]: true }));
    try {
      const res = await api.get(tab.endpoint);
      setResults(prev => ({ ...prev, [tab.key]: res.data[tab.field] }));
    } catch (err) {
      setResults(prev => ({ ...prev, [tab.key]: 'Failed to load insights. Please check your API key and try again.' }));
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
      if (!line.trim()) return <br key={i} />;
      if (/^\d+\./.test(line.trim())) {
        return <p key={i} className="insight-point">{line}</p>;
      }
      if (line.startsWith('===') || line.startsWith('**')) {
        return <p key={i} className="insight-heading">{line.replace(/\*\*/g, '').replace(/===/g, '').trim()}</p>;
      }
      return <p key={i} className="insight-line">{line}</p>;
    });
  };

  const activeTabObj = TABS.find(t => t.key === activeTab);

  return (
    <div className="ai-page">
      <div className="ai-header">
        <h2>🤖 AI Financial Advisor</h2>
        <p>Powered by Google Gemini — personalised insights based on your actual financial data</p>
      </div>

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
                <p>Gemini is analysing your financial data...</p>
              </div>
            ) : results[activeTab] ? (
              <div className="ai-result">
                {formatText(results[activeTab])}
              </div>
            ) : (
              <div className="ai-empty">
                <p>Click the tab to load AI-powered insights for this section.</p>
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
            <p>Ask anything about your finances</p>
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
                <span className="chat-role">{msg.role === 'user' ? '👤 You' : '🤖 Gemini'}</span>
                <div className="chat-text">{formatText(msg.text)}</div>
              </div>
            ))}
            {chatLoading && (
              <div className="chat-msg chat-ai">
                <span className="chat-role">🤖 Gemini</span>
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
