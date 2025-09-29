<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>My Borrow Records</h1>
      <div class="text-subtitle-1">
        Welcome, {{ auth.user?.username }}
      </div>
    </div>

    <!-- Summary Cards -->
    <v-row class="mb-4">
      <v-col cols="12" md="4">
        <v-card>
          <v-card-text>
            <div class="text-h6">Currently Borrowed</div>
            <div class="text-h4 text-primary">{{ activeBorrows.length }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card>
          <v-card-text>
            <div class="text-h6">Overdue Books</div>
            <div class="text-h4 text-error">{{ overdueBorrows.length }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card>
          <v-card-text>
            <div class="text-h6">Total Borrowed</div>
            <div class="text-h4">{{ allBorrows.length }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Active Borrows Section -->
    <v-card class="mb-4" v-if="activeBorrows.length > 0">
      <v-card-title class="bg-primary text-white">
        Currently Borrowed Books
      </v-card-title>
      <v-card-text>
        <v-data-table
          :headers="activeHeaders"
          :items="activeBorrows"
          :loading="loading"
          density="compact"
          hide-default-footer
          :items-per-page="-1"
        >
          <template v-slot:item.status="{ item }">
            <v-chip
              :color="getStatusColor(item.status)"
              size="small"
              variant="flat"
            >
              {{ item.status }}
            </v-chip>
          </template>

          <template v-slot:item.borrowDate="{ item }">
            {{ formatDate(item.borrowDate) }}
          </template>
          
          <template v-slot:item.dueDate="{ item }">
            <span :class="{ 'text-error': isOverdue(item.dueDate) }">
              {{ formatDate(item.dueDate) }}
            </span>
          </template>

          <template v-slot:item.daysLeft="{ item }">
            <span :class="getDaysLeftClass(item.dueDate)">
              {{ getDaysLeft(item.dueDate) }}
            </span>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- No Active Borrows Message -->
    <v-card class="mb-4" v-else>
      <v-card-text class="text-center py-8">
        <v-icon size="64" color="grey" class="mb-4">
          mdi-book-outline
        </v-icon>
        <div class="text-h6 text-grey">
          You don't have any books currently borrowed
        </div>
        <div class="text-body-2 text-grey mt-2">
          Visit the Books section to browse available books
        </div>
        <v-btn 
          color="primary" 
          @click="navigateTo('/books')"
          class="mt-4"
        >
          Browse Books
        </v-btn>
      </v-card-text>
    </v-card>

    <!-- History Section -->
    <BorrowTable
      :borrows="returnedBorrows"
      :loading="loading"
      title="Borrow History (Returned Books)"
      :show-filters="false"
      :show-actions="false"
    />

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getBorrowsByUser, type BorrowRecord } from '~/services/borrowService';
import { useAuthStore } from '~/store/auth';
import { navigateTo } from '#app';
import BorrowTable from '~/components/borrow/BorrowTable.vue';
import { createError } from 'h3';

const auth = useAuthStore();

// Ensure user is logged in
if (!auth.user) {
  throw createError({
    statusCode: 401,
    statusMessage: 'Please log in to view your borrow records.'
  });
}

const allBorrows = ref<BorrowRecord[]>([]);
const loading = ref(false);
const error = ref('');

const activeBorrows = computed(() => 
  allBorrows.value.filter(borrow => 
    borrow.status === 'ACTIVE' || borrow.status === 'OVERDUE'
  )
);

const overdueBorrows = computed(() => 
  allBorrows.value.filter(borrow => borrow.status === 'OVERDUE')
);

const returnedBorrows = computed(() => 
  allBorrows.value.filter(borrow => borrow.status === 'RETURNED')
);

const activeHeaders = [
  { title: 'Book Title', key: 'bookTitle', sortable: true },
  { title: 'Status', key: 'status', sortable: true },
  { title: 'Borrowed', key: 'borrowDate', sortable: true },
  { title: 'Due Date', key: 'dueDate', sortable: true },
  { title: 'Days Left', key: 'daysLeft', sortable: false },
];

onMounted(async () => {
  if (auth.user?.id) {
    await loadUserBorrows();
  }
});

async function loadUserBorrows() {
  try {
    loading.value = true;
    error.value = '';
    allBorrows.value = await getBorrowsByUser(auth.user!.id);
  } catch (err: any) {
    error.value = err.message || 'Failed to load your borrow records';
  } finally {
    loading.value = false;
  }
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'ACTIVE': return 'primary';
    case 'RETURNED': return 'success';
    case 'OVERDUE': return 'error';
    default: return 'grey';
  }
}

function formatDate(dateString: string): string {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString();
}

function isOverdue(dueDateString: string): boolean {
  const dueDate = new Date(dueDateString);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return dueDate < today;
}

function getDaysLeft(dueDateString: string): string {
  const dueDate = new Date(dueDateString);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  dueDate.setHours(0, 0, 0, 0);
  
  const diffTime = dueDate.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays < 0) {
    return `${Math.abs(diffDays)} days overdue`;
  } else if (diffDays === 0) {
    return 'Due today';
  } else if (diffDays === 1) {
    return '1 day left';
  } else {
    return `${diffDays} days left`;
  }
}

function getDaysLeftClass(dueDateString: string): string {
  const dueDate = new Date(dueDateString);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  dueDate.setHours(0, 0, 0, 0);
  
  const diffTime = dueDate.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays < 0) {
    return 'text-error font-weight-bold';
  } else if (diffDays <= 3) {
    return 'text-warning font-weight-medium';
  } else {
    return 'text-success';
  }
}
</script>