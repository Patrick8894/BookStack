import { defineStore } from 'pinia';
import { login, register } from '~/services/authService';

interface User {
  id: number;
  username: string;
  role: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isLoaded: boolean; // Add this to track if auth state is loaded
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
    isLoaded: false, // Initialize as false
  }),

  actions: {
    async login(username: string, password: string) {
      const data = await login(username, password);
      this.token = data.token;
      this.user = data.user;
      this.isLoaded = true;
      
      if (process.client) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
      }
    },

    async register(userData: any) {
      return await register(userData);
    },

    logout() {
      this.token = null;
      this.user = null;
      this.isLoaded = true;
      
      if (process.client) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    },

    loadFromStorage() {
      if (process.client && !this.isLoaded) {
        const token = localStorage.getItem('token');
        const user = localStorage.getItem('user');
        
        if (token && user) {
          this.token = token;
          try {
            this.user = JSON.parse(user);
          } catch (e) {
            localStorage.removeItem('user');
          }
        }
        this.isLoaded = true;
      }
    },
  },
});