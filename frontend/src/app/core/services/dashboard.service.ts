import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { CategoryExpense, DashboardSummary, MonthlyBalance } from '../models/dashboard.model';
import type { TransactionFilters } from '../models/transaction.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  getSummary(filters: Pick<TransactionFilters, 'startDate' | 'endDate'> = {}): Observable<ApiResponse<DashboardSummary>> {
    return this.api.get<ApiResponse<DashboardSummary>>('/api/dashboard/summary', filters);
  }

  getMonthlyBalances(filters: Pick<TransactionFilters, 'startDate' | 'endDate'> = {}): Observable<ApiResponse<MonthlyBalance[]>> {
    return this.api.get<ApiResponse<MonthlyBalance[]>>('/api/dashboard/monthly-balances', filters);
  }

  getCategoryExpenses(filters: Pick<TransactionFilters, 'startDate' | 'endDate'> = {}): Observable<ApiResponse<CategoryExpense[]>> {
    return this.api.get<ApiResponse<CategoryExpense[]>>('/api/dashboard/category-expenses', filters);
  }
}
