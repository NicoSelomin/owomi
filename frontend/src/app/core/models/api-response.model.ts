/**
 * Format de réponse uniforme renvoyé par l'API OWOMI.
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
  error?: ApiError;
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
}
