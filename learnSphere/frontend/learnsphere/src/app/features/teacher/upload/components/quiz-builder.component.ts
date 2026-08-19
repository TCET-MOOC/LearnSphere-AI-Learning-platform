import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { TeacherService } from '../../services/teacher.service';
import { TeacherAssessmentService } from '../../services/assessment.service';
import { Course } from '@core/models/course.model';
import { QuestionDraft } from '@core/models/assessment.model';
import { NotificationService } from '@core/services/notification.service';

const QUESTION_TYPES = ['MCQ', 'SHORT_ANSWER', 'ESSAY'];

@Component({
  selector: 'app-quiz-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div class="quiz-builder">
      <div class="builder-header">
        <div>
          <h3>Course Assessments & Quiz Manager</h3>
          <p class="hint">Create assessments, add auto-graded MCQ questions, or manage existing tests.</p>
        </div>
        <button 
          class="btn btn--primary" 
          *ngIf="!showCreateForm && !createdTestId" 
          (click)="openNewTestForm()">
          <lucide-icon name="plus" [size]="15"></lucide-icon>
          <span>Build New Test</span>
        </button>
        <button 
          class="btn btn--ghost" 
          *ngIf="showCreateForm || createdTestId" 
          (click)="cancelOrFinish()">
          <lucide-icon name="arrow-left" [size]="15"></lucide-icon>
          <span>Back to Tests List</span>
        </button>
      </div>

      <!-- Existing Tests List View -->
      <div class="existing-tests-view" *ngIf="!showCreateForm && !createdTestId">
        <div class="loading-state" *ngIf="loadingTests">
          <span>Loading course tests...</span>
        </div>

        <div class="tests-grid" *ngIf="!loadingTests && existingTests.length > 0">
          <div class="test-card" *ngFor="let t of existingTests">
            <div class="test-card-head">
              <div class="test-title-box">
                <h4>{{ t.title }}</h4>
                <div class="test-badges">
                  <span class="badge duration-badge">
                    <lucide-icon name="clock" [size]="12"></lucide-icon>
                    {{ t.durationMinutes }} mins
                  </span>
                  <span class="badge remedial-badge" *ngIf="t.isRemedial">Remedial / Makeup</span>
                </div>
              </div>
              <div class="test-card-actions">
                <button class="btn btn--soft btn--sm" (click)="manageTest(t)">
                  <lucide-icon name="plus" [size]="13"></lucide-icon>
                  <span>Add Questions</span>
                </button>
                <button class="btn btn--danger-outline btn--sm" (click)="deleteTest(t)">
                  <lucide-icon name="trash-2" [size]="13"></lucide-icon>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-tests" *ngIf="!loadingTests && existingTests.length === 0">
          <lucide-icon name="file-question" [size]="32" class="empty-icon"></lucide-icon>
          <p>No tests created for this course yet.</p>
          <button class="btn btn--primary btn--sm" (click)="openNewTestForm()">Create First Test</button>
        </div>
      </div>

      <!-- Test Creation Form -->
      <div class="test-form-panel" *ngIf="showCreateForm && !createdTestId">
        <div class="form-grid">
          <label *ngIf="!initialCourseId">
            Target Course
            <select [(ngModel)]="courseId" [disabled]="courses.length === 0">
              <option [ngValue]="null" disabled>Select a course…</option>
              <option *ngFor="let c of courses" [ngValue]="c.id">{{ c.title }}</option>
            </select>
          </label>
          <label [class.full]="!!initialCourseId">
            Test Title
            <input type="text" [(ngModel)]="title" placeholder="e.g. Midterm Assessment: Neural Networks" />
          </label>
          <label>
            Duration (Minutes)
            <input type="number" min="1" [(ngModel)]="durationMinutes" placeholder="e.g. 30" />
          </label>
          <label class="checkbox-row">
            <input type="checkbox" [(ngModel)]="isRemedial" id="remedial-check" />
            <span>Mark as Remedial / Makeup Credit Test (Pass threshold $\\ge 40\%$)</span>
          </label>
        </div>

        <div class="form-actions">
          <button
            type="button"
            class="btn btn--primary"
            [disabled]="!courseId || !title || !durationMinutes || creatingTest"
            (click)="createTest()">
            <span>{{ creatingTest ? 'Creating Test…' : 'Next: Add Questions →' }}</span>
          </button>
        </div>
      </div>

      <!-- Questions Authoring Section (Active Test) -->
      <div class="questions-section" *ngIf="createdTestId">
        <div class="active-test-banner">
          <div>
            <span class="active-label">Active Test:</span>
            <strong>{{ title }}</strong>
            <span class="badge" *ngIf="durationMinutes">({{ durationMinutes }} mins)</span>
          </div>
          <button class="btn btn--ghost btn--sm" (click)="cancelOrFinish()">Finish & Save</button>
        </div>

        <!-- Saved Questions List -->
        <div class="saved-questions-box" *ngIf="savedQuestions.length > 0">
          <h5>Saved Questions ({{ savedQuestions.length }})</h5>
          <div class="saved-question" *ngFor="let q of savedQuestions; let i = index">
            <div class="saved-q-head">
              <strong>{{ i + 1 }}. {{ q.body }}</strong>
              <span class="saved-q-meta">{{ q.questionType }} · {{ q.marks }} mark{{ q.marks === 1 ? '' : 's' }}</span>
            </div>
            <div class="saved-q-options" *ngIf="q.options?.length">
              <span 
                class="opt-chip" 
                *ngFor="let opt of q.options"
                [class.is-correct]="opt === q.correctAnswer">
                {{ opt }} {{ opt === q.correctAnswer ? '✓' : '' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Add Question Card -->
        <div class="add-q-card">
          <h4>Add New Question</h4>

          <div class="form-grid">
            <label class="full">
              Question Text / Problem Statement
              <textarea rows="2" [(ngModel)]="draftBody" placeholder="e.g. What is the time complexity of Dijkstra's algorithm with a binary heap?"></textarea>
            </label>
            <label>
              Question Type
              <select [(ngModel)]="draftType">
                <option *ngFor="let t of questionTypes" [value]="t">{{ t }}</option>
              </select>
            </label>
            <label>
              Marks
              <input type="number" min="1" [(ngModel)]="draftMarks" />
            </label>
          </div>

          <!-- MCQ Options Selector -->
          <div class="options-section" *ngIf="draftType === 'MCQ'">
            <label class="full">Answer Options (Select the correct choice):</label>
            <div class="option-row" *ngFor="let opt of draftOptions; let i = index">
              <input type="text" [(ngModel)]="draftOptions[i]" [placeholder]="'Option ' + (i + 1)" />
              <label class="correct-toggle">
                <input 
                  type="radio" 
                  name="correct-option" 
                  [checked]="draftCorrectAnswer === draftOptions[i] && draftOptions[i] !== ''" 
                  (change)="draftCorrectAnswer = draftOptions[i]" 
                />
                <span>Correct Answer</span>
              </label>
              <button type="button" class="btn-icon" (click)="removeOption(i)" title="Remove option">
                <lucide-icon name="x" [size]="14"></lucide-icon>
              </button>
            </div>
            <button type="button" class="btn btn--ghost btn--sm" (click)="addOption()">
              <lucide-icon name="plus" [size]="13"></lucide-icon>
              <span>Add Option</span>
            </button>
          </div>

          <!-- Non-MCQ Key -->
          <label class="full" *ngIf="draftType !== 'MCQ'">
            Model Answer Key (Optional reference for instructor grading):
            <input type="text" [(ngModel)]="draftCorrectAnswer" placeholder="Reference answer phrase" />
          </label>

          <div class="actions">
            <button
              type="button"
              class="btn btn--primary"
              [disabled]="!draftBody || !draftMarks || addingQuestion || (draftType === 'MCQ' && !draftCorrectAnswer)"
              (click)="addQuestion()">
              <lucide-icon name="plus" [size]="14"></lucide-icon>
              <span>{{ addingQuestion ? 'Adding Question…' : 'Add Question' }}</span>
            </button>
            <button type="button" class="btn btn--ghost" (click)="cancelOrFinish()">Done — Finish Test</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .quiz-builder {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: 14px;
      padding: 22px;
      display: flex;
      flex-direction: column;
      gap: 18px;
      box-shadow: var(--card-shadow);
      transition: all 0.2s ease;
    }

    .builder-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;

      h3 { margin: 0; font-size: 16px; font-weight: 700; color: var(--text-primary); }
      .hint { margin: 3px 0 0; font-size: 12px; color: var(--text-muted); }
    }

    .existing-tests-view {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .tests-grid {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .test-card {
      background: var(--bg-surface-elevated);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      padding: 14px 18px;
      transition: border-color 0.15s ease;

      &:hover {
        border-color: var(--brand-primary);
      }
    }

    .test-card-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      flex-wrap: wrap;
    }

    .test-title-box {
      h4 { margin: 0 0 6px 0; font-size: 14px; font-weight: 600; color: var(--text-primary); }
      .test-badges { display: flex; align-items: center; gap: 8px; }
    }

    .badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      font-weight: 600;
      padding: 3px 8px;
      border-radius: 6px;
      background: var(--bg-hover);
      color: var(--text-secondary);

      &.remedial-badge {
        background: var(--status-amber-bg);
        color: var(--status-amber-text);
      }
    }

    .test-card-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .empty-tests {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 32px 16px;
      text-align: center;
      gap: 10px;
      color: var(--text-muted);
      border: 1px dashed var(--border-color);
      border-radius: 10px;

      .empty-icon { opacity: 0.5; }
      p { margin: 0; font-size: 13px; }
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }

    label {
      display: flex;
      flex-direction: column;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
    }

    label.full { grid-column: 1 / -1; }

    input[type=text], input[type=number], select, textarea {
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 9px 12px;
      font: 13px 'Inter', sans-serif;
      color: var(--text-primary);
      background: var(--bg-input);
      transition: all 0.15s ease;
      outline: none;
      resize: vertical;

      &:focus {
        border-color: var(--brand-primary);
        background: var(--bg-surface);
      }
    }

    .checkbox-row {
      flex-direction: row !important;
      align-items: center;
      gap: 8px !important;
      color: var(--text-primary);
      font-weight: 500 !important;
      cursor: pointer;
      grid-column: 1 / -1;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 14px;
    }

    .active-test-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: var(--brand-surface);
      border: 1px solid var(--brand-primary);
      padding: 12px 16px;
      border-radius: 10px;
      color: var(--brand-primary);

      .active-label { font-size: 12px; font-weight: 500; opacity: 0.85; margin-right: 6px; }
      strong { font-size: 14px; font-weight: 700; color: var(--text-primary); }
    }

    .saved-questions-box {
      display: flex;
      flex-direction: column;
      gap: 8px;
      background: var(--bg-surface-elevated);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      padding: 14px;

      h5 { margin: 0 0 6px 0; font-size: 13px; font-weight: 700; color: var(--text-primary); }
    }

    .saved-question {
      border-bottom: 1px solid var(--border-color);
      padding: 8px 0;
      &:last-child { border-bottom: none; }
    }

    .saved-q-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      font-size: 12.5px;
      color: var(--text-primary);
    }

    .saved-q-meta {
      font-size: 11px;
      color: var(--text-muted);
      white-space: nowrap;
    }

    .saved-q-options {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 6px;

      .opt-chip {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 4px;
        background: var(--bg-hover);
        color: var(--text-secondary);

        &.is-correct {
          background: var(--status-green-bg);
          color: var(--status-green-text);
          font-weight: 600;
        }
      }
    }

    .add-q-card {
      background: var(--bg-hover);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 14px;

      h4 { margin: 0; font-size: 14px; font-weight: 700; color: var(--text-primary); }
    }

    .options-section {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .option-row {
      display: flex;
      align-items: center;
      gap: 10px;

      input[type=text] { flex: 1; }
    }

    .correct-toggle {
      flex-direction: row !important;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--text-secondary);
      white-space: nowrap;
      cursor: pointer;
    }

    .btn-icon {
      border: none;
      background: transparent;
      color: var(--status-red-text);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px;
    }

    .actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .btn {
      border: 1px solid transparent;
      border-radius: 8px;
      padding: 9px 16px;
      font: 600 12.5px 'Inter', sans-serif;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
      text-decoration: none;

      &--sm { padding: 6px 12px; font-size: 11.5px; }
      &--primary { background: var(--brand-primary); color: #fff; }
      &--primary:hover:not(:disabled) { background: var(--brand-primary-hover); }
      &--primary:disabled { opacity: 0.5; cursor: not-allowed; }
      &--soft { background: var(--brand-surface); color: var(--brand-primary); border-color: var(--border-color); }
      &--soft:hover { background: var(--brand-primary); color: #fff; }
      &--ghost { background: var(--bg-surface-elevated); border-color: var(--border-color); color: var(--text-primary); }
      &--ghost:hover { background: var(--bg-hover); }
      &--danger-outline { background: transparent; border-color: var(--status-red-bg); color: var(--status-red-text); }
      &--danger-outline:hover { background: var(--status-red-text); color: #fff; }
    }
  `]
})
export class QuizBuilderComponent implements OnInit, OnChanges {
  @Input() initialCourseId: number | null = null;

  courses: Course[] = [];
  existingTests: any[] = [];
  loadingTests = false;

  courseId: number | null = null;
  title = '';
  durationMinutes: number | null = null;
  isRemedial = false;
  creatingTest = false;
  createdTestId: number | null = null;
  showCreateForm = false;

  questionTypes = QUESTION_TYPES;
  savedQuestions: QuestionDraft[] = [];

  draftBody = '';
  draftType = 'MCQ';
  draftMarks: number | null = 1;
  draftOptions: string[] = ['', ''];
  draftCorrectAnswer = '';
  addingQuestion = false;

  constructor(
    private teacherService: TeacherService,
    private teacherAssessmentService: TeacherAssessmentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    if (this.initialCourseId) {
      this.courseId = this.initialCourseId;
      this.loadTests();
    }
    this.teacherService.getCourses().subscribe({
      next: (courses) => {
        this.courses = courses;
        if (!this.courseId && this.courses.length > 0) {
          this.courseId = this.courses[0].id;
          this.loadTests();
        }
      },
      error: () => this.notificationService.error('Could not load your courses.')
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialCourseId'] && this.initialCourseId) {
      this.courseId = this.initialCourseId;
      this.loadTests();
    }
  }

  loadTests(): void {
    if (!this.courseId) return;
    this.loadingTests = true;
    this.teacherAssessmentService.getTestsForCourse(this.courseId).subscribe({
      next: (tests) => {
        this.existingTests = tests || [];
        this.loadingTests = false;
      },
      error: () => {
        this.existingTests = [];
        this.loadingTests = false;
      }
    });
  }

  openNewTestForm(): void {
    this.showCreateForm = true;
    this.createdTestId = null;
    this.title = '';
    this.durationMinutes = 30;
    this.isRemedial = false;
  }

  manageTest(test: any): void {
    this.createdTestId = test.testId || test.id;
    this.title = test.title;
    this.durationMinutes = test.durationMinutes;
    this.isRemedial = !!test.isRemedial;
    this.savedQuestions = [];
    this.resetDraft();
  }

  deleteTest(test: any): void {
    const id = test.testId || test.id;
    if (!id) return;
    if (confirm(`Delete test "${test.title}"?`)) {
      this.teacherAssessmentService.deleteTest(id).subscribe({
        next: () => {
          this.notificationService.success('Test deleted.');
          this.loadTests();
        },
        error: () => this.notificationService.error('Could not delete test.')
      });
    }
  }

  createTest(): void {
    if (!this.courseId || !this.title || !this.durationMinutes) return;
    this.creatingTest = true;
    this.teacherAssessmentService
      .createTest({
        course: { id: this.courseId },
        title: this.title,
        durationMinutes: this.durationMinutes,
        isRemedial: this.isRemedial
      })
      .subscribe({
        next: (test) => {
          this.createdTestId = test?.testId || test?.id;
          this.creatingTest = false;
          this.showCreateForm = false;
          this.notificationService.success('Test created — now add its questions.');
          this.loadTests();
        },
        error: (err) => {
          this.creatingTest = false;
          const msg = err?.error?.message || err?.message || 'Could not create the test.';
          this.notificationService.error(msg);
        }
      });
  }

  addOption(): void {
    this.draftOptions.push('');
  }

  removeOption(index: number): void {
    const removed = this.draftOptions[index];
    this.draftOptions.splice(index, 1);
    if (this.draftCorrectAnswer === removed) {
      this.draftCorrectAnswer = '';
    }
  }

  addQuestion(): void {
    if (!this.createdTestId || !this.draftBody || !this.draftMarks) return;
    const isMcq = this.draftType === 'MCQ';
    const options = isMcq ? this.draftOptions.filter((o) => o.trim().length > 0) : undefined;

    const question: QuestionDraft = {
      body: this.draftBody,
      questionType: this.draftType,
      marks: this.draftMarks,
      options,
      correctAnswer: this.draftCorrectAnswer || undefined
    };

    this.addingQuestion = true;
    this.teacherAssessmentService.addQuestion(this.createdTestId, question).subscribe({
      next: () => {
        this.savedQuestions.push(question);
        this.addingQuestion = false;
        this.resetDraft();
        this.notificationService.success('Question added to test.');
      },
      error: (err) => {
        this.addingQuestion = false;
        this.notificationService.error(err?.error?.message || 'Could not add that question.');
      }
    });
  }

  cancelOrFinish(): void {
    this.createdTestId = null;
    this.showCreateForm = false;
    this.title = '';
    this.durationMinutes = null;
    this.isRemedial = false;
    this.savedQuestions = [];
    this.resetDraft();
    this.loadTests();
  }

  private resetDraft(): void {
    this.draftBody = '';
    this.draftType = 'MCQ';
    this.draftMarks = 1;
    this.draftOptions = ['', ''];
    this.draftCorrectAnswer = '';
  }
}
