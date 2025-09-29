<!-- frontend/components/layout/AppHeader.vue -->
<template>
  <v-app-bar app color="primary" dark elevation="4">
    <!-- Logo/Brand -->
    <v-btn 
      to="/" 
      variant="text" 
      class="text-h6 font-weight-bold"
      style="text-transform: none;"
    >
      📚 BookStack
    </v-btn>

    <v-spacer />

    <!-- Desktop Navigation -->
    <div class="d-none d-md-flex align-center">
      <v-btn to="/" variant="text" class="mx-1">
        <v-icon>mdi-home</v-icon>
        <span class="ml-2">Home</span>
      </v-btn>
      
      <v-btn to="/books" variant="text" class="mx-1">
        <v-icon>mdi-book-search</v-icon>
        <span class="ml-2">Books</span>
      </v-btn>
      
      <!-- Admin Navigation -->
      <v-btn
        v-if="auth.user?.role === 'ADMIN'"
        to="/users"
        variant="text"
        class="mx-1"
      >
        <v-icon>mdi-account-multiple</v-icon>
        <span class="ml-2">Users</span>
      </v-btn>
      
      <!-- Admin & Librarian Navigation -->
      <v-btn
        v-if="canManage"
        to="/borrow"
        variant="text"
        class="mx-1"
      >
        <v-icon>mdi-book-account</v-icon>
        <span class="ml-2">Manage</span>
      </v-btn>
      
      <!-- Member Navigation -->
      <v-btn
        v-if="auth.user?.role === 'MEMBER'"
        to="/my-borrow"
        variant="text"
        class="mx-1"
      >
        <v-icon>mdi-account-clock</v-icon>
        <span class="ml-2">My Books</span>
      </v-btn>
    </div>

    <v-spacer />

    <!-- User Section -->
    <div class="d-flex align-center">
      <template v-if="auth.token">
        <!-- User Info -->
        <div class="d-none d-sm-flex align-center mr-4">
          <v-avatar size="32" :color="getRoleColor(auth.user?.role)" class="mr-2">
            <v-icon size="20" color="white">
              {{ getRoleIcon(auth.user?.role) }}
            </v-icon>
          </v-avatar>
          <div class="d-none d-md-block">
            <div class="text-caption">{{ auth.user?.username }}</div>
            <div class="text-caption opacity-75">{{ auth.user?.role }}</div>
          </div>
        </div>
        
        <!-- Logout Button -->
        <v-btn variant="outlined" color="white" @click="handleLogout">
          <v-icon>mdi-logout</v-icon>
          <span class="ml-2 d-none d-sm-inline">Logout</span>
        </v-btn>
      </template>
      
      <template v-else>
        <v-btn to="/login" variant="outlined" color="white" class="mr-2">
          <v-icon>mdi-login</v-icon>
          <span class="ml-2">Login</span>
        </v-btn>
        <v-btn to="/register" color="secondary">
          <v-icon>mdi-account-plus</v-icon>
          <span class="ml-2 d-none d-sm-inline">Sign Up</span>
        </v-btn>
      </template>
    </div>

    <!-- Mobile Menu -->
    <v-btn 
      v-if="auth.user"
      icon="mdi-menu" 
      class="d-md-none ml-2"
      @click="showMobileMenu = !showMobileMenu"
    />

    <!-- Mobile Navigation Drawer -->
    <v-navigation-drawer
      v-model="showMobileMenu"
      temporary
      location="right"
      width="280"
      class="d-md-none"
    >
      <v-list>
        <v-list-item 
          v-if="auth.user"
          class="bg-primary text-white mb-2"
        >
          <template v-slot:prepend>
            <v-avatar :color="getRoleColor(auth.user?.role)">
              <v-icon color="white">
                {{ getRoleIcon(auth.user?.role) }}
              </v-icon>
            </v-avatar>
          </template>
          <v-list-item-title>{{ auth.user?.username }}</v-list-item-title>
          <v-list-item-subtitle class="text-white opacity-75">
            {{ auth.user?.role }}
          </v-list-item-subtitle>
        </v-list-item>

        <v-list-item to="/" @click="showMobileMenu = false">
          <template v-slot:prepend>
            <v-icon>mdi-home</v-icon>
          </template>
          <v-list-item-title>Home</v-list-item-title>
        </v-list-item>

        <v-list-item to="/books" @click="showMobileMenu = false">
          <template v-slot:prepend>
            <v-icon>mdi-book-search</v-icon>
          </template>
          <v-list-item-title>Books</v-list-item-title>
        </v-list-item>

        <v-list-item 
          v-if="auth.user?.role === 'ADMIN'"
          to="/users" 
          @click="showMobileMenu = false"
        >
          <template v-slot:prepend>
            <v-icon>mdi-account-multiple</v-icon>
          </template>
          <v-list-item-title>Users</v-list-item-title>
        </v-list-item>

        <v-list-item 
          v-if="canManage"
          to="/borrow" 
          @click="showMobileMenu = false"
        >
          <template v-slot:prepend>
            <v-icon>mdi-book-account</v-icon>
          </template>
          <v-list-item-title>Manage Borrows</v-list-item-title>
        </v-list-item>

        <v-list-item 
          v-if="auth.user?.role === 'MEMBER'"
          to="/my-borrow" 
          @click="showMobileMenu = false"
        >
          <template v-slot:prepend>
            <v-icon>mdi-account-clock</v-icon>
          </template>
          <v-list-item-title>My Books</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>
  </v-app-bar>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAuthStore } from '~/store/auth';
import { navigateTo } from '#app';

const auth = useAuthStore();
const showMobileMenu = ref(false);

const canManage = computed(() => 
  auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN'
);

const handleLogout = () => {
  auth.logout();
  showMobileMenu.value = false;
  navigateTo('/login');
};

function getRoleColor(role?: string): string {
  switch (role) {
    case 'ADMIN': return 'red';
    case 'LIBRARIAN': return 'orange';
    case 'MEMBER': return 'green';
    default: return 'grey';
  }
}

function getRoleIcon(role?: string): string {
  switch (role) {
    case 'ADMIN': return 'mdi-shield-crown';
    case 'LIBRARIAN': return 'mdi-book-account';
    case 'MEMBER': return 'mdi-account';
    default: return 'mdi-account-question';
  }
}
</script>