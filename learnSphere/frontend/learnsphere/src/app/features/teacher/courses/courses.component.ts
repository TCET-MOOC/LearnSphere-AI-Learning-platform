import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface TeacherCourse {
  code: string;
  title: string;
  status: 'Live' | 'Draft' | 'Pending' | 'Archived';
  students: number;
  rating: string;
  completion: number;
  lectures: number;
  tone: 'purple' | 'blue' | 'amber' | 'red' | 'grey';
  updated: string;
  nextAction: string;
}

interface LectureItem {
  title: string;
  status: 'Published' | 'Processing' | 'Draft';
  meta: string;
}

@Component({
  selector: 'app-teacher-courses',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent {
  tabs = ['All (7)', 'Live (2)', 'Draft (3)', 'Pending (1)', 'Archived (1)'];
  activeTab = 'All (7)';

  metrics = [
    { label: 'Active courses', value: '5', sub: '2 live / 3 draft', tone: 'purple' },
    { label: 'Total students', value: '486', sub: '+12 this week', tone: 'green' },
    { label: 'Avg. rating', value: '4.7', sub: 'Top 10% platform', tone: 'amber' },
    { label: 'Pending review', value: '1', sub: 'Admin queue', tone: 'red' }
  ];

  courses: TeacherCourse[] = [
    {
      code: 'MA',
      title: 'Engineering Mathematics III',
      status: 'Live',
      students: 312,
      rating: '4.9',
      completion: 64,
      lectures: 20,
      tone: 'purple',
      updated: 'Lecture 9 uploaded today',
      nextAction: 'Add lecture'
    },
    {
      code: 'DM',
      title: 'Discrete Mathematics',
      status: 'Live',
      students: 174,
      rating: '4.5',
      completion: 58,
      lectures: 14,
      tone: 'blue',
      updated: 'Quiz results reviewed',
      nextAction: 'Message class'
    },
    {
      code: 'ST',
      title: 'Statistics for Engineers',
      status: 'Draft',
      students: 0,
      rating: '-',
      completion: 40,
      lectures: 8,
      tone: 'amber',
      updated: 'Needs 2 more lectures',
      nextAction: 'Continue editing'
    },
    {
      code: 'NM',
      title: 'Numerical Methods',
      status: 'Pending',
      students: 0,
      rating: '-',
      completion: 100,
      lectures: 12,
      tone: 'red',
      updated: 'Submitted for admin review',
      nextAction: 'Preview'
    }
  ];

  lectures: LectureItem[] = [
    { title: 'Lec 1 - Introduction to Calculus', status: 'Published', meta: '1,840 views' },
    { title: 'Lec 8 - Laplace Transforms II', status: 'Published', meta: '942 views' },
    { title: 'Lec 9 - Fourier Series', status: 'Processing', meta: 'Uploaded today' },
    { title: 'Lec 10 - Z-Transforms', status: 'Draft', meta: 'Ready to publish' }
  ];

  get filteredCourses(): TeacherCourse[] {
    if (this.activeTab.startsWith('Live')) return this.courses.filter(c => c.status === 'Live');
    if (this.activeTab.startsWith('Draft')) return this.courses.filter(c => c.status === 'Draft');
    if (this.activeTab.startsWith('Pending')) return this.courses.filter(c => c.status === 'Pending');
    if (this.activeTab.startsWith('Archived')) return this.courses.filter(c => c.status === 'Archived');
    return this.courses;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  handleCourseAction(course: TeacherCourse, action: string): void {
    alert(`${action} for course: ${course.title}`);
  }

  getStatusClass(status: TeacherCourse['status'] | LectureItem['status']): string {
    return {
      Live: 'pill--green',
      Draft: 'pill--amber',
      Pending: 'pill--red',
      Archived: 'pill--grey',
      Published: 'pill--green',
      Processing: 'pill--amber'
    }[status] || 'pill--grey';
  }
}
