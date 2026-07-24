import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  IonContent,
  IonHeader,
  IonLabel,
  IonTabBar,
  IonTabButton,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';

interface NavigationItem {
  label: string;
  route: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    IonContent,
    IonHeader,
    IonLabel,
    IonTabBar,
    IonTabButton,
    IonTitle,
    IonToolbar,
  ],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss',
})
export class AppLayoutComponent {
  readonly navigation: NavigationItem[] = [
    { label: 'Dashboard', route: '/app/dashboard' },
    { label: 'Transactions', route: '/app/transactions', disabled: true },
    { label: 'Catégories', route: '/app/categories', disabled: true },
    { label: 'Rapports', route: '/app/reports', disabled: true },
    { label: 'Paramètres', route: '/app/settings', disabled: true },
  ];
}
