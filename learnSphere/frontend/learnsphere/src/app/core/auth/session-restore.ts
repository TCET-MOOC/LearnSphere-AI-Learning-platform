import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';

export { SILENT_AUTH } from './auth.context';

/**
 * Restores the in-memory session (W1-06) from the HttpOnly refresh cookie before the
 * first route is resolved, so a page reload keeps the user signed in and guards see
 * the real session. Without a valid cookie the app simply starts signed out.
 * (Pulled forward from W7-04; see TRACKER §4.)
 */
export function provideSessionRestore(): EnvironmentProviders {
  return provideAppInitializer(() => firstValueFrom(inject(AuthService).restoreSession()).then(() => undefined));
}
