import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Course, CourseStatus } from '@core/models/course.model';

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
        Title
        <input type="text" formControlName="title" placeholder="e.g. Engineering Mathematics III" />
      </label>

      <label class="full">
        Description
        <textarea formControlName="description" rows="4" placeholder="What will students learn?"></textarea>
      </label>

      <label>
        Department
        <input type="text" formControlName="department" placeholder="e.g. Mathematics" />
      </label>

      <label>
        Thumbnail URL
        <input type="text" formControlName="thumbnail" placeholder="https://…" />
      </label>

      <label>
        Price (0 = free)
        <input type="number" min="0" formControlName="price" />
      </label>

      <label *ngIf="isEditMode">
        Status
        <select formControlName="status">
          <option value="DRAFT">Draft</option>
          <option value="PENDING">Pending review</option>
          <option value="LIVE">Live</option>
          <option value="ARCHIVED">Archived</option>
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
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }
    label {
      display: flex;
      flex-direction: column;
      gap: 6px;
      font-size: 11.5px;
      color: #5f5b70;
    }
    label.full { grid-column: 1 / -1; }
    input, textarea, select {
      border: 1px solid #dedce7;
      border-radius: 8px;
      padding: 9px 10px;
      font: 12px Inter, Arial;
      color: #1a1830;
      background: #fff;
      resize: vertical;
    }
    .form-actions { display: flex; justify-content: flex-end; }
    .btn { border: none; border-radius: 8px; padding: 9px 16px; font: 600 12px Inter, Arial; cursor: pointer; }
    .btn--primary { background: #534ab7; color: #fff; }
    .btn--primary:disabled { opacity: 0.6; cursor: default; }
  `]
})
export class CourseEditorComponent implements OnChanges {
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
