<template>
  <v-container class="d-flex justify-center align-center fill-height">
    <v-card width="400">
      <v-card-title>Register</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="handleRegister">
          <v-text-field v-model="username" label="Username" required />
          <v-text-field v-model="password" label="Password" type="password" required />
          <v-btn type="submit" color="primary" block>Register</v-btn>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <NuxtLink to="/login">Already have an account? Login</NuxtLink>
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

const handleRegister = async () => {
  try {
    await auth.register({ username: username.value, password: password.value });
    alert('Registration successful, please login');
    navigateTo('/login');
  } catch (e) {
    alert('Registration failed');
  }
};
</script>
