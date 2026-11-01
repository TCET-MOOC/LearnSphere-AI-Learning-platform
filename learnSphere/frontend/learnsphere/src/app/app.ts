import { Component } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { TopbarComponent } from './shared/components/topbar/topbar.component';
import { TeacherSidebarComponent } from './shared/components/teacher-sidebar/teacher-sidebar.component';
import { AdminSidebarComponent } from './shared/components/admin-sidebar/admin-sidebar.component';
import { AiChatbotComponent } from './shared/components/ai-chatbot/ai-chatbot.component';
import { MobileNavComponent } from './shared/components/mobile-nav/mobile-nav.component';
import { MobileRole } from './core/content/mobile-nav.content';

import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, 
    RouterOutlet, 
    SidebarComponent, 
    TeacherSidebarComponent, 
    AdminSidebarComponent, 
    TopbarComponent, 
    AiChatbotComponent,
    MobileNavComponent
  ],
  templateUrl: './app.html', 
  styleUrls: ['./app.scss'] 
})
export class App {
  title = 'learnsphere';
  
  // 👉 THIS IS THE FIX: Declaring the variable at the class level
  isStandalonePage = false; 
  userRole = 'STUDENT';
  /** Lesson player and quiz player are focus mode on phones: no bottom bar. */
  isFocusRoute = false;

  get mobileRole(): MobileRole {
    return this.userRole as MobileRole;
  }

  constructor(private router: Router, private themeService: ThemeService) {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        
        // 1. Grab the clean URL without any weird query parameters
        const cleanUrl = this.router.url.split('?')[0];

        // 2. Hide layout if on the exact root ('/'), login, or register
        this.isStandalonePage = 
          cleanUrl === '/' || 
          cleanUrl.includes('/login') || 
          cleanUrl.includes('/register');

        this.isFocusRoute = /\/lectures\/|\/player(\/|$)/.test(cleanUrl);

        // 3. Set Roles
        if (cleanUrl.includes('/teacher')) {
          this.userRole = 'TEACHER';
        } else if (cleanUrl.includes('/admin')) {
          this.userRole = 'ADMIN';
        } else {
          this.userRole = 'STUDENT';
        }
        
      }
    });
  }
}