<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>{{ book?.title || 'Loading...' }}</h1>
      <div>
        <v-btn 
          variant="outlined" 
          @click="navigateTo('/books')"
          class="mr-2"
        >
          Back to Books
        </v-btn>
        <v-btn 
          v-if="canEdit && !editMode" 
          color="primary" 
          @click="editMode = true"
        >
          Edit Book
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

    <v-card v-if="book">
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
        
        <BookForm
          :book="book"
          :can-edit="canEdit && editMode"
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
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { getBookById, updateBook } from '~/services/bookService';
import { useAuthStore } from '~/store/auth';
import { validateBook, type BookValidationError } from '@/utils/book-validation';
import BookForm from '~/components/books/BookForm.vue';
import { navigateTo } from '#app';

const route = useRoute();
const auth = useAuthStore();

interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  language: string;
  totalCopies: number;
  availableCopies: number;
}

const book = ref<Book | null>(null);
const loading = ref(false);
const error = ref('');
const editMode = ref(false);
const originalBook = ref<Book | null>(null);
const validationErrors = ref<BookValidationError[]>([]);

const canEdit = computed(() => {
  return auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN';
});

onMounted(async () => {
  await loadBook();
});

async function loadBook() {
  try {
    loading.value = true;
    const bookId = parseInt(route.params.id as string);
    const bookData = await getBookById(bookId);
    book.value = bookData;
    originalBook.value = { ...bookData };
    error.value = '';
  } catch (err: any) {
    error.value = err.message || 'Failed to load book';
  } finally {
    loading.value = false;
  }
}

async function handleSave(formData: any) {
  // Clear previous errors
  validationErrors.value = [];
  error.value = '';
  
  // Validate the updated book data
  const errors = validateBook(formData, true); // true = isUpdate
  
  if (errors.length > 0) {
    validationErrors.value = errors;
    return; // Don't save if there are validation errors
  }
  
  try {
    loading.value = true;
    const bookId = parseInt(route.params.id as string);
    
    const updatedBook = await updateBook(bookId, formData);
    
    book.value = updatedBook;
    originalBook.value = { ...updatedBook };
    editMode.value = false;
    error.value = '';
    validationErrors.value = [];
    
    // Show success message
    alert('Book updated successfully!');
  } catch (err: any) {
    error.value = err.message || 'Failed to update book';
  } finally {
    loading.value = false;
  }
}

function cancelEdit() {
  if (originalBook.value) {
    book.value = { ...originalBook.value };
  }
  editMode.value = false;
  error.value = '';
  validationErrors.value = [];
}
</script>