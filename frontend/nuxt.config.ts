// https://nuxt.com/docs/api/configuration/nuxt-config
import vuetify from 'vite-plugin-vuetify'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },
  modules: [
    '@pinia/nuxt',
  ],
  build: {
    transpile: ['vuetify'],
  },
  css: [
    'vuetify/styles',
    '@mdi/font/css/materialdesignicons.css'
  ],

  // Use vite-plugin-vuetify
  hooks: {
    'vite:extendConfig': (config) => {
      if (!config.plugins) config.plugins = []
      config.plugins.push(vuetify())
    },
  },
  ssr: false,
  runtimeConfig: {
    // Private keys (server only)
    
    // Public keys that are exposed to the client
    public: {
      apiUrl: process.env.NUXT_PUBLIC_API_URL || 'http://localhost:8080/api'
    }
  }
})
