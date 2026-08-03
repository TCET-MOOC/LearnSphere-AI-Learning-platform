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
    EmptyStateComponent
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
        const distinctCourses = new Set(data.map(n => n.courseName));
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
      width: '500px',
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
      const grouped = [...this.notes].sort((a, b) => a.courseName.localeCompare(b.courseName));
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

  private formatMarkdown(content: string): string {
    if (!content) return '';
    let escaped = content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
    escaped = escaped.replace(/`(.*?)`/g, '<code>$1</code>');
    escaped = escaped.replace(/^-\s+(.*)$/gm, '• $1');
    return escaped;
  }

  private generatePDF(notesList: Note[], docTitle: string): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      this.notificationService.error('Popup window blocked. Please disable popup blocker to export notes.');
      return;
    }

    const notesHTML = notesList.map(note => {
      const formattedContent = this.formatMarkdown(note.content);
      const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
        year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
      });
      const durationFormatted = this.formatDuration(note.timestampSeconds);
      const tagsHTML = note.tags.map(t => `<span class="tag">#${t}</span>`).join(' ');

      return `
        <div class="note-item">
          <div class="note-header">
            <h2 class="note-title">${note.title}</h2>
            <div class="note-meta">
              <span class="meta-item"><strong>Course:</strong> ${note.courseName}</span>
              <span class="meta-item"><strong>Lecture:</strong> ${note.lectureLabel} (⏱ ${durationFormatted})</span>
              <span class="meta-item"><strong>Date:</strong> ${formattedDate}</span>
            </div>
          </div>
          <div class="note-body">${formattedContent}</div>
          ${note.tags.length > 0 ? `<div class="note-tags">${tagsHTML}</div>` : ''}
        </div>
      `;
    }).join('<hr class="divider" />');

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>${docTitle}</title>
          <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
            body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              color: #1e293b;
              line-height: 1.5;
              padding: 40px;
              max-width: 800px;
              margin: 0 auto;
              background-color: #ffffff;
            }
            .header {
              border-bottom: 2px solid #534ab7;
              padding-bottom: 20px;
              margin-bottom: 35px;
              display: flex;
              justify-content: space-between;
              align-items: flex-end;
            }
            .header-title {
              font-size: 24px;
              font-weight: 700;
              color: #3c3489;
              margin: 0;
            }
            .header-subtitle {
              font-size: 14px;
              color: #64748b;
              margin: 4px 0 0 0;
            }
            .header-date {
              font-size: 12px;
              color: #64748b;
              text-align: right;
            }
            .note-item {
              margin-bottom: 30px;
              page-break-inside: avoid;
            }
            .note-title {
              font-size: 18px;
              font-weight: 600;
              color: #1e293b;
              margin: 0 0 8px 0;
            }
            .note-meta {
              font-size: 12px;
              color: #64748b;
              margin-bottom: 12px;
              display: flex;
              flex-wrap: wrap;
              gap: 15px;
            }
            .meta-item {
              display: inline-block;
            }
            .note-body {
              font-size: 14px;
              color: #334155;
              white-space: pre-wrap;
              background-color: #f8fafc;
              border-left: 3px solid #534ab7;
              padding: 12px 16px;
              border-radius: 6px;
              margin-bottom: 12px;
              border: 1px solid #e2e8f0;
              border-left-width: 3px;
            }
            .note-body strong {
              font-weight: 600;
              color: #0f172a;
            }
            .note-body code {
              background-color: rgba(83, 74, 183, 0.08);
              color: #3c3489;
              padding: 2px 5px;
              border-radius: 4px;
              font-family: monospace;
              font-size: 13px;
            }
            .note-tags {
              display: flex;
              flex-wrap: wrap;
              gap: 6px;
              margin-top: 8px;
            }
            .tag {
              display: inline-block;
              font-size: 11px;
              font-weight: 500;
              background-color: #eeedfe;
              color: #3c3489;
              padding: 2px 8px;
              border-radius: 9999px;
              border: 0.5px solid rgba(83, 74, 183, 0.15);
            }
            .divider {
              border: 0;
              border-top: 1px solid #e2e8f0;
              margin: 30px 0;
            }
            @media print {
              body {
                padding: 0;
              }
              .divider {
                border-top: 1px dashed #cbd5e1;
              }
            }
          </style>
        </head>
        <body>
          <div class="header">
            <div>
              <h1 class="header-title">LearnSphere Study Notes</h1>
              <p class="header-subtitle">${docTitle}</p>
            </div>
            <div class="header-date">
              <strong>Export Date:</strong> ${new Date().toLocaleDateString()}<br/>
              <strong>Notes Count:</strong> ${notesList.length}
            </div>
          </div>
          <div class="notes-list">
            ${notesHTML}
          </div>
          <script>
            window.onload = function() {
              window.print();
            }
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }
}
