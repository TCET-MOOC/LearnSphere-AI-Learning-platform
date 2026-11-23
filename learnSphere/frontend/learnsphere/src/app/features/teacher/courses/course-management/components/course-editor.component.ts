import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Course, CourseStatus } from '@core/models/course.model';
import { TEACHER_COURSES_COURSE_MANAGEMENT_COURSE_EDITOR_UI } from '@core/content/teacher-courses-course-management-course-editor.ui';

/**
 * CourseEditorComponent is the reactive form for creating/editing a course's
 * core metadata (title, description, department, thumbnail, price, status).
 * Used by CourseManagementComponent for both the "new course" and "edit
 * course" flows.
 */
@Component({
  selector: 'app-course-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form class="course-form" [formGroup]="form" (ngSubmit)="onSubmit()">
      <label>
        {{ ui.title }}
        <input type="text" formControlName="title" [attr.placeholder]="ui.eGEngineeringMathematics" />
      </label>

      <label class="full">
        {{ ui.description }}
        <textarea formControlName="description" rows="4" [attr.placeholder]="ui.whatWillStudentsLearn"></textarea>
      </label>

      <label>
        {{ ui.department }}
        <input type="text" formControlName="department" [attr.placeholder]="ui.eGMathematics" />
      </label>

      <label>
        {{ ui.thumbnailUrl }}
        <input type="text" formControlName="thumbnail" [attr.placeholder]="ui.https" />
      </label>

      <label>
        {{ ui.price0Free }}
        <input type="number" min="0" formControlName="price" />
      </label>

      <label *ngIf="isEditMode">
        {{ ui.status }}
        <select formControlName="status">
          <option value="DRAFT">{{ ui.draft }}</option>
          <option value="PENDING">{{ ui.pendingReview }}</option>
          <option value="LIVE">{{ ui.live }}</option>
          <option value="ARCHIVED">{{ ui.archived }}</option>
        </select>
      </label>

      <div class="form-actions full">
        <button type="submit" class="btn btn--primary" [disabled]="form.invalid || saving">
          {{ saving ? 'Saving…' : (isEditMode ? 'Save changes' : 'Create course') }}
        </button>
      </div>
    </form>
  `,
  styles: [`
    .course-form {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
    }
    /* Phones (360 px audit): one column; fields ran ~100 px past the card. */
    @media (max-width: 599.98px) {
      .course-form { grid-template-columns: minmax(0, 1fr); }
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
    input, textarea, select {
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius-md);
      padding: 10px 12px;
      font: 13px 'Inter', sans-serif;
      color: var(--text-primary);
      background: var(--bg-input);
      transition: border-color 0.15s ease, background 0.15s ease;
      resize: vertical;
      box-sizing: border-box;
      width: 100%;
      min-width: 0;
    }
    input:focus, textarea:focus, select:focus {
      outline: none;
      border-color: var(--brand-primary);
      background: var(--bg-surface);
    }
    .form-actions { display: flex; justify-content: flex-end; }
    .btn { border: none; border-radius: var(--border-radius-md); padding: 10px 20px; font: 600 13px 'Inter', sans-serif; cursor: pointer; transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease, opacity 0.15s ease; }
    .btn--primary { background: var(--brand-primary); color: var(--palette-white); }
    .btn--primary:hover:not(:disabled) { background: var(--brand-primary-hover); }
    .btn--primary:disabled { opacity: 0.6; cursor: not-allowed; }
  `]
})
export class CourseEditorComponent implements OnChanges {
  /** Template copy (Wave 7 gate): `core/content/teacher-courses-course-management-course-editor.ui.ts`. */
  protected readonly ui = TEACHER_COURSES_COURSE_MANAGEMENT_COURSE_EDITOR_UI;
  @Input() course: Course | null = null;
  @Input() saving = false;
  @Output() save = new EventEmitter<Partial<Course>>();

  form: FormGroup;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      department: ['', Validators.required],
      thumbnail: [''],
      price: [0, [Validators.required, Validators.min(0)]],
      status: ['DRAFT']
    });
  }

  get isEditMode(): boolean {
    return !!this.course?.id;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['course'] && this.course) {
      this.form.patchValue({
        title: this.course.title,
        description: this.course.description,
        department: this.course.department,
        thumbnail: this.course.thumbnail,
        price: this.course.price,
        status: this.course.status
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const value = this.form.value;
    const payload: Partial<Course> = {
      title: value.title,
      description: value.description,
      department: value.department,
      thumbnail: value.thumbnail,
      price: value.price
    };
    if (this.isEditMode) {
      payload.status = value.status as CourseStatus;
    }
    this.save.emit(payload);
  }
}
