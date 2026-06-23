import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  IonContent,
  IonHeader,
  IonTitle,
  IonToolbar,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
  IonCardContent,
  IonButton,
  IonText,
} from '@ionic/angular/standalone';
import { environment } from '../../../environments/environment';

interface HealthResponse {
  status: string;
  app: string;
  version: string;
}

/**
 * Page d'accueil temporaire (J1).
 * Vérifie la connexion au backend via l'endpoint /api/health.
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    IonContent,
    IonHeader,
    IonTitle,
    IonToolbar,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonButton,
    IonText,
  ],
  templateUrl: './home.page.html',
  styleUrl: './home.page.scss',
})
export class HomePage {
  private http = inject(HttpClient);

  readonly status = signal<string>('Non vérifié');
  readonly isLoading = signal<boolean>(false);

  checkBackend(): void {
    this.isLoading.set(true);
    this.status.set('Vérification…');
    this.http
      .get<HealthResponse>(`${environment.apiBaseUrl}/api/health`)
      .subscribe({
        next: (res) => {
          this.status.set(`${res.app} ${res.version} — ${res.status}`);
          this.isLoading.set(false);
        },
        error: () => {
          this.status.set('Backend injoignable');
          this.isLoading.set(false);
        },
      });
  }
}
