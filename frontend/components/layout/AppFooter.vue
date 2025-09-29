<!-- frontend/components/layout/AppFooter.vue -->
<template>
  <v-footer class="footer" color="grey-darken-4" dark>
    <v-container>
      <v-row>
        <!-- Brand Section -->
        <v-col cols="12" md="4" class="text-center text-md-left">
          <div class="d-flex align-center justify-center justify-md-start mb-3">
            <v-icon size="32" class="mr-2">mdi-book-open-page-variant</v-icon>
            <span class="text-h6 font-weight-bold">BookStack</span>
          </div>
          <p class="text-body-2 mb-0">
            Your digital library management system for the modern age.
          </p>
        </v-col>

        <!-- Quick Links -->
        <v-col cols="12" md="4" class="text-center">
          <h3 class="text-subtitle-1 font-weight-bold mb-3">Quick Links</h3>
          <div class="d-flex flex-column gap-2">
            <v-btn variant="text" color="white" size="small" to="/">
              Home
            </v-btn>
            <v-btn variant="text" color="white" size="small" to="/books">
              Browse Books
            </v-btn>
            <v-btn 
              v-if="!auth.user"
              variant="text" 
              color="white" 
              size="small" 
              to="/login"
            >
              Login
            </v-btn>
          </div>
        </v-col>

        <!-- Contact Info -->
        <v-col cols="12" md="4" class="text-center text-md-right">
          <h3 class="text-subtitle-1 font-weight-bold mb-3">Contact</h3>
          <div class="d-flex flex-column gap-2">
            <div class="d-flex align-center justify-center justify-md-end">
              <v-icon size="16" class="mr-2">mdi-email</v-icon>
              <span class="text-body-2">support@bookstack.com</span>
            </div>
            <div class="d-flex align-center justify-center justify-md-end">
              <v-icon size="16" class="mr-2">mdi-phone</v-icon>
              <span class="text-body-2">+1 (555) 123-4567</span>
            </div>
          </div>
        </v-col>
      </v-row>

      <v-divider class="my-4" color="grey-darken-2" />

      <!-- Bottom Row -->
      <v-row>
        <v-col cols="12" md="6" class="text-center text-md-left">
          <span class="text-body-2">
            © {{ currentYear }} BookStack. All rights reserved.
          </span>
        </v-col>
        <v-col cols="12" md="6" class="text-center text-md-right">
          <div class="d-flex justify-center justify-md-end gap-2">
            <v-btn icon size="small" variant="text" color="white">
              <v-icon>mdi-github</v-icon>
            </v-btn>
            <v-btn icon size="small" variant="text" color="white">
              <v-icon>mdi-twitter</v-icon>
            </v-btn>
            <v-btn icon size="small" variant="text" color="white">
              <v-icon>mdi-linkedin</v-icon>
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-footer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '~/store/auth';

const auth = useAuthStore();

const currentYear = computed(() => new Date().getFullYear());
</script>

<style scoped>
.footer {
  margin-top: auto;
}
</style>