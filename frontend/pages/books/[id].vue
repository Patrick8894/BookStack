<!-- frontend/pages/books/[id].vue -->
<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <div>
        <h1>{{ book?.title || 'Loading...' }}</h1>
        <div class="text-subtitle-1 text-grey" v-if="book">
          by {{ book.author }}
        </div>
      </div>
      <div class="d-flex gap-2">
        <v-btn 
          variant="outlined" 
          @click="navigateTo('/books')"
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

    <!-- Book Status Card -->
    <v-card class="mb-4" v-if="book && !editMode">
      <v-card-title>Book Status</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="3">
            <div class="text-center">
              <v-icon 
                :color="getAvailabilityColor(book)" 
                size="64"
                class="mb-2"
              >
                {{ getAvailabilityIcon(book) }}
              </v-icon>
              <div class="text-h6">{{ getAvailabilityStatus(book) }}</div>
              <v-chip 
                :color="getAvailabilityColor(book)" 
                size="small" 
                variant="flat"
              >
                {{ book.availableCopies }}/{{ book.totalCopies }} Available
              </v-chip>
            </div>
          </v-col>
          <v-col cols="12" md="9">
            <v-row>
              <v-col cols="6" md="3">
                <div class="text-subtitle-2 text-grey">ISBN</div>
                <div class="text-body-1">{{ book.isbn }}</div>
              </v-col>
              <v-col cols="6" md="3">
                <div class="text-subtitle-2 text-grey">Category</div>
                <div class="text-body-1">
                  <v-chip size="small" variant="outlined">
                    {{ book.category || 'Uncategorized' }}
                  </v-chip>
                </div>
              </v-col>
              <v-col cols="6" md="3">
                <div class="text-subtitle-2 text-grey">Language</div>
                <div class="text-body-1">{{ book.language || 'Not specified' }}</div>
              </v-col>
              <v-col cols="6" md="3">
                <div class="text-subtitle-2 text-grey">Total Copies</div>
                <div class="text-body-1">{{ book.totalCopies }}</div>
              </v-col>
            </v-row>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Book Form Card -->
    <v-card v-if="book">
      <v-card-title>{{ editMode ? 'Edit Book Details' : 'Book Information' }}</v-card-title>
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

      <v-card-actions v-if="canDelete && !editMode">
        <v-spacer />
        <v-btn 
          color="error" 
          variant="outlined"
          @click="handleDelete"
        >
          Delete Book
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
import { deleteBook, getBookById, updateBook } from '~/services/bookService';
import { useAuthStore } from '~/store/auth';
import { validateBook, type BookValidationError } from '@/utils/book-validation';
import BookForm from '~/components/books/BookForm.vue';
import { navigateTo } from '#app';

const route = useRoute();
const router = useRouter();
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
const successMessage = ref('');
const editMode = ref(false);
const originalBook = ref<Book | null>(null);
const validationErrors = ref<BookValidationError[]>([]);

const canEdit = computed(() => {
  return auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN';
});

const canDelete = computed(() => {
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

async function handleSave(formData: Omit<Book, 'id'>) {
  // Clear previous errors
  validationErrors.value = [];
  error.value = '';
  successMessage.value = '';
  
  // Validate the book data
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
    validationErrors.value = [];
    successMessage.value = 'Book updated successfully!';
    
    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
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

async function handleDelete() {
  if (confirm('Are you sure you want to delete this book? This action cannot be undone.')) {
    try {
      loading.value = true;
      await deleteBook(book.value!.id);
      router.push('/books');
    } catch (err: any) {
      error.value = err.message || 'Failed to delete book';
      loading.value = false;
    }
  }
}

function getAvailabilityColor(book: Book): string {
  if (book.availableCopies === 0) return 'error';
  if (book.availableCopies <= 2) return 'warning';
  return 'success';
}

function getAvailabilityIcon(book: Book): string {
  if (book.availableCopies === 0) return 'mdi-book-remove';
  if (book.availableCopies <= 2) return 'mdi-book-alert';
  return 'mdi-book-check';
}

function getAvailabilityStatus(book: Book): string {
  if (book.availableCopies === 0) return 'Unavailable';
  if (book.availableCopies <= 2) return 'Low Stock';
  return 'Available';
}
</script>