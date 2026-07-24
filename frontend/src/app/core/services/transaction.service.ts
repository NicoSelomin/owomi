import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import {
  Transaction,
  TransactionListFilters,
  TransactionPage,
  TransactionRequest,
} from '../models/transaction.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly api = inject(ApiService);

  findAll(filters: TransactionListFilters = {}): Observable<ApiResponse<TransactionPage>> {
    return this.api.get<ApiResponse<TransactionPage>>('/api/transactions', filters);
  }

  findById(id: number): Observable<ApiResponse<Transaction>> {
    return this.api.get<ApiResponse<Transaction>>(`/api/transactions/${id}`);
  }

  create(request: TransactionRequest): Observable<ApiResponse<Transaction>> {
    return this.api.post<ApiResponse<Transaction>, TransactionRequest>('/api/transactions', request);
  }

  update(id: number, request: TransactionRequest): Observable<ApiResponse<Transaction>> {
    return this.api.put<ApiResponse<Transaction>, TransactionRequest>(`/api/transactions/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.api.delete<ApiResponse<void>>(`/api/transactions/${id}`);
  }
}
