import { HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import type { TransactionFilters } from '../models/transaction.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class TransactionExportService {
  private static readonly FALLBACK_FILENAME = 'owomi-transactions.csv';

  private readonly api = inject(ApiService);

  exportCsv(filters: TransactionFilters = {}): Observable<{ blob: Blob; filename: string }> {
    return this.api.getBlobResponse('/api/transactions/export/csv', filters).pipe(
      map((response) => ({
        blob: response.body ?? new Blob([], { type: 'text/csv;charset=utf-8' }),
        filename: this.extractFilename(response),
      }))
    );
  }

  downloadCsv(filters: TransactionFilters = {}): Observable<void> {
    return this.exportCsv(filters).pipe(
      tap(({ blob, filename }) => this.downloadBlob(blob, filename)),
      map(() => undefined)
    );
  }

  extractFilename(response: HttpResponse<Blob>): string {
    const disposition = response.headers.get('Content-Disposition');
    if (!disposition) {
      return TransactionExportService.FALLBACK_FILENAME;
    }

    const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
    const quotedMatch = /filename="([^"]+)"/i.exec(disposition);
    const rawFilename = decodeURIComponent(utf8Match?.[1] ?? quotedMatch?.[1] ?? '');
    return this.safeFilename(rawFilename);
  }

  private safeFilename(filename: string): string {
    const safe = filename.trim().replace(/[^a-zA-Z0-9._-]/g, '-');
    if (!safe || safe === '.' || safe === '..') {
      return TransactionExportService.FALLBACK_FILENAME;
    }
    return safe.endsWith('.csv') ? safe : `${safe}.csv`;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.rel = 'noopener';
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(objectUrl);
  }
}
