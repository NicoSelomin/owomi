import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { Currency } from '../models/user.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CurrencyService {
  private readonly api = inject(ApiService);

  findAll(): Observable<ApiResponse<Currency[]>> {
    return this.api.get<ApiResponse<Currency[]>>('/api/currencies');
  }
}
