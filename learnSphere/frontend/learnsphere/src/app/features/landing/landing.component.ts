import { ActivateDirective } from '@shared/directives/activate.directive';
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { LANDING_HEADER_COPY } from '../../core/content/landing-header.content';
import { LANDING_UI } from '@core/content/landing.ui';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [ActivateDirective, CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  /** Template copy (Wave 7 gate): `core/content/landing.ui.ts`. */
  protected readonly ui = LANDING_UI;
  readonly copy = LANDING_HEADER_COPY;
  isMobileMenuOpen = false;

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }
}
