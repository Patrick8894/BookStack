<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Add New Book</h1>
      <v-btn variant="outlined" @click="navigateTo('/books')">
        Back to Books
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
        
        <BookForm
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
import { addBook } from '~/services/bookService';
import { useAuthStore } from '~/store/auth';
import { validateBook, type BookValidationError } from '@/utils/book-validation';
import BookForm from '~/components/books/BookForm.vue';
import { navigateTo, createError } from '#app';

// Protect this route - only librarians and admins can add books
const auth = useAuthStore();
if (!auth.user || (auth.user.role !== 'ADMIN' && auth.user.role !== 'LIBRARIAN')) {
  throw createError({
    statusCode: 403,
    statusMessage: 'Access denied. Only librarians and admins can add books.'
  });
}

const loading = ref(false);
const error = ref('');
const validationErrors = ref<BookValidationError[]>([]);

async function handleSave(formData: any) {
  // Clear previous errors
  validationErrors.value = [];
  error.value = '';
  
  // Validate the book data
  const errors = validateBook(formData, false); // false = isCreate
  
  if (errors.length > 0) {
    validationErrors.value = errors;
    return; // Don't save if there are validation errors
  }
  
  try {
    loading.value = true;
    
    // Ensure numeric fields are properly converted
    const bookData = {
      ...formData,
      totalCopies: Number(formData.totalCopies),
      availableCopies: Number(formData.availableCopies),
    };
    
    const newBook = await addBook(bookData);
    
    // Show success message
    alert('Book created successfully!');
    
    // Redirect to the new book's detail page
    await navigateTo(`/books/${newBook.id}`);
  } catch (err: any) {
    error.value = err.message || 'Failed to create book';
    loading.value = false;
  }
}

// Remove the onMounted and loadBook functions - they're not needed for new book page
</script>