import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  // Coquille applicative : héberge le routeur.
  // Les écrans d'authentification sont des pages plein écran sur mesure (design OWOMI),
  // d'où l'usage d'un router-outlet standard plutôt qu'ion-router-outlet.
}
