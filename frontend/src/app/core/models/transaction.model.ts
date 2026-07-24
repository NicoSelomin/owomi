import type { Category } from './category.model';

export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  note: string | null;
  date: string;
  category: Category;
}

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  categoryId: number;
  date: string;
  note?: string | null;
}

export interface TransactionPage {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  type?: TransactionType;
  categoryId?: number;
}

export interface TransactionListFilters extends TransactionFilters {
  page?: number;
  size?: number;
}
