<!-- frontend/pages/users/[id].vue -->
<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <div>
        <h1>{{ user?.username || 'Loading...' }}</h1>
        <div class="text-subtitle-1 text-grey" v-if="user">
          <v-chip 
            :color="getRoleColor(user.role)" 
            size="small" 
            variant="flat"
          >
            {{ user.role }}
          </v-chip>
        </div>
      </div>
      <div class="d-flex gap-2">
        <v-btn 
          variant="outlined" 
          @click="navigateTo('/users')"
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

    <!-- User Status Card -->
    <v-card class="mb-4" v-if="user && !editMode">
      <v-card-title>User Information</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="3">
            <div class="text-center">
              <v-avatar 
                :color="getRoleColor(user.role)" 
                size="96"
                class="mb-3"
              >
                <v-icon size="48" color="white">
                  {{ getRoleIcon(user.role) }}
                </v-icon>
              </v-avatar>
              <div class="text-h6">{{ user.username }}</div>
              <v-chip 
                :color="getRoleColor(user.role)" 
                size="small" 
                variant="flat"
              >
                {{ user.role }}
              </v-chip>
            </div>
          </v-col>
          <v-col cols="12" md="9">
            <v-row>
              <v-col cols="6" md="4">
                <div class="text-subtitle-2 text-grey">User ID</div>
                <div class="text-body-1">#{{ user.id }}</div>
              </v-col>
              <v-col cols="6" md="4">
                <div class="text-subtitle-2 text-grey">Role</div>
                <div class="text-body-1">{{ user.role }}</div>
              </v-col>
              <v-col cols="6" md="4">
                <div class="text-subtitle-2 text-grey">Status</div>
                <div class="text-body-1">
                  <v-chip size="small" color="success" variant="flat">
                    Active
                  </v-chip>
                </div>
              </v-col>
            </v-row>
            
            <v-divider class="my-4" />
            
            <div class="text-subtitle-2 text-grey mb-2">Permissions</div>
            <div class="d-flex flex-wrap gap-2">
              <v-chip 
                v-for="permission in getUserPermissions(user.role)" 
                :key="permission"
                size="small" 
                variant="outlined"
              >
                {{ permission }}
              </v-chip>
            </div>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- User Form Card -->
    <v-card v-if="user">
      <v-card-title>{{ editMode ? 'Edit User Details' : 'User Details' }}</v-card-title>
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
      
      <v-card-actions v-if="canDelete && !editMode && user.role !== 'ADMIN'">
        <v-spacer />
        <v-btn 
          color="error" 
          variant="outlined"
          @click="handleDelete"
        >
          Delete User
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>

    <v-alert v-if="successMessage" type="success" class="mt-4">
      {{ successMessage }}
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
const successMessage = ref('');
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
  error.value = '';
  successMessage.value = '';
  
  // Validate the user data
  const errors = validateUser(formData, true); // true = isUpdate
  
  if (errors.length > 0) {
    validationErrors.value = errors;
    return; // Don't save if there are validation errors
  }
  
  try {
    loading.value = true;
    const userId = parseInt(route.params.id as string);
    const updatedUser = await updateUser(userId, formData);
    user.value = updatedUser;
    originalUser.value = { ...updatedUser };
    editMode.value = false;
    validationErrors.value = [];
    successMessage.value = 'User updated successfully!';
    
    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
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
  if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
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

function getRoleColor(role: string): string {
  switch (role) {
    case 'ADMIN': return 'error';
    case 'LIBRARIAN': return 'warning';
    case 'MEMBER': return 'success';
    default: return 'grey';
  }
}

function getRoleIcon(role: string): string {
  switch (role) {
    case 'ADMIN': return 'mdi-shield-crown';
    case 'LIBRARIAN': return 'mdi-book-account';
    case 'MEMBER': return 'mdi-account';
    default: return 'mdi-account-question';
  }
}

function getUserPermissions(role: string): string[] {
  switch (role) {
    case 'ADMIN':
      return ['Manage Users', 'Manage Books', 'Manage Borrows', 'System Settings'];
    case 'LIBRARIAN':
      return ['Manage Books', 'Manage Borrows', 'View Reports'];
    case 'MEMBER':
      return ['View Books', 'View Own Borrows'];
    default:
      return [];
  }
}
</script>