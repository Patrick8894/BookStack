<template>
  <v-form @submit.prevent="submitForm" ref="formRef">
    <v-text-field 
      v-model="form.title" 
      label="Title" 
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('title')"
      required 
      counter="200"
    />
    
    <v-text-field 
      v-model="form.author" 
      label="Author" 
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('author')"
      required 
      counter="100"
    />
    
    <v-text-field 
      v-model="form.isbn" 
      label="ISBN" 
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('isbn')"
      required
      hint="10 or 13 digits (hyphens optional)"
      persistent-hint
    />
    
    <v-text-field 
      v-model="form.category" 
      label="Category" 
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('category')"
      required 
      counter="50"
    />
    
    <v-text-field 
      v-model="form.language" 
      label="Language" 
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('language')"
      required 
      counter="30"
    />
    
    <v-text-field 
      v-model.number="form.totalCopies" 
      label="Total Copies" 
      type="number" 
      min="0"
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('totalCopies')"
      required
    />
    
    <v-text-field 
      v-model.number="form.availableCopies" 
      label="Available Copies" 
      type="number" 
      min="0"
      :readonly="!canEdit" 
      :variant="canEdit ? 'outlined' : 'plain'"
      :error-messages="getFieldErrors('availableCopies')"
      required
    />

    <div class="d-flex gap-2 mt-4" v-if="canEdit">
      <v-btn type="submit" color="primary" :loading="loading">
        {{ isNewBook ? 'Create Book' : 'Update Book' }}
      </v-btn>
      <v-btn variant="outlined" @click="resetForm" :disabled="loading">
        Reset
      </v-btn>
    </div>
  </v-form>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { BookValidationError } from '@/utils/book-validation';

interface BookData {
  id?: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  language: string;
  totalCopies: number;
  availableCopies: number;
}

const props = defineProps<{
  book?: BookData;
  canEdit?: boolean;
  loading?: boolean;
  validationErrors?: BookValidationError[];
}>();

const emit = defineEmits<{
  save: [book: Omit<BookData, 'id'>];
}>();

const formRef = ref();
const form = ref<BookData>({
  title: '',
  author: '',
  isbn: '',
  category: '',
  language: '',
  totalCopies: 1,
  availableCopies: 1,
});

const isNewBook = computed(() => !props.book?.id);

// Watch for book changes and update form
watch(() => props.book, (newBook) => {
  if (newBook) {
    form.value = {
      id: newBook.id,
      title: newBook.title || '',
      author: newBook.author || '',
      isbn: newBook.isbn || '',
      category: newBook.category || '',
      language: newBook.language || '',
      totalCopies: newBook.totalCopies || 1,
      availableCopies: newBook.availableCopies || 1,
    };
  } else {
    // Reset form for new book
    resetForm();
  }
}, { immediate: true, deep: true });

function getFieldErrors(fieldName: string): string[] {
  return props.validationErrors
    ?.filter(error => error.field === fieldName)
    ?.map(error => error.message) || [];
}

function resetForm() {
  if (props.book) {
    // Reset to original book data
    form.value = { ...props.book };
  } else {
    // Reset to empty form for new book
    form.value = {
      title: '',
      author: '',
      isbn: '',
      category: '',
      language: '',
      totalCopies: 1,
      availableCopies: 1,
    };
  }
}

function submitForm() {
  if (!props.canEdit) return;
  
  // Remove the id field before emitting
  const { id, ...bookData } = form.value;
  emit('save', bookData);
}
</script>