import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, Subscription, forkJoin } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';

type ExploreFilterKey = 'ALL' | 'FREE' | 'PAID';

/**
 * W3-10 / AUD-07: Standalone Explore Courses catalogue.
 * Provides public course discovery, search, filtering, and enrollment actions.
 */
@Component({
  selector: 'app-student-explore',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './explore.component.html',
  styleUrls: ['./explore.component.scss']
})
export class ExploreComponent implements OnInit, OnDestroy {
  loading = true;
  courses: Course[] = [];
  enrolledCourseIds = new Set<number>();
  enrollingId: number | null = null;
  searchQuery = '';
  activeFilter: ExploreFilterKey = 'ALL';

  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamSub?: Subscription;

  filters: { key: ExploreFilterKey; label: string }[] = [
    { key: 'ALL', label: 'All Catalog' },
    { key: 'FREE', label: 'Free' },
    { key: 'PAID', label: 'Premium / Paid' }
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
      this.loadCatalog();
    });

    this.queryParamSub = this.route.queryParamMap.subscribe((params) => {
      const q = params.get('search') || '';
      if (q !== this.searchQuery) {
        this.searchQuery = q;
        this.loadCatalog();
      }
    });

    this.loadCatalog();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.queryParamSub?.unsubscribe();
  }

  loadCatalog(): void {
    this.loading = true;
    const q = this.searchQuery.trim();
    forkJoin({
      explore: this.studentService.getExploreCourses({ status: 'LIVE', search: q || undefined }),
      enrolled: this.studentService.getEnrolledCourses()
    }).subscribe({
      next: ({ explore, enrolled }) => {
        this.courses = explore || [];
        this.enrolledCourseIds = new Set((enrolled || []).map(c => c.id));
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

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchSubject.next('');
  }

  setFilter(filter: ExploreFilterKey): void {
    this.activeFilter = filter;
  }

  get filteredCourses(): Course[] {
    if (this.activeFilter === 'FREE') {
      return this.courses.filter(c => !c.price || c.price === 0);
    }
    if (this.activeFilter === 'PAID') {
      return this.courses.filter(c => c.price && c.price > 0);
    }
    return this.courses;
  }

  isEnrolled(courseId: number): boolean {
    return this.enrolledCourseIds.has(courseId);
  }

  onAction(course: Course, event: Event): void {
    event.stopPropagation();
    if (this.isEnrolled(course.id)) {
      this.router.navigate(['/student/courses', course.id]);
      return;
    }

    if (course.price && course.price > 0) {
      this.router.navigate(['/student/courses', course.id]);
    } else {
      this.enrollingId = course.id;
      this.studentService.enrollInCourse(course.id).subscribe({
        next: () => {
          this.notificationService.success('Enrolled! You now have full access.');
          this.enrolledCourseIds.add(course.id);
          this.enrollingId = null;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.enrollingId = null;
          const msg = err?.error?.message || 'Could not enroll in this course.';
          this.notificationService.error(msg);
          this.router.navigate(['/student/courses', course.id]);
          this.cdr.markForCheck();
        }
      });
    }
  }
}
