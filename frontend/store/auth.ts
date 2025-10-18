import { defineStore } from 'pinia';
import { login, register } from '~/services/authService';
import { navigateTo } from '#app';

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

  getters: {
    isTokenExpired(): boolean {
    if (!this.token) return true;
    
    try {
      const tokenParts = this.token.split('.');
      // Ensure we have a valid JWT structure (3 parts)
      if (tokenParts.length !== 3 || !tokenParts[1]) {
        return true;
      }
      
      const payload = JSON.parse(atob(tokenParts[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp < currentTime;
    } catch (error) {
      return true;
    }
  },

    isAuthenticated(): boolean {
      return !!this.user && !!this.token && !this.isTokenExpired;
    }
  },

  actions: {
    async login(username: string, password: string) {
      const data = await login(username, password);
      this.token = data.token;
      this.user = data.user;
      this.isLoaded = true;

      if (import.meta.client) {
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
      
      if (import.meta.client) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    },

    checkTokenValidity() {
      if (this.token && this.isTokenExpired) {
        console.log('Token expired, logging out...');
        this.logout();
        navigateTo('/login');
      }
    },

    loadFromStorage() {
      if (import.meta.client && !this.isLoaded) {
        const token = localStorage.getItem('token');
        const user = localStorage.getItem('user');
        
        if (token && user) {
          this.token = token;
          try {
            this.user = JSON.parse(user);
            this.checkTokenValidity();
          } catch (e) {
            localStorage.removeItem('user');
            localStorage.removeItem('token');
          }
        }
        this.isLoaded = true;
      }
    },
  },
});