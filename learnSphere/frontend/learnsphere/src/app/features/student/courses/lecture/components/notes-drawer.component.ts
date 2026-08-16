import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { StudentService } from '../../../services/student.service';
import { Note } from '@core/models/course.model';
import { NoteCardComponent } from '../../../notes/components/note-card.component';
import { NoteEditorComponent } from '../../../notes/components/note-editor.component';

/**
 * NotesDrawerComponent is a side panel on the lecture player page showing the
 * student's notes for the current lecture, with a quick "add note at current
 * timestamp" action that reuses the shared NoteEditorComponent dialog.
 */
@Component({
  selector: 'app-notes-drawer',
  standalone: true,
  imports: [CommonModule, MatDialogModule, NoteCardComponent],
  template: `
    <section class="card">
      <div class="card-head">
        <h2>Lecture notes</h2>
        <button class="link-btn" type="button" (click)="openNewNote()">+ Add note at {{ formattedTimestamp }}</button>
      </div>

      <p class="hint" *ngIf="loading">Loading notes…</p>
      <div class="notes-list" *ngIf="!loading">
        <app-note-card *ngFor="let note of notes" [note]="note" (changed)="reload()"></app-note-card>
        <p class="hint" *ngIf="notes.length === 0">No notes for this lecture yet.</p>
      </div>
    </section>
  `,
  styles: [`
    .card { background: #fff; border: 1px solid #e8e7ef; border-radius: 12px; padding: 16px; }
    .card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
    .card-head h2 { margin: 0; font-size: 14px; }
    .link-btn { border: none; background: transparent; color: #534ab7; font: 600 11px Inter, Arial; cursor: pointer; }
    .notes-list { display: flex; flex-direction: column; gap: 10px; }
    .hint { font-size: 12px; color: #6b6880; }
  `]
})
export class NotesDrawerComponent implements OnChanges {
  @Input() courseId?: number;
  @Input() courseName = '';
  @Input() lectureId?: number;
  @Input() lectureLabel = '';
  @Input() timestampSeconds = 0;

  notes: Note[] = [];
  loading = false;

  constructor(private studentService: StudentService, private dialog: MatDialog) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['lectureId'] || changes['courseId']) {
      this.reload();
    }
  }

  get formattedTimestamp(): string {
    const mins = Math.floor(this.timestampSeconds / 60);
    const secs = Math.floor(this.timestampSeconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  reload(): void {
    if (!this.courseId) {
      this.notes = [];
      return;
    }
    this.loading = true;
    this.studentService.getNotes(this.courseId).subscribe({
      next: (notes) => {
        this.notes = this.lectureId ? notes.filter(n => n.lectureId === this.lectureId) : notes;
        this.loading = false;
      },
      error: () => {
        this.notes = [];
        this.loading = false;
      }
    });
  }

  openNewNote(): void {
    const dialogRef = this.dialog.open(NoteEditorComponent, {
      width: '500px',
      data: {
        note: null,
        courseId: this.courseId,
        courseName: this.courseName,
        lectureId: this.lectureId,
        lectureLabel: this.lectureLabel,
        timestampSeconds: Math.floor(this.timestampSeconds)
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.reload();
      }
    });
  }
}
