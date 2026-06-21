import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface LectureCard {
  title: string;
  course: string;
  duration: string;
  progress: number;
  status: 'Playing' | 'Completed' | 'New' | 'Saved';
  tone: 'purple' | 'green' | 'amber' | 'blue';
}

@Component({
  selector: 'app-student-lecture',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lecture.component.html',
  styleUrls: ['./lecture.component.scss']
})
export class LectureComponent {
  tabs = ['All courses', 'Engineering Math III', 'DSA', 'Thermodynamics'];
  activeTab = 'All courses';

  lectures: LectureCard[] = [
    { title: 'Laplace Transforms - Part 2', course: 'Engineering Mathematics III', duration: '24 min', progress: 35, status: 'Playing', tone: 'purple' },
    { title: 'Fourier Series Basics', course: 'Engineering Mathematics III', duration: '31 min', progress: 0, status: 'New', tone: 'purple' },
    { title: 'Graph Traversal: BFS', course: 'Data Structures & Algorithms', duration: '28 min', progress: 100, status: 'Completed', tone: 'amber' },
    { title: 'Entropy and Availability', course: 'Thermodynamics', duration: '22 min', progress: 20, status: 'Saved', tone: 'green' },
    { title: 'Process Synchronization', course: 'Operating Systems', duration: '34 min', progress: 100, status: 'Completed', tone: 'blue' },
    { title: 'Routing Algorithms', course: 'Computer Networks', duration: '26 min', progress: 0, status: 'New', tone: 'blue' }
  ];

  playlist = [
    { title: 'Lec 7 - Laplace Transform I', time: '21 min', done: true },
    { title: 'Lec 8 - Laplace Transform II', time: '24 min', done: false },
    { title: 'Lec 9 - Fourier Series', time: '31 min', done: false },
    { title: 'Lec 10 - Z-Transforms', time: '29 min', done: false }
  ];

  notes = [
    'Review convolution theorem before the quiz.',
    'Bookmark examples 3 and 4 for assignment practice.',
    'Ask about inverse transform shortcut in discussion.'
  ];

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  getStatusClass(status: LectureCard['status']): string {
    return {
      Playing: 'pill--purple',
      Completed: 'pill--green',
      New: 'pill--blue',
      Saved: 'pill--amber'
    }[status];
  }
}
