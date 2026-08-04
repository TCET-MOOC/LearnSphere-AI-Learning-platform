import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
export class CoursesComponent implements OnInit {
  loading = true;
  courses: Course[] = [];
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
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.teacherService.getCourses().subscribe({
      next: (courses) => {
        this.courses = courses;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  get filteredCourses(): Course[] {
    if (this.activeFilter === 'ALL') return this.courses;
    return this.courses.filter(c => c.status === this.activeFilter);
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
        this.notificationService.success('Course deleted.');
      },
      error: () => this.notificationService.error('Could not delete course.')
    });
  }
}
