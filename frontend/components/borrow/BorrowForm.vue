<template>
  <v-form @submit.prevent="submitForm" ref="formRef">
    <v-text-field
      v-model.number="form.userId"
      label="User ID"
      type="number"
      min="1"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('userId')"
      required
    />
    
    <v-text-field
      v-model.number="form.bookId"
      label="Book ID"
      type="number"
      min="1"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('bookId')"
      required
    />
    
    <v-textarea
      v-model="form.notes"
      label="Notes (Optional)"
      :readonly="!canEdit"
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('notes')"
      rows="3"
      counter="500"
    />

    <div class="d-flex gap-2 mt-4" v-if="canEdit">
      <v-btn type="submit" color="primary" :loading="loading">
        {{ formType === 'borrow' ? 'Borrow Book' : 'Return Book' }}
      </v-btn>
      <v-btn variant="outlined" @click="resetForm" :disabled="loading">
        Reset
      </v-btn>
    </div>
  </v-form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface BorrowData {
  userId: number | '';
  bookId: number | '';
  notes: string;
}

interface ValidationError {
  field: string;
  message: string;
}

const props = defineProps<{
  canEdit?: boolean;
  loading?: boolean;
  validationErrors?: ValidationError[];
  formType: 'borrow' | 'return';
  initialData?: Partial<BorrowData>;
}>();

const emit = defineEmits<{
  submit: [data: BorrowData];
}>();

const formRef = ref();
const form = ref<BorrowData>({
  userId: '',
  bookId: '',
  notes: '',
});

// Watch for initial data changes
watch(() => props.initialData, (newData) => {
  if (newData) {
    form.value = {
      userId: newData.userId || '',
      bookId: newData.bookId || '',
      notes: newData.notes || '',
    };
  }
}, { immediate: true, deep: true });

function getFieldErrors(fieldName: string): string[] {
  return props.validationErrors
    ?.filter(error => error.field === fieldName)
    ?.map(error => error.message) || [];
}

function resetForm() {
  if (props.initialData) {
    form.value = { ...props.initialData, notes: props.initialData.notes || '' };
  } else {
    form.value = {
      userId: '',
      bookId: '',
      notes: '',
    };
  }
}

function submitForm() {
  if (!props.canEdit) return;
  
  emit('submit', { ...form.value });
}
</script>