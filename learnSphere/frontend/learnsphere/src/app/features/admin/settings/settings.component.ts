import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

interface SettingRow { label: string; hint: string; value?: string; action?: string; enabled?: boolean; }
interface SettingsPanel { title: string; description: string; rows: SettingRow[]; }

@Component({ selector: 'app-admin-settings', standalone: true, imports: [CommonModule], templateUrl: './settings.component.html', styleUrls: ['./settings.component.scss'] })
export class SettingsComponent {
  readonly name = 'Admin User';
  readonly email = 'admin@learnsphere.in';
  readonly tabs = ['General', 'Notifications', 'Privacy & security', 'Account'];
  activeTab = 'General';
  saved = false;
  panels: Record<string, SettingsPanel> = {
  "General": {
    "title": "Platform controls",
    "description": "Configure publishing and automated safety rules.",
    "rows": [
      {
        "label": "AI abuse detection",
        "hint": "Auto-flag posts, comments, and messages",
        "enabled": true
      },
      {
        "label": "Sentiment analysis",
        "hint": "Analyse comments and reviews",
        "enabled": true
      },
      {
        "label": "Course approval required",
        "hint": "Review courses before publishing",
        "enabled": true
      }
    ]
  },
  "Notifications": {
    "title": "Admin alerts",
    "description": "Choose which operational events need attention.",
    "rows": [
      {
        "label": "High-risk flag alerts",
        "hint": "Immediate trust and safety alert",
        "enabled": true
      },
      {
        "label": "College applications",
        "hint": "When verification documents arrive",
        "enabled": true
      },
      {
        "label": "Weekly platform digest",
        "hint": "Users, revenue, and moderation summary",
        "enabled": true
      }
    ]
  },
  "Privacy & security": {
    "title": "Privacy & security",
    "description": "Enforce administrative protection.",
    "rows": [
      {
        "label": "Two-factor authentication",
        "hint": "Required for every administrator",
        "enabled": true
      },
      {
        "label": "Session timeout",
        "hint": "Sign out inactive admins after 30 minutes",
        "enabled": true
      },
      {
        "label": "Login audit alerts",
        "hint": "Alert on a new device or location",
        "enabled": true
      }
    ]
  },
  "Account": {
    "title": "Administration account",
    "description": "Manage access, exports, and administrator controls.",
    "rows": [
      {
        "label": "Admin role",
        "hint": "Full platform access",
        "value": "Super Admin"
      },
      {
        "label": "Audit log",
        "hint": "Review recent administrator actions",
        "action": "View"
      },
      {
        "label": "Manage admins",
        "hint": "Add, remove, or update access",
        "action": "Open"
      }
    ]
  }
};
  toggle(row: SettingRow): void { row.enabled = !row.enabled; this.showSaved(); }
  showSaved(): void { this.saved = true; window.setTimeout(() => this.saved = false, 1600); }
}

