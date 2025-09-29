import { useAuthStore } from '~/store/auth';
import { defineNuxtRouteMiddleware, navigateTo } from '#app';

export default defineNuxtRouteMiddleware((to) => {
  // Skip authentication check during SSR to avoid hydration mismatch
  if (process.server) {
    return;
  }
  
  const auth = useAuthStore();
  
  // Initialize auth store from localStorage on client side
  if (process.client && !auth.token) {
    auth.loadFromStorage();
  }
  
  if (!auth.token && to.path !== '/' && to.path !== '/login' && to.path !== '/register') {
    console.log("Find no token, redirecting to login");
    return navigateTo('/login');
  }
});