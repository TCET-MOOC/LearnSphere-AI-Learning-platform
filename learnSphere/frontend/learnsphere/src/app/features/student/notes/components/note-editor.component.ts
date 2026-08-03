import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { StudentService } from '../../services/student.service';
import { Note } from '@core/models/course.model';

/**
 * NoteEditorComponent is a reactive form modal displayed inside MatDialog.
 * Used to create a new note or edit an existing one. Supports chip input of tags.
 */
@Component({
  selector: 'app-note-editor',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule
  ],
  templateUrl: './note-editor.component.html',
  styleUrls: ['./note-editor.component.scss']
})
export class NoteEditorComponent implements OnInit {
  /**
   * Reactive form tracking title and content validation.
   */
  noteForm!: FormGroup;

  /**
   * Mode toggle: true for update note, false for create note.
   */
  isEditMode: boolean = false;

  /**
   * Local state of tag chips.
   */
  tags: string[] = [];

  /**
   * Two-way bound tag input placeholder buffer.
   */
  currentTag: string = '';

  // Default mock course/lecture metadata for new notes opened from "My Notes" main page
  courseName: string = 'Engineering Mathematics III';
  courseId: string = 'math-iii';
  lectureId: string = 'lec-8';
  lectureLabel: string = 'Lec 8';
  timestampSeconds: number = 1334; // Default playback timestamp (e.g. ⏱ 22:14)

  constructor(
    private fb: FormBuilder,
    private studentService: StudentService,
    public dialogRef: MatDialogRef<NoteEditorComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { note?: Note }
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.data && !!this.data.note;
    
    if (this.isEditMode && this.data.note) {
      const note = this.data.note;
      this.courseName = note.courseName;
      this.courseId = note.courseId;
      this.lectureId = note.lectureId;
      this.lectureLabel = note.lectureLabel;
      this.timestampSeconds = note.timestampSeconds;
      this.tags = [...note.tags];
      
      this.noteForm = this.fb.group({
        title: [note.title, [Validators.required, Validators.maxLength(100)]],
        content: [note.content, [Validators.required]]
      });
    } else {
      this.noteForm = this.fb.group({
        title: ['', [Validators.required, Validators.maxLength(100)]],
        content: ['', [Validators.required]]
      });
    }
  }

  /**
   * Appends the tag input string as a chip when pressing Enter.
   */
  addTag(event: Event): void {
    event.preventDefault(); // prevent triggering form submission
    const tag = this.currentTag.trim().replace(/^#/, ''); // Strip leading hashes if typed
    if (tag && !this.tags.includes(tag)) {
      this.tags.push(tag);
      this.currentTag = '';
    }
  }

  /**
   * Removes a tag chip from the local list.
   */
  removeTag(tagToRemove: string): void {
    this.tags = this.tags.filter(t => t !== tagToRemove);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  /**
   * Inserts markdown formatting tags around selected content in the textarea.
   */
  insertFormatting(format: string): void {
    const textarea = document.getElementById('note-content') as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value || '';
    const selectedText = text.substring(start, end);
    let replacement = '';

    switch (format) {
      case 'bold':
        replacement = `**${selectedText || 'bold text'}**`;
        break;
      case 'italic':
        replacement = `*${selectedText || 'italic text'}*`;
        break;
      case 'code':
        replacement = `\`${selectedText || 'code'}\``;
        break;
      case 'list':
        replacement = `\n- ${selectedText || 'list item'}`;
        break;
    }

    const newValue = text.substring(0, start) + replacement + text.substring(end);
    this.noteForm.patchValue({ content: newValue });
    this.noteForm.get('content')?.markAsDirty();
    this.noteForm.get('content')?.markAsTouched();

    // Keep focus and update selection range
    setTimeout(() => {
      textarea.focus();
      const newCursorPos = start + replacement.length;
      textarea.setSelectionRange(newCursorPos, newCursorPos);
    });
  }

  /**
   * Dispatches create or update calls depending on the active editMode state.
   */
  onSubmit(): void {
    if (this.noteForm.invalid) return;

    const formValues = this.noteForm.value;
    const notePayload: Partial<Note> = {
      title: formValues.title,
      content: formValues.content,
      tags: this.tags,
      courseId: this.courseId,
      courseName: this.courseName,
      lectureId: this.lectureId,
      lectureLabel: this.lectureLabel,
      timestampSeconds: this.timestampSeconds
    };

    if (this.isEditMode && this.data.note) {
      this.studentService.updateNote(this.data.note.id, notePayload).subscribe({
        next: (res: any) => this.dialogRef.close(res),
        error: (err: any) => console.error('Failed to update note', err)
      });
    } else {
      this.studentService.createNote(notePayload).subscribe({
        next: (res: any) => this.dialogRef.close(res),
        error: (err: any) => console.error('Failed to create note', err)
      });
    }
  }
}
