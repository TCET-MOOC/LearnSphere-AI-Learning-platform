import { Component, Input, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TeacherService } from '../../../services/teacher.service';
import { UploadService } from '../../../services/upload.service';
import { TeacherAssessmentService } from '../../../services/assessment.service';
import { Lecture } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';
import { DurationPipe } from '@shared/pipes/duration.pipe';
import { 
  LucideAngularModule, 
  HelpCircle, 
  Sparkles, 
  Plus, 
  Trash2, 
  Check, 
  X, 
  BookOpen 
} from 'lucide-angular';

/**
 * LectureListComponent manages the lectures belonging to a single course:
 * lists them in order, provides add/edit, plus Udemy/Coursera style Post-Video Quiz creation.
 */
@Component({
  selector: 'app-lecture-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DurationPipe, LucideAngularModule],
  templateUrl: './lecture-list.component.html',
  styleUrls: ['./lecture-list.component.scss']
})
export class LectureListComponent implements OnChanges {
  @Input() courseId!: number;

  lectures: Lecture[] = [];
  loading = true;

  showForm = false;
  editingLecture: Lecture | null = null;
  form: FormGroup;
  saving = false;

  uploadingVideo = false;
  uploadPercent = 0;

  // Quiz Management Modal State
  showQuizModal = false;
  quizLecture: Lecture | null = null;
  activeQuizTab: 'ai' | 'manual' = 'ai';
  loadingQuiz = false;
  existingQuiz: any = null;

  // AI Quiz Creator
  aiPrompt = '';
  aiCount = 3;
  generatingAi = false;
  aiGeneratedQuestions: any[] = [];
  savingQuiz = false;

  // Manual Question Creator
  manualQuizTitle = '';
  manualDuration = 10;
  manualQuestions: any[] = [
    { body: '', options: ['', '', '', ''], correctIndex: 0, explanation: '' }
  ];

  constructor(
    private teacherService: TeacherService,
    private uploadService: UploadService,
    private teacherAssessmentService: TeacherAssessmentService,
    private notificationService: NotificationService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      number: [1, [Validators.required, Validators.min(1)]],
      videoUrl: [''],
      duration: [0, [Validators.required, Validators.min(0)]],
      isDownloadable: [false],
      status: ['DRAFT']
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['courseId'] && this.courseId) {
      this.load();
    }
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.teacherService.getLectures(this.courseId).subscribe({
      next: (lectures) => {
        this.lectures = (lectures || []).sort((a, b) => a.number - b.number);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.lectures = [];
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openAddForm(): void {
    this.editingLecture = null;
    this.form.reset({
      title: '',
      number: this.lectures.length + 1,
      videoUrl: '',
      duration: 0,
      isDownloadable: false,
      status: 'DRAFT'
    });
    this.showForm = true;
  }

  openEditForm(lecture: Lecture): void {
    this.editingLecture = lecture;
    this.form.reset({
      title: lecture.title,
      number: lecture.number,
      videoUrl: lecture.videoUrl,
      duration: lecture.duration,
      isDownloadable: lecture.isDownloadable,
      status: lecture.status
    });
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingLecture = null;
  }

  onVideoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingVideo = true;
    this.uploadPercent = 0;
    this.uploadService.uploadVideo(file).subscribe({
      next: (evt) => {
        this.uploadPercent = evt.percent;
        if (evt.type === 'done' && evt.url) {
          this.form.patchValue({ videoUrl: evt.url });
          this.uploadingVideo = false;
          this.notificationService.success('Video uploaded.');
        }
      },
      error: () => {
        this.uploadingVideo = false;
        this.notificationService.error('Video upload failed.');
      }
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const payload: Partial<Lecture> = this.form.value;

    const request$ = this.editingLecture
      ? this.teacherService.updateLecture(this.editingLecture.id, payload)
      : this.teacherService.addLecture(this.courseId, payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.notificationService.success(this.editingLecture ? 'Lecture updated.' : 'Lecture added.');
        this.load();
      },
      error: () => {
        this.saving = false;
        this.notificationService.error('Could not save lecture.');
      }
    });
  }

  remove(lecture: Lecture): void {
    if (!confirm(`Delete "${lecture.title}"?`)) return;
    this.teacherService.deleteLecture(lecture.id).subscribe({
      next: () => {
        this.lectures = this.lectures.filter(l => l.id !== lecture.id);
        this.notificationService.success('Lecture deleted.');
      },
      error: () => this.notificationService.error('Could not delete lecture.')
    });
  }

  // --- Post-Video Quiz Management ---

  openQuizModal(lecture: Lecture): void {
    this.quizLecture = lecture;
    this.showQuizModal = true;
    this.loadingQuiz = true;
    this.aiGeneratedQuestions = [];
    this.aiPrompt = lecture.title;
    this.manualQuizTitle = `Post-Video Quiz: ${lecture.title}`;
    this.manualQuestions = [
      { body: '', options: ['', '', '', ''], correctIndex: 0, explanation: '' }
    ];

    this.teacherAssessmentService.getTestsForLecture(lecture.id).subscribe({
      next: (tests) => {
        this.existingQuiz = tests && tests.length > 0 ? tests[0] : null;
        this.loadingQuiz = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.existingQuiz = null;
        this.loadingQuiz = false;
        this.cdr.markForCheck();
      }
    });
  }

  closeQuizModal(): void {
    this.showQuizModal = false;
    this.quizLecture = null;
    this.existingQuiz = null;
    this.aiGeneratedQuestions = [];
  }

  generateAiQuestions(): void {
    if (!this.quizLecture) return;
    this.generatingAi = true;

    this.teacherAssessmentService.extractAiQuestions(this.aiPrompt || this.quizLecture.title, this.aiCount).subscribe({
      next: (res) => {
        this.aiGeneratedQuestions = res.questions || [];
        this.generatingAi = false;
        this.notificationService.success(`Generated ${this.aiGeneratedQuestions.length} questions using NVIDIA NIM.`);
        this.cdr.markForCheck();
      },
      error: () => {
        this.generatingAi = false;
        this.notificationService.error('Failed to generate questions. Please try again.');
        this.cdr.markForCheck();
      }
    });
  }

  saveAiQuiz(): void {
    if (!this.quizLecture || this.aiGeneratedQuestions.length === 0 || this.savingQuiz) return;
    this.savingQuiz = true;

    const draft = {
      course: { id: this.courseId },
      lecture: { id: this.quizLecture.id },
      title: `Quiz: ${this.quizLecture.title}`,
      durationMinutes: 10,
      isRemedial: false
    };

    this.teacherAssessmentService.createTest(draft).subscribe({
      next: (test) => {
        const questionPayloads = this.aiGeneratedQuestions.map(q => ({
          body: q.question,
          options: q.options,
          correctAnswer: q.options[q.correctIndex || 0] || q.options[0],
          marks: 1,
          questionType: 'MCQ'
        }));

        this.teacherAssessmentService.addQuestionsBulk(test.testId, questionPayloads as any).subscribe({
          next: () => {
            this.savingQuiz = false;
            this.notificationService.success('Post-video quiz saved with AI questions!');
            this.openQuizModal(this.quizLecture!);
          },
          error: () => {
            this.savingQuiz = false;
            this.notificationService.error('Failed to save quiz questions.');
          }
        });
      },
      error: () => {
        this.savingQuiz = false;
        this.notificationService.error('Could not create quiz.');
      }
    });
  }

  addManualQuestionField(): void {
    this.manualQuestions.push({
      body: '',
      options: ['', '', '', ''],
      correctIndex: 0,
      explanation: ''
    });
  }

  removeManualQuestionField(index: number): void {
    if (this.manualQuestions.length > 1) {
      this.manualQuestions.splice(index, 1);
    }
  }

  saveManualQuiz(): void {
    if (!this.quizLecture || this.savingQuiz) return;
    const validQuestions = this.manualQuestions.filter(q => q.body.trim().length > 0);
    if (validQuestions.length === 0) {
      this.notificationService.error('Please enter at least one question.');
      return;
    }

    this.savingQuiz = true;
    const draft = {
      course: { id: this.courseId },
      lecture: { id: this.quizLecture.id },
      title: this.manualQuizTitle || `Quiz: ${this.quizLecture.title}`,
      durationMinutes: this.manualDuration || 10,
      isRemedial: false
    };

    this.teacherAssessmentService.createTest(draft).subscribe({
      next: (test) => {
        const questionPayloads = validQuestions.map(q => ({
          body: q.body,
          options: q.options,
          correctAnswer: q.options[q.correctIndex || 0] || q.options[0],
          marks: 1,
          questionType: 'MCQ'
        }));

        this.teacherAssessmentService.addQuestionsBulk(test.testId, questionPayloads as any).subscribe({
          next: () => {
            this.savingQuiz = false;
            this.notificationService.success('Manual post-video quiz saved successfully!');
            this.openQuizModal(this.quizLecture!);
          },
          error: () => {
            this.savingQuiz = false;
            this.notificationService.error('Failed to save quiz questions.');
          }
        });
      },
      error: () => {
        this.savingQuiz = false;
        this.notificationService.error('Could not create quiz.');
      }
    });
  }

  deleteQuiz(quizId: number): void {
    if (!confirm('Are you sure you want to delete this lecture quiz?')) return;
    this.teacherAssessmentService.deleteTest(quizId).subscribe({
      next: () => {
        this.existingQuiz = null;
        this.notificationService.success('Quiz deleted.');
        this.cdr.markForCheck();
      },
      error: () => this.notificationService.error('Failed to delete quiz.')
    });
  }
}
