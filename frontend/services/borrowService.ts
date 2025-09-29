import { useNuxtApp } from '#app';

export interface BorrowRequest {
  userId: number;
  bookId: number;
  notes?: string;
}

export interface ReturnRequest {
  notes?: string;
}

export interface BorrowRecord {
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

export async function borrowBook(request: BorrowRequest): Promise<BorrowRecord> {
  const { $api } = useNuxtApp();
  const res = await $api.post('/borrows', request);
  return res.data;
}

export async function returnBook(borrowId: number, request: ReturnRequest): Promise<BorrowRecord> {
  const { $api } = useNuxtApp();
  const res = await $api.put(`/borrows/${borrowId}/return`, request);
  return res.data;
}

export async function getAllBorrows(): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get('/borrows');
  return res.data;
}

export async function getBorrowById(id: number): Promise<BorrowRecord> {
  const { $api } = useNuxtApp();
  const res = await $api.get(`/borrows/${id}`);
  return res.data;
}

export async function getBorrowsByUser(userId: number): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get(`/borrows/user/${userId}`);
  return res.data;
}

export async function getActiveBorrowsByUser(userId: number): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get(`/borrows/user/${userId}/active`);
  return res.data;
}

export async function getBorrowsByBook(bookId: number): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get(`/borrows/book/${bookId}`);
  return res.data;
}

export async function getBorrowsByStatus(status: string): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get(`/borrows/status?status=${status}`);
  return res.data;
}

export async function getOverdueBorrows(): Promise<BorrowRecord[]> {
  const { $api } = useNuxtApp();
  const res = await $api.get('/borrows/overdue');
  return res.data;
}

export async function deleteBorrow(id: number): Promise<void> {
  const { $api } = useNuxtApp();
  await $api.delete(`/borrows/${id}`);
}