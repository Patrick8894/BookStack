import { useAuthStore } from '@/store/auth';
import { defineNuxtRouteMiddleware, navigateTo } from '#app';

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();
  if (!auth.token && to.path !== '/' && to.path !== '/login' && to.path !== '/register') {
    return navigateTo('/login');
  }
});