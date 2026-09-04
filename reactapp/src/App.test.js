import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

test('renders application brand title', () => {
  render(<App />);
  const brandTitles = screen.getAllByText(/Personal Finance and Budget Management Application/i);
  expect(brandTitles.length).toBeGreaterThanOrEqual(1);
  expect(brandTitles[0]).toBeInTheDocument();
});

test('renders public navigation links', () => {
  render(<App />);
  expect(screen.getByRole('link', { name: /^Home$/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /^Login$/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /^Register$/i })).toBeInTheDocument();
});

test('renders footer copyright text', () => {
  render(<App />);
  const footer = screen.getByText(/© 2024 Personal Finance and Budget Management Application/i);
  expect(footer).toBeInTheDocument();
});
