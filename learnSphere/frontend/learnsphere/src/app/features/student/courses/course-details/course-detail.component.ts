import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student.service';
import { Course, Lecture } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.scss']
})
export class CourseDetailComponent implements OnInit {
  courseId!: number;
  course: Course | null = null;
  lectures: Lecture[] = [];
  isEnrolled = false;
  enrolling = false;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!id) {
        this.router.navigate(['/student/courses']);
        return;
      }
      this.courseId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.studentService.getPublicCourse(this.courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.loading = false;
        this.checkEnrollment();
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Course not found.');
        this.router.navigate(['/student/courses']);
      }
    });
  }

  checkEnrollment(): void {
    this.studentService.getCourseDetail(this.courseId).subscribe({
      next: () => {
        this.isEnrolled = true;
        this.loadLectures();
      },
      error: () => {
        this.isEnrolled = false;
      }
    });
  }

  loadLectures(): void {
    this.studentService.getCourseLectures(this.courseId).subscribe({
      next: (lectures) => (this.lectures = lectures),
      error: () => (this.lectures = [])
    });
  }

  get isFree(): boolean {
    return !this.course || !this.course.price || this.course.price === 0;
  }

  enroll(): void {
    if (!this.isFree) {
      this.notificationService.info('This course requires payment — checkout coming soon.');
      return;
    }
    this.enrolling = true;
    this.studentService.enrollInCourse(this.courseId).subscribe({
      next: () => {
        this.enrolling = false;
        this.notificationService.success('Enrolled! Enjoy the course.');
        this.isEnrolled = true;
        this.loadLectures();
      },
      error: (err) => {
        this.enrolling = false;
        this.notificationService.error(err?.error?.message || 'Could not enroll in this course.');
      }
    });
  }

  goToLecture(lecture: Lecture): void {
    this.router.navigate(['/student/courses', this.courseId, 'lecture', lecture.id]);
  }
}
