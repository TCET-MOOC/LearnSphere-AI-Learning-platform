import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { CourseCardComponent } from './components/course-card.component';
import { ExploreSectionComponent } from './components/explore-section.component';
import { NotificationService } from '@core/services/notification.service';

type CourseFilterKey = 'ALL' | 'IN_PROGRESS' | 'COMPLETED' | 'LIVE' | 'ARCHIVED';

@Component({
  selector: 'app-student-courses',
  standalone: true,
  imports: [CommonModule, CourseCardComponent, ExploreSectionComponent],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent implements OnInit {
  loading = true;
  enrolledCourses: Course[] = [];
  exploreCourses: Course[] = [];
  enrollingId: number | null = null;

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
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCourses();
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
    this.studentService.getExploreCourses({ status: 'LIVE' }).subscribe({
      next: (courses) => {
        const enrolledIds = new Set(this.enrolledCourses.map(c => c.id));
        this.exploreCourses = courses.filter(c => !enrolledIds.has(c.id));
        this.cdr.markForCheck();
      },
      error: () => {
        this.exploreCourses = [];
        this.cdr.markForCheck();
      }
    });
  }

  get filteredCourses(): Course[] {
    if (this.activeFilter === 'ALL') return this.enrolledCourses;
    if (this.activeFilter === 'IN_PROGRESS') {
      return this.enrolledCourses.filter(c => !c.completed && (c.progressPercent == null || c.progressPercent < 100));
    }
    if (this.activeFilter === 'COMPLETED') {
      return this.enrolledCourses.filter(c => c.completed || (c.progressPercent != null && c.progressPercent >= 100));
    }
    return this.enrolledCourses.filter(c => c.status === this.activeFilter);
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
