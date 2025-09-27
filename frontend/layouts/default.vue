<template>
  <v-app>
    <!-- Header -->
    <AppHeader />

    <!-- Main Content -->
    <v-main>
      <NuxtPage />
    </v-main>

    <!-- Footer -->
    <AppFooter />
  </v-app>
</template>

<script setup lang="ts">
import AppHeader from '../components/layout/AppHeader.vue';
import AppFooter from '../components/layout/AppFooter.vue';
</script>
