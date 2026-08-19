import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { SidebarService } from '../../../core/services/sidebar.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './admin-sidebar.component.html',
  styleUrls: ['./admin-sidebar.component.scss'] // Using a custom dark SCSS file
})
export class AdminSidebarComponent {
  isCollapsed$: Observable<boolean>;
  isMobileOpen$: Observable<boolean>;

  constructor(private sidebarService: SidebarService) {
    this.isCollapsed$ = this.sidebarService.isCollapsed$;
    this.isMobileOpen$ = this.sidebarService.isMobileOpen$;
  }

  toggleSidebar(): void {
    this.sidebarService.toggleSidebar();
  }

  closeMobile(): void {
    this.sidebarService.closeMobile();
  }
}