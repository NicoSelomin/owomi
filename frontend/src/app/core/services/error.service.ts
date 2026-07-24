import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiError, ApiResponse } from '../models/api-response.model';

export interface UiError {
  code: string;
  message: string;
  details: string[];
  status: number;
}

@Injectable({ providedIn: 'root' })
export class ErrorService {
  toUiError(error: unknown): UiError {
    if (error instanceof HttpErrorResponse) {
      const apiError = this.extractApiError(error.error);
      if (apiError) {
        return {
          code: apiError.code,
          message: apiError.message,
          details: apiError.details ?? [],
          status: error.status,
        };
      }

      return {
        code: 'NETWORK_OR_SERVER_ERROR',
        message: this.genericMessage(error.status),
        details: [],
        status: error.status,
      };
    }

    return {
      code: 'UNKNOWN_ERROR',
      message: 'Une erreur inattendue est survenue.',
      details: [],
      status: 0,
    };
  }

  private extractApiError(body: unknown): ApiError | null {
    if (!body || typeof body !== 'object') {
      return null;
    }
    const response = body as Partial<ApiResponse<unknown>>;
    if (!response.error?.code || !response.error.message) {
      return null;
    }
    return {
      code: response.error.code,
      message: response.error.message,
      details: response.error.details ?? [],
    };
  }

  private genericMessage(status: number): string {
    if (status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
    }
    return 'Une erreur est survenue. Réessayez.';
  }
}
