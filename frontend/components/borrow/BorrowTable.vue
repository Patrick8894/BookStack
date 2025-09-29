<template>
  <v-card>
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <!-- Filters -->
      <div class="d-flex gap-4 mb-4" v-if="showFilters">
        <v-text-field
          v-model="filters.userId"
          label="Filter by User ID"
          type="number"
          clearable
          density="compact"
          style="max-width: 200px"
        />
        <v-text-field
          v-model="filters.bookId"
          label="Filter by Book ID"
          type="number"
          clearable
          density="compact"
          style="max-width: 200px"
        />
        <v-select
          v-model="filters.status"
          label="Filter by Status"
          :items="statusOptions"
          clearable
          density="compact"
          style="max-width: 200px"
        />
        <v-btn color="primary" @click="applyFilters" density="compact">
          Apply Filters
        </v-btn>
        <v-btn variant="outlined" @click="clearFilters" density="compact">
          Clear
        </v-btn>
      </div>

      <v-data-table
        :headers="headers"
        :items="filteredBorrows"
        :loading="loading"
        class="elevation-1"
        density="compact"
        hover
        @click:row="handleRowClick"
      >
        <!-- Custom status column with color coding -->
        <template v-slot:item.status="{ item }">
          <v-chip
            :color="getStatusColor(item.status)"
            size="small"
            variant="flat"
          >
            {{ item.status }}
          </v-chip>
        </template>

        <!-- Format dates -->
        <template v-slot:item.borrowDate="{ item }">
          {{ formatDate(item.borrowDate) }}
        </template>
        
        <template v-slot:item.dueDate="{ item }">
          {{ formatDate(item.dueDate) }}
        </template>
        
        <template v-slot:item.returnDate="{ item }">
          {{ item.returnDate ? formatDate(item.returnDate) : '-' }}
        </template>

        <!-- Actions column -->
        <template v-slot:item.actions="{ item }" v-if="showActions">
          <v-btn
            v-if="item.status === 'ACTIVE' && canReturn"
            color="success"
            size="small"
            variant="outlined"
            @click.stop="$emit('return', item)"
          >
            Return
          </v-btn>
          <v-btn
            v-if="canDelete"
            color="error"
            size="small"
            variant="outlined"
            @click.stop="$emit('delete', item)"
            class="ml-2"
          >
            Delete
          </v-btn>
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

interface BorrowRecord {
  id: number;
  userId: number;
  bookId: number;
  userName: string;
  bookTitle: string;
  status: 'ACTIVE' | 'RETURNED' | 'OVERDUE';
  borrowDate: string;
  dueDate: string;
  returnDate?: string;
  notes?: string;
}

const props = defineProps<{
  borrows: BorrowRecord[];
  loading?: boolean;
  title?: string;
  showFilters?: boolean;
  showActions?: boolean;
  canReturn?: boolean;
  canDelete?: boolean;
}>();

const emit = defineEmits<{
  return: [borrow: BorrowRecord];
  delete: [borrow: BorrowRecord];
  rowClick: [borrow: BorrowRecord];
}>();

const filters = ref({
  userId: '',
  bookId: '',
  status: '',
});

const statusOptions = [
  { title: 'Active', value: 'ACTIVE' },
  { title: 'Returned', value: 'RETURNED' },
  { title: 'Overdue', value: 'OVERDUE' },
];

const headers = computed(() => {
  const baseHeaders = [
    // { title: 'ID', key: 'id', sortable: true },
    // { title: 'User ID', key: 'userId', sortable: true },
    { title: 'User Name', key: 'userName', sortable: true },
    // { title: 'Book ID', key: 'bookId', sortable: true },
    { title: 'Book Title', key: 'bookTitle', sortable: true },
    { title: 'Status', key: 'status', sortable: true },
    { title: 'Borrow Date', key: 'borrowDate', sortable: true },
    { title: 'Due Date', key: 'dueDate', sortable: true },
    { title: 'Return Date', key: 'returnDate', sortable: true },
  ];

  if (props.showActions) {
    baseHeaders.push({ title: 'Actions', key: 'actions', sortable: false });
  }

  return baseHeaders;
});

const filteredBorrows = computed(() => {
  let result = [...props.borrows];

  if (filters.value.userId) {
    result = result.filter(borrow => 
      borrow.userId.toString().includes(filters.value.userId)
    );
  }

  if (filters.value.bookId) {
    result = result.filter(borrow => 
      borrow.bookId.toString().includes(filters.value.bookId)
    );
  }

  if (filters.value.status) {
    result = result.filter(borrow => 
      borrow.status === filters.value.status
    );
  }

  return result;
});

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

function applyFilters() {
  // Filters are reactive, so this just forces a re-render
}

function clearFilters() {
  filters.value = {
    userId: '',
    bookId: '',
    status: '',
  };
}

function handleRowClick(event: PointerEvent, { item }: { item: BorrowRecord }) {
  emit('rowClick', item);
}
</script>