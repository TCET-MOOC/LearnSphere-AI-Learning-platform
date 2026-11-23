import { Component } from '@angular/core';
import { AccountSettingsComponent } from '@shared/components/account-settings/account-settings.component';
import { STUDENT_SETTINGS_COPY } from '@core/content/student-settings.content';

/** Student settings (W7-12): the shared account settings with this role's rows and copy. */
@Component({
  selector: 'app-student-settings',
  standalone: true,
  imports: [AccountSettingsComponent],
  template: '<app-account-settings [copy]="copy"></app-account-settings>',
})
export class SettingsComponent {
  readonly copy = STUDENT_SETTINGS_COPY;
}
