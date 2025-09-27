import { AxiosInstance } from 'axios';

declare module '#app' {
  interface NuxtApp {
    $api: AxiosInstance;
  }
}

// For Nuxt Auto-imports
declare module 'nuxt/dist/app/nuxt' {
  interface NuxtApp {
    $api: AxiosInstance;
  }
}

declare module 'vuetify/styles';