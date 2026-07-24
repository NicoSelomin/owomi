import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TokenService } from '../../../core/services/token.service';

/**
 * Écran d'accueil (splash) — reproduction de docs/mockups/owomi_splash.html.
 * 3 layouts responsive : mobile / tablette / desktop.
 */
@Component({
  selector: 'app-splash',
  standalone: true,
  templateUrl: './splash.page.html',
  styleUrl: './splash.page.scss',
})
export class SplashPage implements OnInit {
  private readonly router = inject(Router);
  private readonly tokenService = inject(TokenService);

  ngOnInit(): void {
    const target = this.tokenService.isAuthenticated() ? '/app/dashboard' : '/auth/login';
    queueMicrotask(() => this.router.navigateByUrl(target, { replaceUrl: true }));
  }
}
