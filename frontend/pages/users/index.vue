<!-- frontend/pages/users/index.vue -->
<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>User Management</h1>
      <v-btn 
        v-if="canAddUser"
        color="primary" 
        @click="navigateTo('/users/new')"
      >
        Add New User
      </v-btn>
    </div>

    <!-- Summary Cards -->
    <v-row class="mb-4">
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Total Users</div>
            <div class="text-h4 text-primary">{{ users.length }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Admins</div>
            <div class="text-h4 text-error">{{ adminCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Librarians</div>
            <div class="text-h4 text-warning">{{ librarianCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Members</div>
            <div class="text-h4 text-success">{{ memberCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Search and Filters -->
    <v-card class="mb-4">
      <v-card-title>Search Users</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="6">
            <v-text-field 
              v-model="search" 
              label="Search by username" 
              clearable 
              density="compact"
              prepend-inner-icon="mdi-magnify"
              @keyup.enter="handleSearch"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="roleFilter"
              label="Filter by Role"
              :items="roleOptions"
              clearable
              density="compact"
              @update:model-value="handleRoleFilter"
            />
          </v-col>
          <v-col cols="12" md="3" class="d-flex align-center gap-2">
            <v-btn color="primary" @click="handleSearch" :loading="loading">
              Search
            </v-btn>
            <v-btn variant="outlined" @click="clearFilters">
              Clear
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Users Table -->
    <v-card>
      <v-card-title>System Users</v-card-title>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="filteredUsers"
          :loading="loading"
          class="elevation-1"
          density="compact"
          hover
          @click:row="goToUser"
        >
          <!-- Role column with color coding -->
          <template v-slot:item.role="{ item }">
            <v-chip
              :color="getRoleColor(item.role)"
              size="small"
              variant="flat"
            >
              {{ item.role }}
            </v-chip>
          </template>

          <!-- Username with icon -->
          <template v-slot:item.username="{ item }">
            <div class="d-flex align-center gap-2">
              <v-icon :color="getRoleColor(item.role)" size="small">
                {{ getRoleIcon(item.role) }}
              </v-icon>
              <span>{{ item.username }}</span>
            </div>
          </template>

          <!-- Actions column -->
          <template v-slot:item.actions="{ item }" v-if="canAddUser">
            <div class="d-flex gap-1">
              <v-btn
                color="primary"
                size="small"
                variant="outlined"
                @click.stop="navigateTo(`/users/${item.id}`)"
              >
                View
              </v-btn>
              <v-btn
                color="success"
                size="small"
                variant="outlined"
                @click.stop="navigateTo(`/users/${item.id}`)"
              >
                Edit
              </v-btn>
              <v-btn
                v-if="item.role !== 'ADMIN'"
                color="error"
                size="small"
                variant="outlined"
                @click.stop="handleQuickDelete(item)"
              >
                Delete
              </v-btn>
            </div>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Quick Delete Dialog -->
    <v-dialog v-model="showDeleteDialog" max-width="400px">
      <v-card>
        <v-card-title>Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete user "{{ selectedUser?.username }}"?
          This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="outlined" @click="showDeleteDialog = false">
            Cancel
          </v-btn>
          <v-btn 
            color="error" 
            @click="confirmDelete"
            :loading="deleteLoading"
          >
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
import { getAllUsers, searchUsersByUsername, deleteUser } from '@/services/userService';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from '#app';
import { createError } from 'h3';

// Protect this route
const auth = useAuthStore();
if (!auth.user || auth.user.role !== 'ADMIN') {
  throw createError({
    statusCode: 403,
    statusMessage: 'Access denied. Only admins can manage users.'
  });
}

const users = ref<any[]>([]);
const search = ref('');
const roleFilter = ref('');
const loading = ref(false);
const deleteLoading = ref(false);
const error = ref('');
const successMessage = ref('');
const showDeleteDialog = ref(false);
const selectedUser = ref<any>(null);

const canAddUser = computed(() => auth.user?.role === 'ADMIN');

// Statistics
const adminCount = computed(() => 
  users.value.filter(user => user.role === 'ADMIN').length
);

const librarianCount = computed(() => 
  users.value.filter(user => user.role === 'LIBRARIAN').length
);

const memberCount = computed(() => 
  users.value.filter(user => user.role === 'MEMBER').length
);

const roleOptions = [
  { title: 'Admin', value: 'ADMIN' },
  { title: 'Librarian', value: 'LIBRARIAN' },
  { title: 'Member', value: 'MEMBER' }
];

const filteredUsers = computed(() => {
  if (!roleFilter.value) return users.value;
  return users.value.filter(user => user.role === roleFilter.value);
});

const headers = computed(() => {
  const baseHeaders = [
    // { title: 'ID', key: 'id', sortable: true },
    { title: 'Username', key: 'username', sortable: true },
    { title: 'Role', key: 'role', sortable: true },
  ];

  if (canAddUser.value) {
    baseHeaders.push({ title: 'Actions', key: 'actions', sortable: false });
  }

  return baseHeaders;
});

onMounted(async () => {
  await loadUsers();
});

async function loadUsers() {
  try {
    loading.value = true;
    error.value = '';
    users.value = await getAllUsers();
  } catch (err: any) {
    error.value = err.message || 'Failed to load users';
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  try {
    loading.value = true;
    error.value = '';
    
    if (!search.value) {
      await loadUsers();
    } else {
      users.value = await searchUsersByUsername(search.value);
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to search users';
  } finally {
    loading.value = false;
  }
}

function handleRoleFilter() {
  // Filtering is handled by computed property
}

function clearFilters() {
  search.value = '';
  roleFilter.value = '';
  loadUsers();
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

function handleQuickDelete(user: any) {
  selectedUser.value = user;
  showDeleteDialog.value = true;
}

async function confirmDelete() {
  if (!selectedUser.value) return;
  
  try {
    deleteLoading.value = true;
    await deleteUser(selectedUser.value.id);
    
    showDeleteDialog.value = false;
    selectedUser.value = null;
    successMessage.value = `User "${selectedUser.value?.username}" deleted successfully!`;
    await loadUsers();
    
    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (err: any) {
    error.value = err.message || 'Failed to delete user';
  } finally {
    deleteLoading.value = false;
  }
}

function goToUser(event: PointerEvent, { item }: { item: any }) {
  navigateTo(`/users/${item.id}`);
}
</script>