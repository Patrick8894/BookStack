<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>{{ user?.username || 'Loading...' }}</h1>
      <div>
        <v-btn 
          variant="outlined" 
          @click="navigateTo('/users')"
          class="mr-2"
        >
          Back to Users
        </v-btn>
        <v-btn 
          v-if="canEdit && !editMode" 
          color="primary" 
          @click="editMode = true"
        >
          Edit User
        </v-btn>
        <v-btn 
          v-if="canEdit && editMode" 
          variant="outlined" 
          @click="cancelEdit"
        >
          Cancel
        </v-btn>
      </div>
    </div>

    <v-card v-if="user">
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
          :user="user" 
          :can-edit="canEdit && editMode"
          :validation-errors="validationErrors"
          :loading="loading"
          @save="handleSave" 
        />
      </v-card-text>
      
      <v-card-actions v-if="canDelete">
        <v-btn color="error" @click="handleDelete">Delete User</v-btn>
      </v-card-actions>
    </v-card>

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getUserById, updateUser, deleteUser } from '@/services/userService';
import { useAuthStore } from '@/store/auth';
import { validateUser, type ValidationError } from '@/utils/user-validation';
import UserForm from '../../components/users/UserForm.vue';
import { navigateTo } from '#app';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

interface User {
  id: number;
  username: string;
  role: string;
}

const user = ref<User | null>(null);
const loading = ref(false);
const error = ref('');
const editMode = ref(false);
const originalUser = ref<User | null>(null);
const validationErrors = ref<ValidationError[]>([]);

const canEdit = computed(() => auth.user?.role === 'ADMIN');
const canDelete = computed(() => auth.user?.role === 'ADMIN');

onMounted(async () => {
  await loadUser();
});

async function loadUser() {
  try {
    loading.value = true;
    error.value = '';
    const userId = parseInt(route.params.id as string);
    const userData = await getUserById(userId);
    user.value = userData;
    originalUser.value = { ...userData };
  } catch (err: any) {
    error.value = err.message || 'Failed to load user';
  } finally {
    loading.value = false;
  }
}

async function handleSave(formData: any) {
  // Clear previous errors
  validationErrors.value = [];
  
  // Validate the updated user data
  const errors = validateUser(formData, true); // true = isUpdate
  
  if (errors.length > 0) {
    validationErrors.value = errors;
    return; // Don't save if there are validation errors
  }
  
  try {
    loading.value = true;
    error.value = '';
    const userId = parseInt(route.params.id as string);
    
    const updatedUser = await updateUser(userId, formData);
    
    user.value = updatedUser;
    originalUser.value = { ...updatedUser };
    editMode.value = false;
    validationErrors.value = [];
    
    // Show success message
    alert('User updated successfully!');
  } catch (err: any) {
    error.value = err.message || 'Failed to update user';
  } finally {
    loading.value = false;
  }
}

function cancelEdit() {
  if (originalUser.value) {
    user.value = { ...originalUser.value };
  }
  editMode.value = false;
  error.value = '';
  validationErrors.value = [];
}

async function handleDelete() {
  if (confirm('Are you sure you want to delete this user?')) {
    try {
      loading.value = true;
      await deleteUser(user.value!.id);
      router.push('/users');
    } catch (err: any) {
      error.value = err.message || 'Failed to delete user';
      loading.value = false;
    }
  }
}
</script>