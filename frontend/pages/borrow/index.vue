<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Borrow Management</h1>
      <v-btn 
        v-if="canManageBorrows"
        color="primary" 
        @click="showBorrowDialog = true"
      >
        New Borrow
      </v-btn>
    </div>

    <!-- Quick Actions Cards -->
    <v-row class="mb-4">
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Active Borrows</div>
            <div class="text-h4 text-primary">{{ activeBorrowsCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Overdue</div>
            <div class="text-h4 text-error">{{ overdueBorrowsCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Returned Today</div>
            <div class="text-h4 text-success">{{ todayReturnsCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card>
          <v-card-text>
            <div class="text-h6">Total Records</div>
            <div class="text-h4">{{ totalBorrowsCount }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Borrow Records Table -->
    <BorrowTable
      :borrows="borrows"
      :loading="loading"
      title="All Borrow Records"
      :show-filters="true"
      :show-actions="true"
      :can-return="canManageBorrows"
      :can-delete="canDelete"
      @return="handleReturn"
      @delete="handleDelete"
      @row-click="goToBorrowDetail"
    />

    <!-- New Borrow Dialog -->
    <v-dialog v-model="showBorrowDialog" max-width="600px">
      <v-card>
        <v-card-title>Borrow Book</v-card-title>
        <v-card-text>
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
          
          <BorrowForm
            :can-edit="true"
            :loading="submitLoading"
            :validation-errors="validationErrors"
            form-type="borrow"
            @submit="handleBorrowSubmit"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="outlined" @click="showBorrowDialog = false">
            Cancel
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Return Dialog -->
    <v-dialog v-model="showReturnDialog" max-width="600px">
      <v-card>
        <v-card-title>Return Book</v-card-title>
        <v-card-text>
          <div class="mb-4">
            <strong>User:</strong> {{ selectedBorrow?.userName }}<br>
            <strong>Book:</strong> {{ selectedBorrow?.bookTitle }}<br>
            <strong>Due Date:</strong> {{ formatDate(selectedBorrow?.dueDate) }}
          </div>
          
          <v-textarea
            v-model="returnNotes"
            label="Return Notes (Optional)"
            rows="3"
            counter="500"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="outlined" @click="showReturnDialog = false">
            Cancel
          </v-btn>
          <v-btn 
            color="success" 
            @click="confirmReturn"
            :loading="submitLoading"
          >
            Confirm Return
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
import { 
  getAllBorrows, 
  borrowBook, 
  returnBook, 
  deleteBorrow,
  type BorrowRecord,
  type BorrowRequest 
} from '~/services/borrowService';
import { useAuthStore } from '~/store/auth';
import { navigateTo } from '#app';
import BorrowTable from '~/components/borrow/BorrowTable.vue';
import BorrowForm from '~/components/borrow/BorrowForm.vue';
import { createError } from 'h3';

// Protect this route
const auth = useAuthStore();
if (!auth.user || (auth.user.role !== 'ADMIN' && auth.user.role !== 'LIBRARIAN')) {
  throw createError({
    statusCode: 403,
    statusMessage: 'Access denied. Only admins and librarians can manage borrows.'
  });
}

const borrows = ref<BorrowRecord[]>([]);
const loading = ref(false);
const submitLoading = ref(false);
const error = ref('');
const successMessage = ref('');
const validationErrors = ref<any[]>([]);
const showBorrowDialog = ref(false);
const showReturnDialog = ref(false);
const selectedBorrow = ref<BorrowRecord | null>(null);
const returnNotes = ref('');

const canManageBorrows = computed(() => 
  auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN'
);

const canDelete = computed(() => auth.user?.role === 'ADMIN');

// Statistics
const activeBorrowsCount = computed(() => 
  borrows.value.filter(b => b.status === 'ACTIVE').length
);

const overdueBorrowsCount = computed(() => 
  borrows.value.filter(b => b.status === 'OVERDUE').length
);

const todayReturnsCount = computed(() => {
  const today = new Date().toDateString();
  return borrows.value.filter(b => 
    b.returnDate && new Date(b.returnDate).toDateString() === today
  ).length;
});

const totalBorrowsCount = computed(() => borrows.value.length);

onMounted(async () => {
  await loadBorrows();
});

async function loadBorrows() {
  try {
    loading.value = true;
    error.value = '';
    borrows.value = await getAllBorrows();
  } catch (err: any) {
    error.value = err.message || 'Failed to load borrows';
  } finally {
    loading.value = false;
  }
}

async function handleBorrowSubmit(formData: any) {
  try {
    submitLoading.value = true;
    validationErrors.value = [];
    error.value = '';
    
    const borrowRequest: BorrowRequest = {
      userId: Number(formData.userId),
      bookId: Number(formData.bookId),
      notes: formData.notes || undefined,
    };
    
    await borrowBook(borrowRequest);
    showBorrowDialog.value = false;
    successMessage.value = 'Book borrowed successfully!';
    await loadBorrows();
    
    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (err: any) {
    error.value = err.message || 'Failed to borrow book';
  } finally {
    submitLoading.value = false;
  }
}

function handleReturn(borrow: BorrowRecord) {
  selectedBorrow.value = borrow;
  returnNotes.value = '';
  showReturnDialog.value = true;
}

async function confirmReturn() {
  if (!selectedBorrow.value) return;
  
  try {
    submitLoading.value = true;
    error.value = '';
    
    await returnBook(selectedBorrow.value.id, { 
      notes: returnNotes.value || undefined 
    });
    
    showReturnDialog.value = false;
    selectedBorrow.value = null;
    successMessage.value = 'Book returned successfully!';
    await loadBorrows();
    
    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (err: any) {
    error.value = err.message || 'Failed to return book';
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(borrow: BorrowRecord) {
  if (confirm(`Are you sure you want to delete borrow record #${borrow.id}?`)) {
    try {
      loading.value = true;
      await deleteBorrow(borrow.id);
      successMessage.value = 'Borrow record deleted successfully!';
      await loadBorrows();
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        successMessage.value = '';
      }, 3000);
    } catch (err: any) {
      error.value = err.message || 'Failed to delete borrow record';
      loading.value = false;
    }
  }
}

function goToBorrowDetail(borrow: BorrowRecord) {
  // Optional: Navigate to borrow detail page
  navigateTo(`/borrow/${borrow.id}`);
}

function formatDate(dateString?: string): string {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString();
}
</script>