import axios from 'axios';
import { defineNuxtPlugin, useRuntimeConfig } from '#app';

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  
  const api = axios.create({
    baseURL: config.public.apiUrl as string,
    withCredentials: true, // Important for CORS with credentials
  });

  // Only run in browser environment
  if (process.client) {
    api.interceptors.request.use((config) => {
      const token = localStorage.getItem('token');
      if (token && config.url && !config.url.includes('/auth/')) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  return {
    provide: { api },
  };
});