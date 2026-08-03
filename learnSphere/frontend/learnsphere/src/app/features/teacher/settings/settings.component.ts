import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

interface SettingRow { label: string; hint: string; value?: string; action?: string; enabled?: boolean; }
interface SettingsPanel { title: string; description: string; rows: SettingRow[]; }

@Component({ selector: 'app-teacher-settings', standalone: true, imports: [CommonModule], templateUrl: './settings.component.html', styleUrls: ['./settings.component.scss'] })
export class SettingsComponent {
  readonly name = 'Prof. Ajay Sharma';
  readonly email = 'a.sharma@mhcollege.edu.in';
  readonly tabs = ['General', 'Notifications', 'Privacy & security', 'Account'];
  activeTab = 'General';
  saved = false;
  panels: Record<string, SettingsPanel> = {
  "General": {
    "title": "Course defaults",
    "description": "Apply defaults to newly created courses.",
    "rows": [
      {
        "label": "AI question extraction",
        "hint": "Generate draft questions from uploads",
        "enabled": true
      },
      {
        "label": "Notify students",
        "hint": "On every new lecture",
        "enabled": true
      },
      {
        "label": "Lecture discussions",
        "hint": "Allow questions below lectures",
        "enabled": true
      }
    ]
  },
  "Notifications": {
    "title": "Teaching notifications",
    "description": "Choose the teaching activity you hear about.",
    "rows": [
      {
        "label": "New enrolments",
        "hint": "When somebody joins a course",
        "enabled": true
      },
      {
        "label": "Student messages",
        "hint": "In-app and email alerts",
        "enabled": true
      },
      {
        "label": "Monthly payout",
        "hint": "Payment confirmation by email",
        "enabled": false
      }
    ]
  },
  "Privacy & security": {
    "title": "Privacy & security",
    "description": "Manage your public identity and sign-in.",
    "rows": [
      {
        "label": "Public teacher profile",
        "hint": "Show bio, courses, and ratings",
        "enabled": true
      },
      {
        "label": "Direct student messages",
        "hint": "Allow students to start conversations",
        "enabled": true
      },
      {
        "label": "Two-factor authentication",
        "hint": "Secure payouts and account access",
        "enabled": true
      }
    ]
  },
  "Account": {
    "title": "Account & payouts",
    "description": "Manage account details and payment preferences.",
    "rows": [
      {
        "label": "Payout account",
        "hint": "SBI ••••4821 · Verified",
        "action": "Update"
      },
      {
        "label": "Download my data",
        "hint": "Export courses and account activity",
        "action": "Request"
      },
      {
        "label": "Password",
        "hint": "Last changed 18 days ago",
        "action": "Change"
      }
    ]
  }
};
  toggle(row: SettingRow): void { row.enabled = !row.enabled; this.showSaved(); }
  showSaved(): void { this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

