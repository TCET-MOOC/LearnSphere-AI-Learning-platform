import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LucideAngularModule } from 'lucide-angular';
import { catchError, of } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { Course, CourseStatus } from '@core/models/course.model';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    LucideAngularModule
  ],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit {
  loading = false;
  allCourses: Course[] = [];
  courses: Course[] = [];
  activeFilter: 'ALL' | CourseStatus = 'ALL';
  actingId: number | null = null;
  searchQuery = '';

  filters: { id: 'ALL' | CourseStatus; label: string }[] = [
    { id: 'ALL', label: 'All' },
    { id: 'PENDING', label: 'Pending review' },
    { id: 'LIVE', label: 'Live' },
    { id: 'DRAFT', label: 'Draft' },
    { id: 'ARCHIVED', label: 'Archived' }
  ];

  selectedCourseForInspect: Course | null = null;
  inspectLectures: any[] = [];
  loadingLectures = false;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    // GET /api/courses lists all courses. Status changes go through PUT /api/admin/courses/:id/status.
    this.apiService.get<Course[]>('/courses').pipe(
      catchError(() => {
        this.notificationService.error('Failed to load courses.');
        return of([]);
      })
    ).subscribe((courses) => {
      this.allCourses = courses;
      this.applyFilter();
      this.loading = false;
      this.cdr.markForCheck();
    });
  }

  setFilter(filter: 'ALL' | CourseStatus): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  onSearchChange(): void {
    this.applyFilter();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applyFilter();
  }

  private applyFilter(): void {
    let result = this.activeFilter === 'ALL' ? this.allCourses : this.allCourses.filter((c) => c.status === this.activeFilter);
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      result = result.filter(c =>
        c.title.toLowerCase().includes(q) ||
        (c.teacherName && c.teacherName.toLowerCase().includes(q)) ||
        (c.department && c.department.toLowerCase().includes(q))
      );
    }
    this.courses = result;
  }

  count(filter: 'ALL' | CourseStatus): number {
    return filter === 'ALL' ? this.allCourses.length : this.allCourses.filter((c) => c.status === filter).length;
  }

  statusClass(status: CourseStatus): string {
    return {
      PENDING: 'pill--amber',
      LIVE: 'pill--green',
      DRAFT: 'pill--grey',
      ARCHIVED: 'pill--dark'
    }[status];
  }

  setStatus(course: Course, status: CourseStatus, message: string): void {
    this.actingId = course.id;
    this.apiService.put<Course>(`/admin/courses/${course.id}/status`, { status }).subscribe({
      next: () => {
        this.actingId = null;
        this.notificationService.success(message);
        this.load();
      },
      error: () => {
        this.actingId = null;
        this.notificationService.error('Failed to update course status.');
      }
    });
  }

  approve(course: Course): void {
    this.setStatus(course, 'LIVE', `"${course.title}" approved and published live.`);
  }

  reject(course: Course): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Reject Course Submission',
        message: `Are you sure you want to send "${course.title}" back to draft status? The teacher will need to revise and resubmit.`,
        confirmLabel: 'Reject to Draft',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.setStatus(course, 'DRAFT', `"${course.title}" sent back to draft.`);
      }
    });
  }

  archive(course: Course): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Archive Course',
        message: `Are you sure you want to archive "${course.title}"? It will no longer be visible to new students in catalog.`,
        confirmLabel: 'Archive Course',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.setStatus(course, 'ARCHIVED', `"${course.title}" has been archived.`);
      }
    });
  }

  reactivate(course: Course): void {
    this.setStatus(course, 'LIVE', `"${course.title}" is live again.`);
  }

  inspectCourse(course: Course): void {
    this.selectedCourseForInspect = course;
    this.loadingLectures = true;
    this.cdr.markForCheck();
    this.apiService.get<any[]>(`/courses/${course.id}/lectures`).subscribe({
      next: (lectures) => {
        this.inspectLectures = lectures || [];
        this.loadingLectures = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.inspectLectures = [];
        this.loadingLectures = false;
        this.cdr.markForCheck();
      }
    });
  }

  closeInspect(): void {
    this.selectedCourseForInspect = null;
    this.inspectLectures = [];
    this.cdr.markForCheck();
  }
}
