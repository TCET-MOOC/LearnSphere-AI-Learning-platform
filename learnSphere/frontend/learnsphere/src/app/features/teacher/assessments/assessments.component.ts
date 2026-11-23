import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DataTableComponent, TableColumn } from '@shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { NotificationService } from '@core/services/notification.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { TEACHER_ASSESSMENTS_COPY } from '@core/content/teacher-assessments.content';
import { PendingAnswer, TeacherAssessmentService, TeacherTestSummary } from '../services/assessment.service';

/**
 * Teacher assessments (SCR-35; W7-16, W4-05, AC-17). Lists the teacher's tests per course and
 * lecture and the TEXT answers that wait for a grade. Grading goes through
 * `PATCH /answers/{id}/grade` (the server bounds the score and recomputes the attempt); the queue
 * and counts are then read again from the server. Authoring stays in the quiz builder (Upload).
 */
@Component({
  selector: 'app-teacher-assessments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DataTableComponent, EmptyStateComponent],
  templateUrl: './assessments.component.html',
  styleUrls: ['./assessments.component.scss'],
})
export class TeacherAssessmentsComponent implements OnInit {
  protected readonly copy = TEACHER_ASSESSMENTS_COPY;

  readonly columns: TableColumn[] = [
    { key: 'courseTitle', header: TEACHER_ASSESSMENTS_COPY.columns.course, sortable: true },
    { key: 'lecture', header: TEACHER_ASSESSMENTS_COPY.columns.lecture },
    { key: 'title', header: TEACHER_ASSESSMENTS_COPY.columns.test, sortable: true },
    { key: 'questionCount', header: TEACHER_ASSESSMENTS_COPY.columns.questions, sortable: true, type: 'number' },
    { key: 'pendingCount', header: TEACHER_ASSESSMENTS_COPY.columns.pending, sortable: true, type: 'number' },
  ];

  tests: (TeacherTestSummary & { lecture: string })[] = [];
  pending: PendingAnswer[] = [];
  loading = true;
  loadError = false;
  marks: Record<number, number | null> = {};
  errors: Record<number, string | null> = {};
  savingId: number | null = null;

  constructor(
    private api: TeacherAssessmentService,
    private notify: NotificationService,
    private errorHandler: ErrorHandlerService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    forkJoin({ tests: this.api.getTeacherAssessments(), pending: this.api.getPendingAnswers() }).subscribe({
      next: ({ tests, pending }) => {
        this.tests = tests.map((t) => ({ ...t, lecture: t.lectureTitle ?? this.copy.wholeCourse }));
        this.pending = pending;
        this.loading = false;
        this.loadError = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
        this.cdr.markForCheck();
      },
    });
  }

  max(a: PendingAnswer): number {
    return a.maxMarks ?? 100;
  }

  save(a: PendingAnswer): void {
    const value = this.marks[a.answerId];
    const max = this.max(a);
    if (value === null || value === undefined || Number.isNaN(Number(value)) || value < 0 || value > max) {
      this.errors[a.answerId] = this.copy.outOfRange(max);
      return;
    }
    this.errors[a.answerId] = null;
    this.savingId = a.answerId;
    this.api.gradeAnswer(a.answerId, Number(value)).subscribe({
      next: () => {
        this.savingId = null;
        this.notify.success(this.copy.graded(a.studentName ?? this.copy.unknownStudent));
        this.load();
      },
      error: (err) => {
        this.savingId = null;
        const message = this.errorHandler.extractMessage(err, this.copy.gradeFailed);
        this.errors[a.answerId] = message;
        this.notify.error(message);
        this.cdr.markForCheck();
      },
    });
  }
}
