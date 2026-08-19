import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarService } from '../../../core/services/sidebar.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
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