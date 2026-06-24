import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Écran d'accueil (splash) — reproduction de docs/mockups/owomi_splash.html.
 * 3 layouts responsive : mobile / tablette / desktop.
 */
@Component({
  selector: 'app-splash',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './splash.page.html',
  styleUrl: './splash.page.scss',
})
export class SplashPage {}
