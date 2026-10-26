import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { CourseCardComponent } from './components/course-card.component';
import { ExploreSectionComponent } from './components/explore-section.component';
import { NotificationService } from '@core/services/notification.service';

type CourseFilterKey = 'ALL' | 'IN_PROGRESS' | 'COMPLETED' | 'LIVE' | 'ARCHIVED';

@Component({
  selector: 'app-student-courses',
  standalone: true,
  imports: [CommonModule, FormsModule, CourseCardComponent, ExploreSectionComponent],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit, OnDestroy {
  loading = true;
  enrolledCourses: Course[] = [];
  exploreCourses: Course[] = [];
  enrollingId: number | null = null;
  exploring = false;
  searchQuery = '';
  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamSub?: Subscription;

  activeFilter: CourseFilterKey = 'ALL';
  filters: { key: CourseFilterKey; label: string }[] = [
    { key: 'ALL', label: 'All' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'COMPLETED', label: 'Completed' },
    { key: 'LIVE', label: 'Live' },
    { key: 'ARCHIVED', label: 'Archived' }
  ];

  constructor(
    private studentService: StudentService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((q) => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: q ? { search: q } : {},
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
      this.loadExplore();
    });
    // Honor ?search= (e.g. from the global topbar search box).
    this.queryParamSub = this.route.queryParamMap.subscribe((params) => {
      const q = params.get('search') || '';
      if (q !== this.searchQuery) {
        this.searchQuery = q;
        this.loadExplore();
      }
    });
    this.loadCourses();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.queryParamSub?.unsubscribe();
  }

  loadCourses(): void {
    this.loading = true;
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        this.enrolledCourses = courses;
        this.loading = false;
        this.cdr.markForCheck();
        this.loadExplore();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadExplore(): void {
    this.exploring = true;
    const q = this.searchQuery.trim();
    this.studentService.getExploreCourses({ status: 'LIVE', search: q || undefined }).subscribe({
      next: (courses) => {
        const enrolledIds = new Set(this.enrolledCourses.map(c => c.id));
        this.exploreCourses = courses.filter(c => !enrolledIds.has(c.id));
        this.exploring = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.exploreCourses = [];
        this.exploring = false;
        this.cdr.markForCheck();
      }
    });
  }

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchSubject.next('');
  }

  private matchesSearch(c: Course): boolean {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) return true;
    return (c.title || '').toLowerCase().includes(q) ||
      (c.teacherName || '').toLowerCase().includes(q) ||
      (c.department || '').toLowerCase().includes(q);
  }

  get filteredCourses(): Course[] {
    const searched = this.enrolledCourses.filter(c => this.matchesSearch(c));
    if (this.activeFilter === 'ALL') return searched;
    if (this.activeFilter === 'IN_PROGRESS') {
      return searched.filter(c => !c.completed && (c.progressPercent == null || c.progressPercent < 100));
    }
    if (this.activeFilter === 'COMPLETED') {
      return searched.filter(c => c.completed || (c.progressPercent != null && c.progressPercent >= 100));
    }
    return searched.filter(c => c.status === this.activeFilter);
  }

  setFilter(filter: CourseFilterKey): void {
    this.activeFilter = filter;
  }

  onEnroll(courseId: number): void {
    this.enrollingId = courseId;
    this.studentService.enrollInCourse(courseId).subscribe({
      next: () => {
        this.notificationService.success('Enrolled! The course now appears in your list.');
        this.enrollingId = null;
        this.loadCourses();
      },
      error: (err) => {
        this.enrollingId = null;
        const message = err?.error?.message || 'Could not enroll in this course.';
        this.notificationService.error(message);
        this.router.navigate(['/student/courses', courseId]);
      }
    });
  }
}
