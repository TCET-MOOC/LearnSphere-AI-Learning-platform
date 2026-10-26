import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface LectureContext {
  currentTimestamp: number;
  activeCueText: string;
}

const EMPTY_CONTEXT: LectureContext = {
  currentTimestamp: 0,
  activeCueText: ''
};

@Injectable({ providedIn: 'root' })
export class LectureContextService {
  private contextSubject = new BehaviorSubject<LectureContext>({ ...EMPTY_CONTEXT });

  readonly context$ = this.contextSubject.asObservable();

  get snapshot(): LectureContext {
    return this.contextSubject.value;
  }

  updateContext(partial: Partial<LectureContext>): void {
    this.contextSubject.next({ ...this.contextSubject.value, ...partial });
  }

  reset(): void {
    this.contextSubject.next({ ...EMPTY_CONTEXT });
  }
}
