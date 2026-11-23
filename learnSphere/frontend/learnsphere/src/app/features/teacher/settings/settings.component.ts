import { Component } from '@angular/core';
import { AccountSettingsComponent } from '@shared/components/account-settings/account-settings.component';
import { TEACHER_SETTINGS_COPY } from '@core/content/teacher-settings.content';

/** Teacher settings (W7-12): the shared account settings with this role's rows and copy. */
@Component({
  selector: 'app-teacher-settings',
  standalone: true,
  imports: [AccountSettingsComponent],
  template: '<app-account-settings [copy]="copy"></app-account-settings>',
})
export class SettingsComponent {
  readonly copy = TEACHER_SETTINGS_COPY;
}
