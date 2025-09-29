<template>
  <v-form @submit.prevent="submitForm" ref="formRef">
    <v-text-field
      v-model="form.username"
      label="Username"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('username')"
      required
      counter="50"
    />
    
    <v-text-field
      v-model="form.password"
      label="Password"
      type="password"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('password')"
      persistent-hint
    />
    
    <v-select
      v-model="form.role"
      label="Role"
      :items="roleOptions"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('role')"
      required
    />
    
    <div class="d-flex gap-2 mt-4" v-if="canEdit">
      <v-btn type="submit" color="primary" :loading="loading">
        {{ isUpdate ? 'Update User' : 'Create User' }}
      </v-btn>
      <v-btn variant="outlined" @click="resetForm" :disabled="loading">
        Reset
      </v-btn>
    </div>
  </v-form>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { ValidationError } from '@/utils/user-validation';

interface UserData {
  id?: number;
  username: string;
  password?: string;
  role: string;
}

const props = defineProps<{
  user?: UserData;
  canEdit?: boolean;
  loading?: boolean;
  validationErrors?: ValidationError[];
}>();

const emit = defineEmits<{
  save: [user: Omit<UserData, 'id'>];
}>();

const formRef = ref();
const form = ref<UserData>({
  username: '',
  password: '',
  role: 'MEMBER'
});

const roleOptions = [
  { title: 'Admin', value: 'ADMIN' },
  { title: 'Librarian', value: 'LIBRARIAN' },
  { title: 'Member', value: 'MEMBER' }
];

const isUpdate = computed(() => !!props.user?.id);

// Watch for user changes and update form
watch(() => props.user, (newUser) => {
  if (newUser) {
    form.value = {
      id: newUser.id,
      username: newUser.username || '',
      password: '', // Always start empty for updates
      role: newUser.role || 'MEMBER'
    };
  } else {
    // Reset form for new user
    resetForm();
  }
}, { immediate: true, deep: true });

function getFieldErrors(fieldName: string): string[] {
  return props.validationErrors
    ?.filter(error => error.field === fieldName)
    ?.map(error => error.message) || [];
}

function resetForm() {
  if (props.user) {
    // Reset to original user data
    form.value = {
      ...props.user,
      password: '' // Always clear password
    };
  } else {
    // Reset to empty form for new user
    form.value = {
      username: '',
      password: '',
      role: 'MEMBER'
    };
  }
}

function submitForm() {
  if (!props.canEdit) return;
  
  // Create the update object, excluding id and empty password for updates
  const userData: any = {
    username: form.value.username,
    role: form.value.role
  };
  
  // Only include password if it's provided
  if (form.value.password && form.value.password.trim() !== '') {
    userData.password = form.value.password;
  }
  
  emit('save', userData);
}
</script>