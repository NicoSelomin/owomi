import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

type QueryValue = string | number | boolean | null | undefined;
type QueryParams = object;

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  get<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http.get<T>(this.url(path), { params: this.toHttpParams(params) });
  }

  post<T, B = unknown>(path: string, body: B, params?: QueryParams): Observable<T> {
    return this.http.post<T>(this.url(path), body, { params: this.toHttpParams(params) });
  }

  put<T, B = unknown>(path: string, body: B, params?: QueryParams): Observable<T> {
    return this.http.put<T>(this.url(path), body, { params: this.toHttpParams(params) });
  }

  delete<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http.delete<T>(this.url(path), { params: this.toHttpParams(params) });
  }

  getBlobResponse(path: string, params?: QueryParams): Observable<HttpResponse<Blob>> {
    return this.http.get(this.url(path), {
      params: this.toHttpParams(params),
      observe: 'response',
      responseType: 'blob',
    });
  }

  private url(path: string): string {
    return `${this.baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  }

  private toHttpParams(params?: QueryParams): HttpParams {
    let httpParams = new HttpParams();
    if (!params) {
      return httpParams;
    }

    Object.entries(params as Record<string, QueryValue | readonly QueryValue[]>).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach((entry) => {
          if (entry !== null && entry !== undefined) {
            httpParams = httpParams.append(key, String(entry));
          }
        });
        return;
      }

      if (value !== null && value !== undefined) {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
