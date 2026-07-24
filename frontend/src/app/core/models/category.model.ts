import type { TransactionType } from './transaction.model';

export interface Category {
  id: number;
  name: string;
  icon: string;
  color: string;
  type: TransactionType;
  isDefault: boolean;
}

export interface CategoryRequest {
  name: string;
  icon: string;
  color: string;
  type: TransactionType;
}
