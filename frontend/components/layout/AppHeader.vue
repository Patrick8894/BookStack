<template>
  <v-app-bar app color="primary" dark>
    <v-toolbar-title>📚 BookStack</v-toolbar-title>

    <v-spacer />

    <!-- Navigation Buttons -->
    <div class="d-flex align-center">
      <v-btn to="/" variant="text" class="mx-2" color="white">Home</v-btn>
      <v-btn to="/books" variant="text" class="mx-2" color="white">Books</v-btn>
      <v-btn
        v-if="auth.user?.role === 'ADMIN'"
        to="/users"
        variant="text"
        class="mx-2"
        color="white"
      >
        Users
      </v-btn>
      <v-btn
        v-if="auth.user?.role !== 'MEMBER'"
        to="/borrow"
        variant="text"
        class="mx-2"
        color="white"
      >
        Borrow
      </v-btn>
    </div>

    <v-spacer />

    <!-- Auth Buttons -->
    <div class="d-flex align-center">
      <template v-if="auth.token">
        <span class="mx-3">Hello, {{ auth.user?.username }}</span>
        <v-btn variant="outlined" color="white" @click="handleLogout">Logout</v-btn>
      </template>
      <template v-else>
        <v-btn to="/login" variant="outlined" color="white" class="mx-2">Login</v-btn>
        <v-btn to="/register" color="secondary" class="mx-2">Register</v-btn>
      </template>
    </div>
  </v-app-bar>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/store/auth'; // use ~/ instead of @
import { navigateTo } from '#app';

const auth = useAuthStore();

const handleLogout = () => {
  auth.logout();
  navigateTo('/login');
};
</script>
