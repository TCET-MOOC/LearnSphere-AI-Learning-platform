import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { TeacherService } from '../services/teacher.service';
import { Course } from '@core/models/course.model';
import { CourseCardComponent } from './components/course-card.component';
import { NotificationService } from '@core/services/notification.service';

type StatusFilter = 'ALL' | 'LIVE' | 'DRAFT' | 'PENDING' | 'ARCHIVED';

@Component({
  selector: 'app-teacher-courses',
  standalone: true,
  imports: [CommonModule, RouterModule, CourseCardComponent],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit, OnDestroy {
  loading = true;
  courses: Course[] = [];
  searchQuery = '';
  private queryParamSub?: Subscription;
  activeFilter: StatusFilter = 'ALL';
  filters: { key: StatusFilter; label: string }[] = [
    { key: 'ALL', label: 'All' },
    { key: 'LIVE', label: 'Live' },
    { key: 'DRAFT', label: 'Draft' },
    { key: 'PENDING', label: 'Pending' },
    { key: 'ARCHIVED', label: 'Archived' }
  ];

  constructor(
    private teacherService: TeacherService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Honor ?search= from the global topbar search box.
    this.queryParamSub = this.route.queryParamMap.subscribe((params) => {
      this.searchQuery = params.get('search') || '';
      this.cdr.markForCheck();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this.queryParamSub?.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.teacherService.getCourses().subscribe({
      next: (courses) => {
        this.courses = courses || [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.courses = [];
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get filteredCourses(): Course[] {
    const base = this.activeFilter === 'ALL' ? this.courses : this.courses.filter(c => c.status === this.activeFilter);
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) return base;
    return base.filter(c =>
      (c.title || '').toLowerCase().includes(q) ||
      (c.teacherName || '').toLowerCase().includes(q) ||
      (c.department || '').toLowerCase().includes(q)
    );
  }

  clearSearch(): void {
    this.searchQuery = '';
  }

  setFilter(filter: StatusFilter): void {
    this.activeFilter = filter;
  }

  get metrics() {
    const total = this.courses.length;
    const live = this.courses.filter(c => c.status === 'LIVE').length;
    const draft = this.courses.filter(c => c.status === 'DRAFT').length;
    const pending = this.courses.filter(c => c.status === 'PENDING').length;
    return [
      { label: 'Active courses', value: String(total), sub: `${live} live / ${draft} draft` },
      { label: 'Live', value: String(live), sub: 'Visible to students' },
      { label: 'Draft', value: String(draft), sub: 'Still in progress' },
      { label: 'Pending review', value: String(pending), sub: 'Admin queue' }
    ];
  }

  onDelete(courseId: number): void {
    if (!confirm('Delete this course? This cannot be undone.')) return;
    this.teacherService.deleteCourse(courseId).subscribe({
      next: () => {
        this.courses = this.courses.filter(c => c.id !== courseId);
        this.notificationService.success('Course deleted successfully.');
      },
      error: (err) => {
        console.error('Delete course error:', err);
        const msg = err?.error?.message || (typeof err?.error === 'string' ? err.error : 'Could not delete course.');
        this.notificationService.error(msg);
      }
    });
  }
}
