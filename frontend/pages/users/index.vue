<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Users Management</h1>
      <v-btn 
        v-if="canAddUser"
        color="primary" 
        @click="navigateTo('/users/new')"
      >
        Add New User
      </v-btn>
    </div>

    <v-card>
      <v-card-text>
        <v-text-field 
          v-model="search" 
          label="Search by username" 
          @keyup.enter="handleSearch" 
          clearable 
          class="mb-4"
        />
        <v-btn color="primary" @click="handleSearch" class="mr-2">Search</v-btn>
      </v-card-text>
    </v-card>

    <v-data-table
      :headers="headers"
      :items="users"
      class="mt-4"
      density="compact"
      hover
      @click:row="goToUser"
    />

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getAllUsers, searchUsersByUsername } from '@/services/userService';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from '#app';

const auth = useAuthStore();
const users = ref<any[]>([]);
const search = ref('');
const error = ref('');

const canAddUser = computed(() => auth.user?.role === 'ADMIN');

const headers = [
  { title: 'ID', key: 'id' },
  { title: 'Username', key: 'username' },
  { title: 'Role', key: 'role' },
];

async function loadUsers() {
  try {
    error.value = '';
    users.value = await getAllUsers();
  } catch (err: any) {
    error.value = err.message || 'Failed to load users';
  }
}

async function handleSearch() {
  try {
    error.value = '';
    if (!search.value) {
      await loadUsers();
    } else {
      users.value = await searchUsersByUsername(search.value);
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to search users';
  }
}

function goToUser(event: PointerEvent, { item }: { item: any }) {
  navigateTo(`/users/${item.id}`);
}

onMounted(loadUsers);
</script>