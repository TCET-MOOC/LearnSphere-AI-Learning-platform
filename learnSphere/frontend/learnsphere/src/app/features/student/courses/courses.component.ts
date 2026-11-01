import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { LucideAngularModule } from 'lucide-angular';
import { CourseCardComponent } from './components/course-card.component';
import { MY_COURSES_MOBILE_COPY } from '@core/content/my-courses-mobile.content';
import { CourseRowState, courseRowState, filterCounts } from './my-courses-mobile';
import { pickResumeCourse } from '../dashboard/home-mobile';
import { ResumeLesson, loadResumeLesson } from '../resume-lesson';

/** Phones get the approved 360 px My Courses; the Continue bar's requests are only made there. */
const PHONE_QUERY = '(max-width: 767.98px)';
/** Lets the floating AI button lift above the Continue bar (it lives in the app shell). */
const CONTINUE_BAR_BODY_CLASS = 'has-continue-bar';

type CourseFilterKey = 'ALL' | 'IN_PROGRESS' | 'COMPLETED';

/**
 * W3-10 / AUD-07 / W3-12 / AUD-09: Enrolled-only My Courses surface.
 * Shows active and completed course enrollments with progress and resume targets.
 * Catalogue browsing has moved to standalone /student/explore.
 */
@Component({
  selector: 'app-student-courses',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule, CourseCardComponent],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit, OnDestroy {
  loading = true;
  errorMessage: string | null = null;
  enrolledCourses: Course[] = [];
  searchQuery = '';
  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamSub?: Subscription;

  activeFilter: CourseFilterKey = 'ALL';
  filters: { key: CourseFilterKey; label: string }[] = [
    { key: 'ALL', label: 'All Courses' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'COMPLETED', label: 'Completed' }
  ];

  // Approved 360 px My Courses
  isPhone = false;
  continueLesson: ResumeLesson | null = null;
  readonly chipKeys: CourseFilterKey[] = ['ALL', 'IN_PROGRESS', 'COMPLETED'];

  /** Getter, not a field: in the combined unit run the content module can initialise after this one. */
  get mcCopy() {
    return MY_COURSES_MOBILE_COPY;
  }

  get chipCounts() {
    return filterCounts(this.enrolledCourses.filter((c) => this.matchesSearch(c)));
  }

  byline(course: Course): string {
    return [course.teacherName, course.department].filter(Boolean).join(', ');
  }

  rowState(course: Course): CourseRowState {
    return courseRowState(course);
  }

  constructor(
    private studentService: StudentService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.isPhone = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(PHONE_QUERY).matches;
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
    });

    this.queryParamSub = this.route.queryParamMap.subscribe((params) => {
      const q = params.get('search') || '';
      if (q !== this.searchQuery) {
        this.searchQuery = q;
        this.cdr.markForCheck();
      }
    });

    this.loadCourses();
  }

  ngOnDestroy(): void {
    document.body.classList.remove(CONTINUE_BAR_BODY_CLASS);
    this.searchSub?.unsubscribe();
    this.queryParamSub?.unsubscribe();
  }

  loadCourses(): void {
    this.loading = true;
    this.errorMessage = null;
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        this.enrolledCourses = courses || [];
        this.loading = false;
        this.cdr.markForCheck();
        if (this.isPhone) this.loadContinueLesson();
      },
      error: () => {
        this.enrolledCourses = [];
        this.loading = false;
        this.errorMessage = 'Could not load your enrolled courses. Please check connection and try again.';
        this.cdr.markForCheck();
      }
    });
  }

  /** Continue bar: the lesson to resume in the unfinished course with the most progress. */
  private loadContinueLesson(): void {
    const course = pickResumeCourse(this.enrolledCourses);
    if (!course) return;
    loadResumeLesson(this.studentService, course).subscribe((lesson) => {
      this.continueLesson = lesson;
      document.body.classList.add(CONTINUE_BAR_BODY_CLASS);
      this.cdr.markForCheck();
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
    return searched;
  }

  setFilter(filter: CourseFilterKey): void {
    this.activeFilter = filter;
  }
}
