import { Injectable, inject } from '@angular/core';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { Observable, firstValueFrom } from 'rxjs';

type ToastColor = 'success' | 'warning' | 'danger' | 'primary' | 'medium';

@Injectable({ providedIn: 'root' })
export class AuthFeedbackService {
  private readonly loadingController = inject(LoadingController);
  private readonly toastController = inject(ToastController);

  async runWithLoading<T>(message: string, action: () => Observable<T>): Promise<T> {
    const loading = await this.loadingController.create({
      message,
      spinner: 'crescent',
      backdropDismiss: false,
    });

    await loading.present();

    try {
      return await firstValueFrom(action());
    } finally {
      await loading.dismiss().catch(() => undefined);
    }
  }

  async showToast(message: string, color: ToastColor = 'primary'): Promise<void> {
    const toast = await this.toastController.create({
      message,
      color,
      duration: 3200,
      position: 'top',
    });
    await toast.present();
  }
}
