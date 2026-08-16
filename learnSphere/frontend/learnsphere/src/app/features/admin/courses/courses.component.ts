import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Course, CourseStatus } from '@core/models/course.model';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit {
  loading = false;
  allCourses: Course[] = [];
  courses: Course[] = [];
  activeFilter: 'ALL' | CourseStatus = 'ALL';
  actingId: number | null = null;

  filters: { id: 'ALL' | CourseStatus; label: string }[] = [
    { id: 'ALL', label: 'All' },
    { id: 'PENDING', label: 'Pending review' },
    { id: 'LIVE', label: 'Live' },
    { id: 'DRAFT', label: 'Draft' },
    { id: 'ARCHIVED', label: 'Archived' }
  ];

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.apiService.get<Course[]>('/courses').subscribe({
      next: (courses) => {
        this.allCourses = courses;
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.notificationService.error('Failed to load courses.');
        this.loading = false;
      }
    });
  }

  setFilter(filter: 'ALL' | CourseStatus): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.courses = this.activeFilter === 'ALL' ? this.allCourses : this.allCourses.filter((c) => c.status === this.activeFilter);
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
    this.setStatus(course, 'LIVE', `"${course.title}" approved and is now live.`);
  }

  reject(course: Course): void {
    this.setStatus(course, 'DRAFT', `"${course.title}" sent back to draft.`);
  }

  archive(course: Course): void {
    this.setStatus(course, 'ARCHIVED', `"${course.title}" archived.`);
  }

  reactivate(course: Course): void {
    this.setStatus(course, 'LIVE', `"${course.title}" is live again.`);
  }
}
