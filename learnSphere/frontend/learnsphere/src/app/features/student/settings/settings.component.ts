import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

interface SettingRow { label: string; hint: string; value?: string; action?: string; enabled?: boolean; }
interface SettingsPanel { title: string; description: string; rows: SettingRow[]; }

@Component({ selector: 'app-student-settings', standalone: true, imports: [CommonModule], templateUrl: './settings.component.html', styleUrls: ['./settings.component.scss'] })
export class SettingsComponent {
  readonly name = 'Raj Shah';
  readonly email = 'raj.shah@mhcollege.edu.in';
  readonly tabs = ['General', 'Notifications', 'Privacy & security', 'Account'];
  activeTab = 'General';
  saved = false;
  panels: Record<string, SettingsPanel> = {
  "General": {
    "title": "Learning preferences",
    "description": "Choose defaults for your classroom experience.",
    "rows": [
      {
        "label": "Video quality",
        "hint": "Default streaming quality",
        "value": "Auto",
        "action": "Change"
      },
      {
        "label": "Playback speed",
        "hint": "Default lecture speed",
        "value": "1×",
        "action": "Change"
      },
      {
        "label": "Language",
        "hint": "Interface and captions",
        "value": "English",
        "action": "Change"
      }
    ]
  },
  "Notifications": {
    "title": "Notification preferences",
    "description": "Choose which learning updates reach you.",
    "rows": [
      {
        "label": "New lecture uploaded",
        "hint": "When an enrolled teacher uploads",
        "enabled": true
      },
      {
        "label": "Assessment reminders",
        "hint": "48 and 24 hours before a deadline",
        "enabled": true
      },
      {
        "label": "Leaderboard changes",
        "hint": "When your rank moves",
        "enabled": false
      }
    ]
  },
  "Privacy & security": {
    "title": "Privacy & security",
    "description": "Control visibility and protect your account.",
    "rows": [
      {
        "label": "Leaderboard visibility",
        "hint": "Visible to batchmates",
        "enabled": true
      },
      {
        "label": "Profile discoverability",
        "hint": "Allow classmates to find your profile",
        "enabled": true
      },
      {
        "label": "Two-factor authentication",
        "hint": "Add an extra sign-in step",
        "enabled": false
      }
    ]
  },
  "Account": {
    "title": "Account management",
    "description": "Manage data and account access.",
    "rows": [
      {
        "label": "College email",
        "hint": "Verified academic identity",
        "value": "Verified"
      },
      {
        "label": "Download my data",
        "hint": "Receive a copy of your LearnSphere data",
        "action": "Request"
      },
      {
        "label": "Password",
        "hint": "Last changed 30 days ago",
        "action": "Change"
      }
    ]
  }
};
  toggle(row: SettingRow): void { row.enabled = !row.enabled; this.showSaved(); }
  showSaved(): void { this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

