import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TeacherService } from '../services/teacher.service';
import { NotificationService } from '@core/services/notification.service';
import { StudentStanding } from '@core/models/user.model';
import { StatCardComponent } from '@shared/components/stat-cards/stat-card.component';
import { ProgressBarComponent } from '@shared/components/progress-bar/progress-bar.component';
import { DataTableComponent, TableColumn } from '@shared/components/data-table/data-table.component';
import { StatusPillComponent } from '@shared/components/status-pills/status-pill.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

/**
 * StudentsComponent displays the student standings, KPI stats, top performers,
 * a full standings list using data-table, and an at-risk students nudge channel.
 */
@Component({
  selector: 'app-students',
  standalone: true,
  imports: [
    CommonModule,
    StatCardComponent,
    ProgressBarComponent,
    DataTableComponent,
    StatusPillComponent,
    TimeAgoPipe,
    EmptyStateComponent
  ],
  templateUrl: './students.component.html',
  styleUrls: ['./students.component.scss']
})
export class StudentsComponent implements OnInit {
  /**
   * Complete standings array loaded from the teacher service.
   */
  allStandings: StudentStanding[] = [];

  /**
   * Screen standings filtered by active tab selection.
   */
  filteredStandings: StudentStanding[] = [];
  
  /**
   * High rank performers subset (1 to 3).
   */
  topPerformers: StudentStanding[] = [];

  /**
   * Mid-low rank performers subset (4+).
   */
  otherStandings: StudentStanding[] = [];

  // Tab filters state variables
  courses = ['Math III', 'Discrete Math'];
  selectedTab = 'Math III';

  // KPI card metrics variables
  totalEnrolled = 0;
  avgCompletion = 0;
  atRiskCount = 0;

  /**
   * Tracking set of student IDs currently receiving nudges to handle button disable states.
   */
  nudgingIds = new Set<string>();

  /**
   * Column configuration metadata for the data-table component.
   */
  tableColumns: TableColumn[] = [
    { key: 'rank', header: 'Rank', sortable: true },
    { key: 'avatarUrl', header: 'Avatar', type: 'avatar' },
    { key: 'name', header: 'Name', sortable: true },
    { key: 'lecturesWatched', header: 'Progress', type: 'progress' },
    { key: 'scorePercent', header: 'Score', type: 'percent', sortable: true }
  ];

  constructor(
    private teacherService: TeacherService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.fetchStandings();
  }

  /**
   * Loads the standings data from TeacherService.
   */
  fetchStandings(): void {
    this.teacherService.getStudentStandings('all').subscribe({
      next: (data: any) => {
        this.allStandings = data;
        this.applyTabFilter();
      },
      error: (err: any) => console.error('Failed to fetch standings', err)
    });
  }

  /**
   * Applies sorting and filtering conditions based on active course tab or at-risk conditions.
   */
  applyTabFilter(): void {
    let data = [...this.allStandings];
    
    if (this.selectedTab === 'Math III') {
      // Math III mock scores mapping
      data = this.allStandings.map((s, idx) => ({ ...s, rank: idx + 1 }));
    } else if (this.selectedTab === 'Discrete Math') {
      // Discrete Math mock values shift
      data = this.allStandings.map((s, idx) => ({ 
        ...s, 
        rank: idx + 1,
        scorePercent: Math.max(30, s.scorePercent - 5),
        lecturesWatched: Math.max(1, s.lecturesWatched - 2)
      }));
    } else if (this.selectedTab === 'At-risk students') {
      data = this.allStandings.filter(s => s.isAtRisk);
    }
    
    this.filteredStandings = data;
    this.calculateStats();
    this.splitPerformers();
  }

  /**
   * Computes dashboard header KPI counts.
   */
  calculateStats(): void {
    this.totalEnrolled = this.filteredStandings.length;
    if (this.totalEnrolled > 0) {
      const totalProgress = this.filteredStandings.reduce((acc, curr) => acc + (curr.lecturesWatched / curr.totalLectures) * 100, 0);
      this.avgCompletion = Math.round(totalProgress / this.totalEnrolled);
    } else {
      this.avgCompletion = 0;
    }
    this.atRiskCount = this.filteredStandings.filter(s => s.isAtRisk).length;
  }

  /**
   * Separates the high performers cards from the lower rank table list rows.
   */
  splitPerformers(): void {
    this.topPerformers = this.filteredStandings.filter(s => s.rank >= 1 && s.rank <= 3);
    this.otherStandings = this.filteredStandings.filter(s => s.rank > 3);
  }

  /**
   * Switches the active standings tab selection.
   */
  selectTab(tab: string): void {
    this.selectedTab = tab;
    this.applyTabFilter();
  }

  /**
   * Sends a warning nudge notification. Prevents double clicks using the nudgingIds Set.
   */
  sendNudge(studentId: string): void {
    this.nudgingIds.add(studentId);
    const courseId = this.selectedTab === 'Discrete Math' ? 'discrete-math' : 'math-iii';
    
    this.teacherService.sendNudge(studentId, courseId).subscribe({
      next: (res: any) => {
        this.nudgingIds.delete(studentId);
        this.notificationService.success(res.message);
      },
      error: (err: any) => {
        this.nudgingIds.delete(studentId);
        this.notificationService.error('Failed to send nudge notification.');
      }
    });
  }

  /**
   * Helper check determining if a nudge API request is currently in flight.
   */
  isNudging(studentId: string): boolean {
    return this.nudgingIds.has(studentId);
  }

  /**
   * Triggers client-side CSV text file generation and browser download download.
   */
  exportList(event: Event): void {
    event.preventDefault();
    const headers = 'Rank,Name,Score,Lectures Watched,Total Lectures,At Risk\n';
    const rows = this.filteredStandings
      .map(s => `${s.rank},"${s.name}",${s.scorePercent}%,${s.lecturesWatched},${s.totalLectures},${s.isAtRisk ? 'Yes' : 'No'}`)
      .join('\n');
      
    const blob = new Blob([headers + rows], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `student-standings-${this.selectedTab.replace(' ', '-').toLowerCase()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
