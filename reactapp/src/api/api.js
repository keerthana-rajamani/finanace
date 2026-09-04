import axios from 'axios';

const LIVE_TUNNEL_URL = 'https://warrior-tours-discs-systematic.trycloudflare.com';

const getBaseUrl = () => {
  if (process.env.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  if (typeof window !== 'undefined' && window.localStorage && localStorage.getItem('backend_url')) {
    return localStorage.getItem('backend_url');
  }
  if (typeof window !== 'undefined' && window.location) {
    const { hostname, port, origin } = window.location;
    if (hostname.includes('github.io')) {
      return `${LIVE_TUNNEL_URL}/api`;
    }
    if ((hostname === 'localhost' || hostname === '127.0.0.1') && (port === '8081' || port === '3000')) {
      return 'http://localhost:8080/api';
    }
    return `${origin}/api`;
  }
  return '/api';
};

const api = axios.create({
  baseURL: getBaseUrl(),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
