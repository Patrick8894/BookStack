<!-- frontend/pages/index.vue -->
<template>
  <div>
    <!-- Hero Section -->
    <section class="hero-section">
      <v-container>
        <v-row align="center" justify="center" class="text-center">
          <v-col cols="12" md="8">
            <v-icon size="80" color="white" class="mb-4">
              mdi-book-open-page-variant
            </v-icon>
            <h1 class="display-1 font-weight-bold mb-4 text-white">
              Welcome to BookStack
            </h1>
            <p class="text-h6 mb-6 text-white" style="opacity: 0.9;">
              Your digital library management system
            </p>
            <div class="d-flex flex-column flex-md-row gap-4 justify-center">
              <v-btn 
                size="large" 
                color="white" 
                variant="flat"
                @click="navigateTo('/books')"
                prepend-icon="mdi-book-search"
              >
                Browse Books
              </v-btn>
              <v-btn 
                v-if="!auth.user"
                size="large" 
                variant="outlined" 
                color="white"
                @click="navigateTo('/login')"
              >
                Get Started
              </v-btn>
            </div>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <!-- Quick Access Cards -->
    <v-container class="py-12">
      <v-row justify="center">
        <v-col cols="12" class="text-center mb-8">
          <h2 class="text-h4 font-weight-bold text-grey-darken-2">
            {{ auth.user ? 'Quick Access' : 'What You Can Do' }}
          </h2>
        </v-col>
      </v-row>

      <v-row>
        <!-- Browse Books Card -->
        <v-col cols="12" md="4">
          <v-card 
            class="action-card text-center" 
            elevation="4" 
            hover
            @click="navigateTo('/books')"
          >
            <v-card-text class="pa-8">
              <v-icon size="64" color="primary" class="mb-4">
                mdi-book-search
              </v-icon>
              <h3 class="text-h6 mb-3">Browse Books</h3>
              <p class="text-body-2 text-grey-darken-1">
                Explore our collection and find your next great read
              </p>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- My Borrows Card (Members) -->
        <v-col cols="12" md="4" v-if="auth.user?.role === 'MEMBER'">
          <v-card 
            class="action-card text-center" 
            elevation="4" 
            hover
            @click="navigateTo('/my-borrow')"
          >
            <v-card-text class="pa-8">
              <v-icon size="64" color="success" class="mb-4">
                mdi-account-clock
              </v-icon>
              <h3 class="text-h6 mb-3">My Borrows</h3>
              <p class="text-body-2 text-grey-darken-1">
                Check your borrowed books and due dates
              </p>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Manage Borrows Card (Admin/Librarian) -->
        <v-col cols="12" md="4" v-if="canManage">
          <v-card 
            class="action-card text-center" 
            elevation="4" 
            hover
            @click="navigateTo('/borrow')"
          >
            <v-card-text class="pa-8">
              <v-icon size="64" color="warning" class="mb-4">
                mdi-book-account
              </v-icon>
              <h3 class="text-h6 mb-3">Manage Library</h3>
              <p class="text-body-2 text-grey-darken-1">
                Handle borrowing, returns, and book management
              </p>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Users Management Card (Admin only) -->
        <v-col cols="12" md="4" v-if="auth.user?.role === 'ADMIN'">
          <v-card 
            class="action-card text-center" 
            elevation="4" 
            hover
            @click="navigateTo('/users')"
          >
            <v-card-text class="pa-8">
              <v-icon size="64" color="error" class="mb-4">
                mdi-account-multiple
              </v-icon>
              <h3 class="text-h6 mb-3">Manage Users</h3>
              <p class="text-body-2 text-grey-darken-1">
                Add and manage system users and permissions
              </p>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Easy Borrowing Card (for guests) -->
        <v-col cols="12" md="4" v-if="!auth.user">
          <v-card class="action-card text-center" elevation="4">
            <v-card-text class="pa-8">
              <v-icon size="64" color="success" class="mb-4">
                mdi-book-check
              </v-icon>
              <h3 class="text-h6 mb-3">Easy Borrowing</h3>
              <p class="text-body-2 text-grey-darken-1">
                Simple book borrowing with automated due date tracking
              </p>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Smart Management Card (for guests) -->
        <v-col cols="12" md="4" v-if="!auth.user">
          <v-card class="action-card text-center" elevation="4">
            <v-card-text class="pa-8">
              <v-icon size="64" color="warning" class="mb-4">
                mdi-chart-line
              </v-icon>
              <h3 class="text-h6 mb-3">Smart Management</h3>
              <p class="text-body-2 text-grey-darken-1">
                Advanced tools for librarians and administrators
              </p>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <!-- Welcome Message for Users -->
    <v-container class="py-8" v-if="auth.user">
      <v-row justify="center">
        <v-col cols="12" md="8">
          <v-card color="primary" class="text-center" elevation="4">
            <v-card-text class="pa-8 text-white">
              <v-icon size="48" color="white" class="mb-4">
                mdi-account-circle
              </v-icon>
              <h3 class="text-h5 mb-3">
                Welcome back, {{ auth.user.username }}!
              </h3>
              <p class="text-body-1" style="opacity: 0.9;">
                You're logged in as {{ auth.user.role.toLowerCase() }}. 
                {{ getRoleDescription(auth.user.role) }}
              </p>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <!-- Call to Action for Guests -->
    <v-container class="py-8" v-if="!auth.user">
      <v-row justify="center">
        <v-col cols="12" md="8">
          <v-card class="text-center" elevation="4">
            <v-card-text class="pa-8">
              <h3 class="text-h5 mb-4 text-primary">
                Ready to Get Started?
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-6">
                Join BookStack today and start managing your library experience
              </p>
              <div class="d-flex flex-column flex-md-row gap-4 justify-center">
                <v-btn 
                  size="large" 
                  color="primary" 
                  @click="navigateTo('/register')"
                >
                  Sign Up Now
                </v-btn>
                <v-btn 
                  size="large" 
                  variant="outlined" 
                  color="primary"
                  @click="navigateTo('/login')"
                >
                  Login
                </v-btn>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '~/store/auth';
import { navigateTo } from '#app';

const auth = useAuthStore();

const canManage = computed(() => 
  auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN'
);

function getRoleDescription(role: string): string {
  switch (role) {
    case 'ADMIN':
      return 'You have full access to all system features.';
    case 'LIBRARIAN':
      return 'You can manage books and borrowing activities.';
    case 'MEMBER':
      return 'You can browse and borrow books from our collection.';
    default:
      return '';
  }
}
</script>

<style scoped>
.hero-section {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
  min-height: 60vh;
  display: flex;
  align-items: center;
}

.action-card {
  transition: all 0.3s ease;
  cursor: pointer;
  height: 100%;
}

.action-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.15);
}

@media (max-width: 960px) {
  .hero-section {
    min-height: 50vh;
  }
}
</style>