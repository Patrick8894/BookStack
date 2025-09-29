<!-- frontend/pages/books/index.vue -->
<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Books Management</h1>
      <v-btn 
        v-if="canAddBook"
        color="primary" 
        @click="navigateTo('/books/new')"
      >
        Add New Book
      </v-btn>
    </div>

    <!-- Summary Cards -->
    <v-row class="mb-4">
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Total Books</div>
            <div class="text-h4 text-primary">{{ books.length }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Available</div>
            <div class="text-h4 text-success">{{ availableBooksCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Fully Borrowed</div>
            <div class="text-h4 text-warning">{{ fullyBorrowedCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Categories</div>
            <div class="text-h4">{{ uniqueCategories }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Search and Filters -->
    <v-card class="mb-4">
      <v-card-title>Search & Filter Books</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="4">
            <v-text-field 
              v-model="filters.title" 
              label="Search by Title" 
              clearable 
              density="compact"
              prepend-inner-icon="mdi-magnify"
            />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field 
              v-model="filters.author" 
              label="Search by Author" 
              clearable 
              density="compact"
              prepend-inner-icon="mdi-account"
            />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field 
              v-model="filters.category" 
              label="Search by Category" 
              clearable 
              density="compact"
              prepend-inner-icon="mdi-tag"
            />
          </v-col>
        </v-row>
        <div class="d-flex gap-2">
          <v-btn color="primary" @click="handleSearch" :loading="loading">
            Search Books
          </v-btn>
          <v-btn variant="outlined" @click="clearFilters">
            Clear Filters
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- Books Table -->
    <v-card>
      <v-card-title>Book Collection</v-card-title>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="books"
          :loading="loading"
          class="elevation-1"
          density="compact"
          hover
          @click:row="goToBook"
        >
          <!-- Availability status column -->
          <template v-slot:item.availability="{ item }">
            <v-chip
              :color="getAvailabilityColor(item)"
              size="small"
              variant="flat"
            >
              {{ getAvailabilityStatus(item) }}
            </v-chip>
          </template>

          <!-- Available copies with progress -->
          <template v-slot:item.availableCopies="{ item }">
            <div class="d-flex align-center gap-2">
              <span>{{ item.availableCopies }}/{{ item.totalCopies }}</span>
              <v-progress-linear
                :model-value="(item.availableCopies / item.totalCopies) * 100"
                :color="getAvailabilityColor(item)"
                height="6"
                style="min-width: 60px"
              />
            </div>
          </template>

          <!-- Category with icon -->
          <template v-slot:item.category="{ item }">
            <v-chip size="small" variant="outlined">
              {{ item.category || 'Uncategorized' }}
            </v-chip>
          </template>

          <!-- Actions column -->
          <template v-slot:item.actions="{ item }" v-if="canAddBook">
            <div class="d-flex gap-1">
              <v-btn
                color="primary"
                size="small"
                variant="outlined"
                @click.stop="navigateTo(`/books/${item.id}`)"
              >
                View
              </v-btn>
              <v-btn
                color="success"
                size="small"
                variant="outlined"
                @click.stop="navigateTo(`/books/${item.id}`)"
              >
                Edit
              </v-btn>
            </div>
          </template>
        </v-data-table>
      </v-card-text>
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
import { getAllBooks, searchBooks } from '@/services/bookService';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from '#app';

const auth = useAuthStore();
const books = ref<any[]>([]);
const loading = ref(false);
const error = ref('');
const successMessage = ref('');

const filters = ref({
  title: '',
  author: '',
  category: ''
});

const canAddBook = computed(() => 
  auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN'
);

// Statistics
const availableBooksCount = computed(() => 
  books.value.filter(book => book.availableCopies > 0).length
);

const fullyBorrowedCount = computed(() => 
  books.value.filter(book => book.availableCopies === 0).length
);

const uniqueCategories = computed(() => {
  const categories = new Set(books.value.map(book => book.category).filter(Boolean));
  return categories.size;
});

const headers = computed(() => {
  const baseHeaders = [
    // { title: 'ID', key: 'id', sortable: true },
    { title: 'Title', key: 'title', sortable: true },
    { title: 'Author', key: 'author', sortable: true },
    // { title: 'ISBN', key: 'isbn', sortable: true },
    { title: 'Category', key: 'category', sortable: true },
    { title: 'Language', key: 'language', sortable: true },
    { title: 'Availability', key: 'availability', sortable: false },
    { title: 'Copies', key: 'availableCopies', sortable: true },
  ];

  if (canAddBook.value) {
    baseHeaders.push({ title: 'Actions', key: 'actions', sortable: false });
  }

  return baseHeaders;
});

onMounted(async () => {
  await loadBooks();
});

async function loadBooks() {
  try {
    loading.value = true;
    error.value = '';
    books.value = await getAllBooks();
  } catch (err: any) {
    error.value = err.message || 'Failed to load books';
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  try {
    loading.value = true;
    error.value = '';
    
    if (!filters.value.title && !filters.value.author && !filters.value.category) {
      await loadBooks();
    } else {
      books.value = await searchBooks({
        title: filters.value.title || undefined,
        author: filters.value.author || undefined,
        category: filters.value.category || undefined,
      });
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to search books';
  } finally {
    loading.value = false;
  }
}

function clearFilters() {
  filters.value = {
    title: '',
    author: '',
    category: ''
  };
  loadBooks();
}

function getAvailabilityColor(book: any): string {
  if (book.availableCopies === 0) return 'error';
  if (book.availableCopies <= 2) return 'warning';
  return 'success';
}

function getAvailabilityStatus(book: any): string {
  if (book.availableCopies === 0) return 'Unavailable';
  if (book.availableCopies <= 2) return 'Low Stock';
  return 'Available';
}

function goToBook(event: PointerEvent, { item }: { item: any }) {
  navigateTo(`/books/${item.id}`);
}
</script>