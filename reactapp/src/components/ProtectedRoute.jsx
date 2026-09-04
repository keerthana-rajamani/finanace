import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children }) {
  const { auth } = useAuth();
  const location = useLocation();
  return auth ? children : <Navigate to="/login" state={{ from: location }} replace />;
}
