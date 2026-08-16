import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TeacherService, TrendingCourse } from '../services/teacher.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-trending',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './trending.component.html',
  styleUrls: ['./trending.component.scss']
})
export class TrendingComponent implements OnInit {
  trendingCourses: TrendingCourse[] = [];
  loading = true;

  constructor(
    private teacherService: TeacherService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.teacherService.getTrendingCourses(10).subscribe({
      next: (courses) => {
        this.trendingCourses = courses;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Failed to load trending courses.');
      }
    });
  }

  isMine(course: TrendingCourse): boolean {
    return course.teacherId === this.authService.currentUser?.id;
  }

  /** Best rank among the current teacher's own courses in this trending list, or null if none appear. */
  get myBestRank(): TrendingCourse | null {
    const mine = this.trendingCourses.filter((c) => this.isMine(c));
    if (mine.length === 0) {
      return null;
    }
    return mine.reduce((best, c) => (c.rank < best.rank ? c : best), mine[0]);
  }
}
