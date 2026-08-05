'use client';
import { useEffect } from 'react';

export function BackendWakeup() {
  useEffect(() => {
    // Silently wake the HF Space on first page load
    fetch('/api/v1/actuator/health', { method: 'GET' }).catch(() => {});
  }, []);
  return null;
}