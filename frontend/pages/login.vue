<template>
  <v-container class="d-flex justify-center align-center fill-height">
    <v-card width="400">
      <v-card-title>Login</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="handleLogin">
          <v-text-field v-model="username" label="Username" required />
          <v-text-field v-model="password" label="Password" type="password" required />
          <v-btn type="submit" color="primary" block>Login</v-btn>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <NuxtLink to="/register">Don't have an account? Register</NuxtLink>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from '#app';

const username = ref('');
const password = ref('');
const auth = useAuthStore();

const handleLogin = async () => {
  try {
    await auth.login(username.value, password.value);
    navigateTo('/');
  } catch (e) {
    alert('Login failed');
  }
};
</script>
