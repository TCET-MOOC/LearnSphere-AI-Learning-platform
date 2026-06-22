import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type AppRole = 'student' | 'teacher' | 'admin';

@Injectable({ providedIn: 'root' })
export class NotificationStateService {
  private readonly counts = new BehaviorSubject<Record<AppRole, number>>({
    student: 4,
    teacher: 4,
    admin: 4
  });

  readonly counts$ = this.counts.asObservable();

  count(role: AppRole): number {
    return this.counts.value[role];
  }

  markOneRead(role: AppRole): void {
    const current = this.count(role);
    if (current > 0) this.update(role, current - 1);
  }

  markAllRead(role: AppRole): void {
    this.update(role, 0);
  }

  private update(role: AppRole, count: number): void {
    this.counts.next({ ...this.counts.value, [role]: count });
  }
}
