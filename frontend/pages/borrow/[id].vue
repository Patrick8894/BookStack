<template>
  <v-container>
    <div class="d-flex justify-space-between align-center mb-4">
      <h1>Borrow Record #{{ borrow?.id }}</h1>
      <v-btn 
        variant="outlined" 
        @click="navigateTo('/borrow')"
      >
        Back to Borrow Management
      </v-btn>
    </div>

    <v-card v-if="borrow">
      <v-card-title>Borrow Details</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="12" md="6">
            <v-list density="compact">
              <v-list-item>
                <v-list-item-title>Borrow ID</v-list-item-title>
                <v-list-item-subtitle>{{ borrow.id }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>User</v-list-item-title>
                <v-list-item-subtitle>{{ borrow.userName }} (ID: {{ borrow.userId }})</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>Book</v-list-item-title>
                <v-list-item-subtitle>{{ borrow.bookTitle }} (ID: {{ borrow.bookId }})</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>Status</v-list-item-title>
                <v-list-item-subtitle>
                  <v-chip
                    :color="getStatusColor(borrow.status)"
                    size="small"
                    variant="flat"
                  >
                    {{ borrow.status }}
                  </v-chip>
                </v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </v-col>
          <v-col cols="12" md="6">
            <v-list density="compact">
              <v-list-item>
                <v-list-item-title>Borrow Date</v-list-item-title>
                <v-list-item-subtitle>{{ formatDate(borrow.borrowDate) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <v-list-item-title>Due Date</v-list-item-title>
                <v-list-item-subtitle>{{ formatDate(borrow.dueDate) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="borrow.returnDate">
                <v-list-item-title>Return Date</v-list-item-title>
                <v-list-item-subtitle>{{ formatDate(borrow.returnDate) }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item v-if="borrow.notes">
                <v-list-item-title>Notes</v-list-item-title>
                <v-list-item-subtitle>{{ borrow.notes }}</v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </v-col>
        </v-row>
      </v-card-text>
      
      <v-card-actions v-if="canManageBorrows">
        <v-btn
          v-if="borrow.status === 'ACTIVE'"
          color="success"
          @click="showReturnDialog = true"
        >
          Return Book
        </v-btn>
        <v-btn
          v-if="canDelete"
          color="error"
          variant="outlined"
          @click="handleDelete"
        >
          Delete Record
        </v-btn>
      </v-card-actions>
    </v-card>

    <!-- Return Dialog -->
    <v-dialog v-model="showReturnDialog" max-width="500px">
      <v-card>
        <v-card-title>Return Book</v-card-title>
        <v-card-text>
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
            :loading="loading"
          >
            Confirm Return
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-alert v-if="error" type="error" class="mt-4">
      {{ error }}
    </v-alert>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { 
  getBorrowById, 
  returnBook, 
  deleteBorrow, 
  type BorrowRecord 
} from '~/services/borrowService';
import { useAuthStore } from '~/store/auth';
import { navigateTo } from '#app';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const borrow = ref<BorrowRecord | null>(null);
const loading = ref(false);
const error = ref('');
const showReturnDialog = ref(false);
const returnNotes = ref('');

const canManageBorrows = computed(() => 
  auth.user?.role === 'ADMIN' || auth.user?.role === 'LIBRARIAN'
);

const canDelete = computed(() => auth.user?.role === 'ADMIN');

onMounted(async () => {
  await loadBorrow();
});

async function loadBorrow() {
  try {
    loading.value = true;
    const borrowId = parseInt(route.params.id as string);
    borrow.value = await getBorrowById(borrowId);
    error.value = '';
  } catch (err: any) {
    error.value = err.message || 'Failed to load borrow record';
  } finally {
    loading.value = false;
  }
}

async function confirmReturn() {
  if (!borrow.value) return;
  
  try {
    loading.value = true;
    error.value = '';
    
    await returnBook(borrow.value.id, { 
      notes: returnNotes.value || undefined 
    });
    
    showReturnDialog.value = false;
    await loadBorrow(); // Reload to show updated status
  } catch (err: any) {
    error.value = err.message || 'Failed to return book';
  } finally {
    loading.value = false;
  }
}

async function handleDelete() {
  if (!borrow.value) return;
  
  if (confirm(`Are you sure you want to delete borrow record #${borrow.value.id}?`)) {
    try {
      loading.value = true;
      await deleteBorrow(borrow.value.id);
      router.push('/borrow');
    } catch (err: any) {
      error.value = err.message || 'Failed to delete borrow record';
      loading.value = false;
    }
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
</script>