import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { StudentService } from '../../services/student.service';
import { Note } from '@core/models/course.model';
import { NoteEditorComponent } from './note-editor.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { StatusPillComponent } from '@shared/components/status-pills/status-pill.component';
import { DurationPipe } from '@shared/pipes/duration.pipe';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';

/**
 * NoteCardComponent renders a single student note card.
 * Handles edit/delete triggers and clicking the timestamp seeks the lecture video.
 */
@Component({
  selector: 'app-note-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatMenuModule,
    StatusPillComponent,
    DurationPipe,
    TimeAgoPipe
  ],
  templateUrl: './note-card.component.html',
  styleUrls: ['./note-card.component.scss']
})
export class NoteCardComponent {
  /**
   * The individual Note model to display.
   */
  @Input() note!: Note;

  /**
   * Emits when this note is mutated (edited or deleted) to reload the parent list.
   */
  @Output() changed = new EventEmitter<void>();

  /**
   * Parses basic markdown (bold, italic, inline code, lists) and escapes original HTML.
   */
  get formattedContent(): string {
    if (!this.note || !this.note.content) return '';

    // Safety escape
    let content = this.note.content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Markdown conversion
    content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    content = content.replace(/\*(.*?)\*/g, '<em>$1</em>');
    content = content.replace(/`(.*?)`/g, '<code class="inline-code">$1</code>');
    content = content.replace(/^-\s+(.*)$/gm, '<span class="bullet-item">• $1</span>');

    return content;
  }

  constructor(
    private studentService: StudentService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  /**
   * Opens the note editor inside MatDialog pre-filled with the current note model data.
   */
  onEdit(): void {
    const dialogRef = this.dialog.open(NoteEditorComponent, {
      width: '500px',
      data: { note: this.note } // edit mode
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.changed.emit();
      }
    });
  }

  /**
   * Displays the confirmation dialog prior to calling the delete service.
   */
  onDelete(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '350px',
      data: {
        title: 'Delete Note',
        message: 'Are you sure you want to delete this note? This action cannot be undone.',
        confirmLabel: 'Delete',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(confirm => {
      if (confirm) {
        this.studentService.deleteNote(this.note.id).subscribe({
          next: () => {
            this.changed.emit();
          },
          error: (err: any) => console.error('Failed to delete note', err)
        });
      }
    });
  }

  /**
   * Navigates the student back to the lecture page at the bookmarked timestamp position.
   */
  navigateToTimestamp(event: Event): void {
    event.preventDefault();
    this.router.navigate(
      ['/student/courses', this.note.courseId, 'lecture', this.note.lectureId],
      { queryParams: { t: this.note.timestampSeconds } }
    );
  }
}
