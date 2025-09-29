import { useAuthStore } from '~/store/auth';
import { defineNuxtPlugin } from '#app';

export default defineNuxtPlugin(() => {
  const authStore = useAuthStore();
  
  // Load auth data from localStorage on app initialization
  authStore.loadFromStorage();
});