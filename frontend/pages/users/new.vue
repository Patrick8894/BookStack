<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Add New User</h1>
      <v-btn variant="outlined" @click="navigateTo('/users')">
        Back to Users
      </v-btn>
    </div>

    <v-card>
      <v-card-text>
        <!-- Show validation errors -->
        <v-alert 
          v-if="validationErrors.length > 0" 
          type="error" 
          class="mb-4"
        >
          <ul>
            <li v-for="error in validationErrors" :key="error.field">
              {{ error.message }}
            </li>
          </ul>
        </v-alert>
        
        <UserForm
          :can-edit="true"
          :loading="loading"
          :validation-errors="validationErrors"
          @save="handleSave"
        />
      </v-card-text>
    </v-card>

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { createUser } from '~/services/userService';
import { useAuthStore } from '~/store/auth';
import { validateUser, type ValidationError } from '@/utils/user-validation';
import UserForm from '~/components/users/UserForm.vue';
import { navigateTo, createError } from '#app';

// Protect this route - only admins can add users
const auth = useAuthStore();
if (!auth.user || auth.user.role !== 'ADMIN') {
  throw createError({
    statusCode: 403,
    statusMessage: 'Access denied. Only admins can add users.'
  });
}

const loading = ref(false);
const error = ref('');
const validationErrors = ref<ValidationError[]>([]);

async function handleSave(formData: any) {
  // Clear previous errors
  validationErrors.value = [];
  error.value = '';
  
  // Validate the user data
  const errors = validateUser(formData, false); // false = isCreate
  
  if (errors.length > 0) {
    validationErrors.value = errors;
    return; // Don't save if there are validation errors
  }
  
  try {
    loading.value = true;
    
    const newUser = await createUser(formData);
    
    // Redirect to the new user's detail page
    await navigateTo(`/users/${newUser.id}`);
  } catch (err: any) {
    error.value = err.message || 'Failed to create user';
    loading.value = false;
  }
}
</script>