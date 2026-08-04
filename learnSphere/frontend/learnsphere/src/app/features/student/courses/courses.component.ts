import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { CourseCardComponent } from './components/course-card.component';
import { ExploreSectionComponent } from './components/explore-section.component';
import { NotificationService } from '@core/services/notification.service';

type StatusFilter = 'ALL' | 'LIVE' | 'DRAFT' | 'PENDING' | 'ARCHIVED';

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

  activeFilter: StatusFilter = 'ALL';
  filters: { key: StatusFilter; label: string }[] = [
    { key: 'ALL', label: 'All' },
    { key: 'LIVE', label: 'Live' },
    { key: 'DRAFT', label: 'Draft' },
    { key: 'ARCHIVED', label: 'Archived' }
  ];

  constructor(
    private studentService: StudentService,
    private router: Router,
    private notificationService: NotificationService
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
        this.loadExplore();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadExplore(): void {
    this.studentService.getExploreCourses({ status: 'LIVE' }).subscribe({
      next: (courses) => {
        const enrolledIds = new Set(this.enrolledCourses.map(c => c.id));
        this.exploreCourses = courses.filter(c => !enrolledIds.has(c.id));
      },
      error: () => {
        this.exploreCourses = [];
      }
    });
  }

  get filteredCourses(): Course[] {
    if (this.activeFilter === 'ALL') return this.enrolledCourses;
    return this.enrolledCourses.filter(c => c.status === this.activeFilter);
  }

  setFilter(filter: StatusFilter): void {
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
