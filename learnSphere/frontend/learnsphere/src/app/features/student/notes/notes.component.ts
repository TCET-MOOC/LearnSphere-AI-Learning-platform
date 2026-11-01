import { buildNotesExportHtml } from './notes-export';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { StudentService } from '../services/student.service';
import { Note } from '@core/models/course.model';
import { NoteCardComponent } from './components/note-card.component';
import { NoteEditorComponent } from './components/note-editor.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { NotificationService } from '@core/services/notification.service';
import { LucideAngularModule, Search, Plus } from 'lucide-angular';

/**
 * NotesComponent is the parent page component for managing student notes.
 * It fetches notes on load, provides client-side filters (by course tabs and text search),
 * handles new note creation via a MatDialog modal, and binds the bottom export triggers.
 */
@Component({
  selector: 'app-notes',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    NoteCardComponent,
    EmptyStateComponent,
    LucideAngularModule
  ],
  templateUrl: './notes.component.html',
  styleUrls: ['./notes.component.scss']
})
export class NotesComponent implements OnInit {
  /**
   * Complete array of loaded notes.
   */
  notes: Note[] = [];

  /**
   * Filtered note subset representing active search/tab conditions.
   */
  filteredNotes: Note[] = [];

  /**
   * Dynamic list of courses parsed from the note data.
   */
  courses: string[] = [];

  /**
   * Currently active selected tab filter.
   */
  selectedCourse: string = 'all';

  /**
   * Active text search filter query.
   */
  searchQuery: string = '';

  /**
   * Boolean state indicating if network request is pending.
   */
  loading: boolean = true;

  constructor(
    private studentService: StudentService,
    private dialog: MatDialog,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadNotes();
  }

  /**
   * Fetches the notes list from StudentService.
   */
  loadNotes(): void {
    this.loading = true;
    this.studentService.getNotes().subscribe({
      next: (data: Note[]) => {
        this.notes = data;
        
        // Extract distinct course names dynamically from the loaded notes data
        const distinctCourses = new Set(data.map(n => n.courseName).filter((c): c is string => !!c));
        this.courses = Array.from(distinctCourses);
        
        this.applyFilters();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Failed to load notes', err);
        this.loading = false;
      }
    });
  }

  /**
   * Applies client-side tab selection and title/content search filtering.
   */
  applyFilters(): void {
    this.filteredNotes = this.notes.filter(note => {
      // Filter by dynamic courseName tab selection
      const matchesCourse = this.selectedCourse === 'all' || note.courseName === this.selectedCourse;
      
      // Filter by text query matches in title or note content
      const query = this.searchQuery.toLowerCase().trim();
      const matchesSearch = !query || 
        note.title.toLowerCase().includes(query) || 
        note.content.toLowerCase().includes(query);
        
      return matchesCourse && matchesSearch;
    });
  }

  /**
   * Switches the active course tab selection.
   */
  selectTab(course: string): void {
    this.selectedCourse = course;
    this.applyFilters();
  }

  /**
   * Triggers re-filtering when search query changes.
   */
  onSearchChange(): void {
    this.applyFilters();
  }

  /**
   * Opens the NoteEditorComponent inside a MatDialog modal with no pre-filled data.
   */
  openNewNoteDialog(): void {
    const dialogRef = this.dialog.open(NoteEditorComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: { note: null } // opened in creation mode
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadNotes(); // reload list on success
      }
    });
  }

  /**
   * Refreshes the notes list after a note is edited or deleted in a child card component.
   */
  onNotesChanged(): void {
    this.loadNotes();
  }

  /**
   * Action methods for client-side exporting of notes.
   */
  exportAll(): void {
    if (!this.notes || this.notes.length === 0) {
      this.notificationService.error('No notes available to export.');
      return;
    }
    this.generatePDF(this.notes, 'All Study Notes');
  }

  exportByCourse(): void {
    if (!this.notes || this.notes.length === 0) {
      this.notificationService.error('No notes available to export.');
      return;
    }
    
    if (this.selectedCourse === 'all') {
      // Group and export all notes sorted by course name
      const grouped = [...this.notes].sort((a, b) => (a.courseName || '').localeCompare(b.courseName || ''));
      this.generatePDF(grouped, 'Study Notes Grouped by Course');
    } else {
      // Export only selected course
      const courseNotes = this.notes.filter(n => n.courseName === this.selectedCourse);
      if (courseNotes.length === 0) {
        this.notificationService.info(`No notes found for course: ${this.selectedCourse}`);
        return;
      }
      this.generatePDF(courseNotes, `Study Notes - ${this.selectedCourse}`);
    }
  }

  copyToClipboard(): void {
    const notesToCopy = this.filteredNotes;
    if (!notesToCopy || notesToCopy.length === 0) {
      this.notificationService.error('No notes in the current view to copy.');
      return;
    }

    const textContent = notesToCopy
      .map(n => `[${n.courseName} - ${n.lectureLabel} at ${this.formatDuration(n.timestampSeconds)}]\nTitle: ${n.title}\nContent: ${n.content}\nTags: ${n.tags.map(t => '#' + t).join(', ') || 'None'}`)
      .join('\n\n' + '='.repeat(40) + '\n\n');

    if (navigator.clipboard) {
      navigator.clipboard.writeText(textContent).then(() => {
        this.notificationService.success('Notes copied to clipboard!');
      }).catch(err => {
        console.error('Clipboard copy failed:', err);
        this.notificationService.error('Failed to copy notes to clipboard.');
      });
    } else {
      this.notificationService.error('Clipboard copy is not supported in this browser.');
    }
  }

  private formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  private generatePDF(notesList: Note[], docTitle: string): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      this.notificationService.error('Popup window blocked. Please disable popup blocker to export notes.');
      return;
    }

    printWindow.document.write(buildNotesExportHtml(notesList, docTitle));
    printWindow.document.close();
  }
}
