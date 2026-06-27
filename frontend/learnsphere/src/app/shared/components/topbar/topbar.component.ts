import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router'; // <-- Add this import

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule], // <-- Ensure CommonModule is here for *ngIf
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  isMenuOpen = false;

  // Placeholder user data (You can make this dynamic later!)
  userName = 'Admin User';
  userEmail = 'admin@learnsphere.in';
  userInitials = 'AD';

  constructor(private router: Router) {}

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu() {
    this.isMenuOpen = false;
  }

  signOut() {
    this.closeMenu();
    // Route the user back to the login page
    this.router.navigate(['/login']);
  }
}