import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface StudentCourse {
  code: string;
  title: string;
  teacher: string;
  department: string;
  lectures: number;
  completed: number;
  progress: number;
  status: 'In progress' | 'Completed' | 'Not started';
  tone: 'purple' | 'green' | 'amber' | 'blue' | 'rose' | 'grey';
  next: string;
}

interface ExploreCourse {
  title: string;
  teacher: string;
  rating: string;
  tag: string;
  tone: 'purple' | 'green' | 'blue';
}

@Component({
  selector: 'app-student-courses',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent {
  tabs = ['All (7)', 'In progress (2)', 'Completed (3)', 'Not started (2)', 'Explore'];
  activeTab = 'All (7)';

  constructor(private router: Router) {}

  summaryCards = [
    { label: 'Average progress', value: '71%', sub: 'Across enrolled courses', tone: 'purple' },
    { label: 'Completed', value: '3', sub: 'Certificates ready', tone: 'green' },
    { label: 'In progress', value: '2', sub: 'Resume this week', tone: 'amber' },
    { label: 'Not started', value: '2', sub: 'Added recently', tone: 'grey' }
  ];

  courses: StudentCourse[] = [
    {
      code: 'MA',
      title: 'Engineering Mathematics III',
      teacher: 'Prof. Sharma',
      department: 'Mathematics',
      lectures: 20,
      completed: 8,
      progress: 40,
      status: 'In progress',
      tone: 'purple',
      next: 'Laplace Transforms - Part 2'
    },
    {
      code: 'DS',
      title: 'Data Structures & Algorithms',
      teacher: 'Prof. Mehta',
      department: 'Computer Science',
      lectures: 20,
      completed: 14,
      progress: 70,
      status: 'In progress',
      tone: 'amber',
      next: 'Graph traversal practice'
    },
    {
      code: 'TH',
      title: 'Thermodynamics',
      teacher: 'Prof. Patil',
      department: 'Mechanical',
      lectures: 15,
      completed: 3,
      progress: 20,
      status: 'In progress',
      tone: 'green',
      next: 'Entropy and irreversibility'
    },
    {
      code: 'OS',
      title: 'Operating Systems',
      teacher: 'Prof. Kulkarni',
      department: 'Computer Science',
      lectures: 18,
      completed: 18,
      progress: 100,
      status: 'Completed',
      tone: 'blue',
      next: 'Certificate available'
    },
    {
      code: 'WT',
      title: 'Web Technologies',
      teacher: 'Prof. Joshi',
      department: 'Information Tech',
      lectures: 12,
      completed: 12,
      progress: 100,
      status: 'Completed',
      tone: 'rose',
      next: 'Final project submitted'
    },
    {
      code: 'CN',
      title: 'Computer Networks',
      teacher: 'Prof. Deshpande',
      department: 'Computer Science',
      lectures: 16,
      completed: 0,
      progress: 0,
      status: 'Not started',
      tone: 'grey',
      next: 'Start with network models'
    },
    {
      code: 'DM',
      title: 'Discrete Mathematics',
      teacher: 'Prof. Sharma',
      department: 'Mathematics',
      lectures: 14,
      completed: 0,
      progress: 0,
      status: 'Not started',
      tone: 'grey',
      next: 'Sets and relations'
    }
  ];

  exploreCourses: ExploreCourse[] = [
    { title: 'Machine Learning Basics', teacher: 'Prof. Gupta', rating: '4.8', tag: 'AI pick', tone: 'purple' },
    { title: 'Fluid Mechanics', teacher: 'Prof. Nair', rating: '4.6', tag: 'Trending', tone: 'green' },
    { title: 'Robotics Intro', teacher: 'Prof. Rao', rating: '4.5', tag: 'Free', tone: 'blue' }
  ];

  get filteredCourses(): StudentCourse[] {
    if (this.activeTab.startsWith('In progress')) return this.courses.filter(c => c.status === 'In progress');
    if (this.activeTab.startsWith('Completed')) return this.courses.filter(c => c.status === 'Completed');
    if (this.activeTab.startsWith('Not started')) return this.courses.filter(c => c.status === 'Not started');
    return this.courses;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  goToCourse(courseCode: string): void {
    this.router.navigate(['/student/lectures'], { queryParams: { course: courseCode } });
  }

  getStatusClass(status: StudentCourse['status']): string {
    return {
      'In progress': 'pill--purple',
      Completed: 'pill--green',
      'Not started': 'pill--grey'
    }[status];
  }
}
