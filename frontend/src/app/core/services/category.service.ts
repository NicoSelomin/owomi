import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { Category, CategoryRequest } from '../models/category.model';
import type { TransactionType } from '../models/transaction.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly api = inject(ApiService);

  findAll(type?: TransactionType): Observable<ApiResponse<Category[]>> {
    return this.api.get<ApiResponse<Category[]>>('/api/categories', { type });
  }

  findById(id: number): Observable<ApiResponse<Category>> {
    return this.api.get<ApiResponse<Category>>(`/api/categories/${id}`);
  }

  create(request: CategoryRequest): Observable<ApiResponse<Category>> {
    return this.api.post<ApiResponse<Category>, CategoryRequest>('/api/categories', request);
  }

  update(id: number, request: CategoryRequest): Observable<ApiResponse<Category>> {
    return this.api.put<ApiResponse<Category>, CategoryRequest>(`/api/categories/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.api.delete<ApiResponse<void>>(`/api/categories/${id}`);
  }
}
