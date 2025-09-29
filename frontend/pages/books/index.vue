<template>
  <v-container>
    <v-text-field v-model="title" label="Search by Title" clearable />
    <v-text-field v-model="author" label="Search by Author" clearable />
    <v-text-field v-model="category" label="Search by Category" clearable />
    <v-btn color="primary" class="mt-2" @click="handleSearch">Search</v-btn>

    <v-data-table
      :headers="headers"
      :items="books"
      class="mt-4"
      dense
      hover
      @click:row="goToBook"
    />

    <div class="mt-4" v-if="isLibrarianOrAdmin">
      <v-btn color="primary" @click="navigateTo('/books/new')">Add New Book</v-btn>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getAllBooks, searchBooks } from '@/services/bookService';
import { useAuthStore } from '@/store/auth';
import { navigateTo } from '#app';

const books = ref<any[]>([]);
const title = ref('');
const author = ref('');
const category = ref('');
const auth = useAuthStore();

const headers = [
  { title: 'ID', key: 'id' },
  { title: 'Title', key: 'title' },
  { title: 'Author', key: 'author' },
  { title: 'Category', key: 'category' },
  { title: 'Available', key: 'availableCopies' },
  { title: 'Total', key: 'totalCopies' },
];

const isLibrarianOrAdmin = auth.user?.role === 'LIBRARIAN' || auth.user?.role === 'ADMIN';

async function loadBooks() {
  books.value = await getAllBooks();
}

async function handleSearch() {
  if (!title.value && !author.value && !category.value) {
    await loadBooks();
  } else {
    books.value = await searchBooks({
      title: title.value || undefined,
      author: author.value || undefined,
      category: category.value || undefined,
    });
  }
}

function goToBook(event: PointerEvent, { item }: { item: any }) {
  navigateTo(`/books/${item.id}`);
}

onMounted(loadBooks);
</script>
