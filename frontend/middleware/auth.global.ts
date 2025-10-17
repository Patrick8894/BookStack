import { useAuthStore } from '~/store/auth';
import { defineNuxtRouteMiddleware, navigateTo } from '#app';

export default defineNuxtRouteMiddleware((to) => {
  // Skip authentication check during SSR to avoid hydration mismatch
  if (import.meta.server) {
    return;
  }
  
  const auth = useAuthStore();
  
  // Initialize auth store from localStorage on client side
  if (import.meta.client && !auth.token) {
    auth.loadFromStorage();
  }

  if (auth.token && (to.path === '/login' || to.path === '/register')) {
    console.log("Already logged in, redirecting to /");
    return navigateTo('/');
  }
  
  if (!auth.token && to.path !== '/' && to.path !== '/login' && to.path !== '/register') {
    console.log("Find no token, redirecting to login");
    return navigateTo('/login');
  }
});