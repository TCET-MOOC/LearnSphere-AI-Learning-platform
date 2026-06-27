import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
 
interface KpiCard {
  icon: string;
  value: string | number;
  label: string;
  trend?: 'up' | 'down';
  trendValue?: string;
}
 
interface CourseProgress {
  id: string;
  title: string;
  teacher: string;
  department: string;
  thumbnail: string;
  progress: number;
  lecturesWatched: number;
  totalLectures: number;
  lastWatched: string;
  color: string;
}
 
interface PendingAssessment {
  id: string;
  title: string;
  course: string;
  questions: number;
  timeLimit: number;
  dueDate: string;
  urgency: 'URGENT' | 'UPCOMING' | 'REMEDIAL';
}
 
interface Notification {
  id: string;
  icon: string;
  message: string;
  time: string;
  read: boolean;
}
 
interface RecommendedCourse {
  id: string;
  title: string;
  teacher: string;
  department: string;
  thumbnail: string;
  students: number;
  rating: number;
  tag: string;
}
 
@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
 
  studentName = 'Arjun Mehta';
  currentDate = new Date();
 
  kpiCards: KpiCard[] = [
    { icon: 'book-open', value: 8, label: 'Enrolled Courses', trend: 'up', trendValue: '2 this month' },
    { icon: 'calendar-check', value: '84%', label: 'Attendance Score', trend: 'down', trendValue: '3% vs last week' },
    { icon: 'trophy', value: '#12', label: 'Leaderboard Rank', trend: 'up', trendValue: '4 places up' },
    { icon: 'award', value: 3, label: 'Certificates Earned', trend: 'up', trendValue: '1 this month' },
  ];
 
  continueLearning: CourseProgress[] = [
    {
      id: 'c1',
      title: 'Data Structures & Algorithms',
      teacher: 'Dr. Priya Nair',
      department: 'Computer Science',
      thumbnail: 'CS',
      progress: 68,
      lecturesWatched: 17,
      totalLectures: 25,
      lastWatched: '2 hours ago',
      color: '#534AB7'
    },
    {
      id: 'c2',
      title: 'Engineering Mathematics III',
      teacher: 'Prof. Rakesh Sharma',
      department: 'Mathematics',
      thumbnail: 'MA',
      progress: 45,
      lecturesWatched: 9,
      totalLectures: 20,
      lastWatched: 'Yesterday',
      color: '#1D9E75'
    },
    {
      id: 'c3',
      title: 'Thermodynamics & Heat Transfer',
      teacher: 'Dr. Sunita Patil',
      department: 'Mechanical Engg.',
      thumbnail: 'ME',
      progress: 22,
      lecturesWatched: 4,
      totalLectures: 18,
      lastWatched: '3 days ago',
      color: '#BA7517'
    }
  ];
 
  pendingAssessments: PendingAssessment[] = [
    {
      id: 'a1',
      title: 'Module 4 Quiz — Sorting Algorithms',
      course: 'Data Structures & Algorithms',
      questions: 15,
      timeLimit: 20,
      dueDate: 'Today, 11:59 PM',
      urgency: 'URGENT'
    },
    {
      id: 'a2',
      title: 'Mid-semester Assessment',
      course: 'Engineering Mathematics III',
      questions: 30,
      timeLimit: 60,
      dueDate: 'Tomorrow, 5:00 PM',
      urgency: 'UPCOMING'
    },
    {
      id: 'a3',
      title: 'Remedial Certification Exam',
      course: 'Basic Electrical Engineering',
      questions: 30,
      timeLimit: 45,
      dueDate: 'Jun 25, 2026',
      urgency: 'REMEDIAL'
    }
  ];
 
  notifications: Notification[] = [
    { id: 'n1', icon: '🎬', message: 'Dr. Priya Nair uploaded Lecture 18: Dynamic Programming.', time: '1h ago', read: false },
    { id: 'n2', icon: '📊', message: 'You moved up 4 places on the college leaderboard.', time: '3h ago', read: false },
    { id: 'n3', icon: '💬', message: 'Prof. Sharma replied to your question in Maths discussion.', time: '5h ago', read: true },
    { id: 'n4', icon: '🏅', message: 'Certificate issued for Operating Systems Fundamentals.', time: 'Yesterday', read: true },
  ];
 
  recommendedCourses: RecommendedCourse[] = [
    {
      id: 'r1',
      title: 'Introduction to Machine Learning',
      teacher: 'Dr. Amit Kulkarni',
      department: 'CS / AI',
      thumbnail: 'ML',
      students: 234,
      rating: 4.8,
      tag: 'Trending'
    },
    {
      id: 'r2',
      title: 'VLSI Design Fundamentals',
      teacher: 'Prof. Meera Joshi',
      department: 'Electronics',
      thumbnail: 'VL',
      students: 156,
      rating: 4.6,
      tag: 'Your dept'
    },
    {
      id: 'r3',
      title: 'Cloud Computing & DevOps',
      teacher: 'Dr. Rahul Desai',
      department: 'Computer Science',
      thumbnail: 'CC',
      students: 312,
      rating: 4.7,
      tag: 'Popular'
    }
  ];
 
  constructor(private router: Router) {}
 
  ngOnInit(): void {}
 
  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }
 
  getUrgencyClass(urgency: string): string {
    const map: Record<string, string> = {
      URGENT: 'pill--red',
      UPCOMING: 'pill--amber',
      REMEDIAL: 'pill--purple'
    };
    return map[urgency] || 'pill--grey';
  }
 
  getUrgencyLabel(urgency: string): string {
    return urgency.charAt(0) + urgency.slice(1).toLowerCase();
  }
 
  navigateToCourse(courseId: string): void {
    this.router.navigate(['/student/courses', courseId]);
  }
 
  navigateToAssessment(assessmentId: string): void {
    this.router.navigate(['/student/assessments', assessmentId]);
  }
 
  navigateToAllCourses(): void {
    this.router.navigate(['/student/courses']);
  }
 
  navigateToAllAssessments(): void {
    this.router.navigate(['/student/assessments']);
  }
}