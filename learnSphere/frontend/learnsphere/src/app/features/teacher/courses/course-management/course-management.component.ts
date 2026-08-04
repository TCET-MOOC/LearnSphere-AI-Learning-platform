import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TeacherService } from '../../services/teacher.service';
import { Course } from '@core/models/course.model';
import { CourseEditorComponent } from './components/course-editor.component';
import { LectureListComponent } from './components/lecture-list.component';
import { NotificationService } from '@core/services/notification.service';

/**
 * CourseManagementComponent is the single-course authoring page: create a
 * new course, or edit an existing one's metadata and manage its lectures.
 */
@Component({
  selector: 'app-course-management',
  standalone: true,
  imports: [CommonModule, RouterModule, CourseEditorComponent, LectureListComponent],
  templateUrl: './course-management.component.html',
  styleUrls: ['./course-management.component.scss']
})
export class CourseManagementComponent implements OnInit {
  courseId: number | null = null;
  course: Course | null = null;
  loading = false;
  saving = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private teacherService: TeacherService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.courseId = Number(idParam);
      this.load();
    }
  }

  get isEditMode(): boolean {
    return !!this.courseId;
  }

  load(): void {
    if (!this.courseId) return;
    this.loading = true;
    this.teacherService.getCourse(this.courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Could not load course.');
        this.router.navigate(['/teacher/courses']);
      }
    });
  }

  onSave(payload: Partial<Course>): void {
    this.saving = true;
    if (this.isEditMode && this.courseId) {
      this.teacherService.updateCourse(this.courseId, payload).subscribe({
        next: (course) => {
          this.course = course;
          this.saving = false;
          this.notificationService.success('Course updated.');
        },
        error: () => {
          this.saving = false;
          this.notificationService.error('Could not update course.');
        }
      });
    } else {
      this.teacherService.createCourse(payload).subscribe({
        next: (course) => {
          this.saving = false;
          this.notificationService.success('Course created — now add some lectures.');
          this.router.navigate(['/teacher/courses', course.id, 'manage']);
        },
        error: () => {
          this.saving = false;
          this.notificationService.error('Could not create course.');
        }
      });
    }
  }
}
