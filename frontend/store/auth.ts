import { defineStore } from 'pinia';
import { login, register } from '@/services/authService';

interface User {
  id: number;
  username: string;
  role: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
  }),

  actions: {
    async login(username: string, password: string) {
      const data = await login(username, password);
      this.token = data.token;
      this.user = data.user;
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
    },

    async register(userData: any) {
      return await register(userData);
    },

    logout() {
      this.token = null;
      this.user = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    },

    loadFromStorage() {
      const token = localStorage.getItem('token');
      const user = localStorage.getItem('user');
      if (token && user) {
        this.token = token;
        this.user = JSON.parse(user);
      }
    },
  },
});
