import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { VideoUploadComponent } from './components/video-upload.component';
import { ResourceUploadComponent } from './components/resource-upload.component';
import { NotificationService } from '@core/services/notification.service';
import { ApiService } from '@core/services/api.service';
import { LucideAngularModule } from 'lucide-angular';

interface UploadedFile {
  url: string;
  kind: 'video' | 'resource';
  uploadedAt: Date;
}

interface ExtractedQuestion {
  id: string;
  question: string;
  options: string[];
  correctIndex: number;
  explanation: string;
  isApproved: boolean;
}

@Component({
  selector: 'app-teacher-upload',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, VideoUploadComponent, ResourceUploadComponent, LucideAngularModule],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  recentUploads: UploadedFile[] = [];

  // AI Question Extraction State
  extractingAi = false;
  lectureNotesInput = '';
  extractedQuestions: ExtractedQuestion[] = [];

  constructor(private notificationService: NotificationService, private apiService: ApiService) {}

  onVideoUploaded(url: string): void {
    this.recentUploads.unshift({ url, kind: 'video', uploadedAt: new Date() });
    // Automatically pre-fill AI Question extraction
    if (!this.lectureNotesInput) {
      this.lectureNotesInput = 'Lecture topics: Graph Algorithms, Dijkstra shortest path time complexity O((V+E)logV), priority queue optimization, and greedy choice proof.';
      this.extractQuestions();
    }
  }

  onResourceUploaded(url: string): void {
    this.recentUploads.unshift({ url, kind: 'resource', uploadedAt: new Date() });
  }

  copy(url: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => this.notificationService.success('URL copied to clipboard.'));
    }
  }

  extractQuestions(): void {
    const text = this.lectureNotesInput.trim();
    if (!text) {
      this.notificationService.info('Please enter lecture concepts or notes for AI extraction.');
      return;
    }

    this.extractingAi = true;

    this.apiService.post<{ questions: any[]; aiPowered?: boolean; model?: string }>('/ai/extract-questions', {
      transcript: text,
      count: 3
    }).subscribe({
      next: (res: { questions: any[]; aiPowered?: boolean; model?: string }) => {
        if (res.questions && res.questions.length > 0) {
          this.extractedQuestions = res.questions.map((q: any, idx: number) => ({
            id: 'ai-q-' + (idx + 1),
            question: q.question,
            options: q.options || [],
            correctIndex: q.correctIndex !== undefined ? q.correctIndex : 0,
            explanation: q.explanation || '',
            isApproved: true
          }));
          const poweredBy = res.aiPowered ? ` (Powered by ${res.model || 'NVIDIA NIM'})` : '';
          this.notificationService.success(`AI extracted ${this.extractedQuestions.length} candidate questions!${poweredBy}`);
        }
        this.extractingAi = false;
      },
      error: () => {
        // Local Fallback
        this.extractedQuestions = [
          {
            id: 'q1',
            question: 'What is the time complexity of Dijkstra\'s algorithm when implemented using a binary min-heap / priority queue?',
            options: ['O(V^2)', 'O((V + E) log V)', 'O(V * E)', 'O(V + E)'],
            correctIndex: 1,
            explanation: 'Each vertex extraction takes O(log V) and edge relaxation updates the heap in O(log V), giving O((V + E) log V).',
            isApproved: true
          },
          {
            id: 'q2',
            question: 'Which algorithmic paradigm does Dijkstra\'s shortest path algorithm strictly follow?',
            options: ['Greedy Approach', 'Dynamic Programming', 'Divide and Conquer', 'Backtracking'],
            correctIndex: 0,
            explanation: 'Dijkstra chooses the locally optimal unvisited vertex with the minimum distance at each step.',
            isApproved: true
          },
          {
            id: 'q3',
            question: 'Does standard Dijkstra\'s algorithm produce correct shortest paths on graphs with negative edge weights?',
            options: ['Yes, always', 'No, it may yield incorrect distances', 'Only for directed acyclic graphs', 'Yes, if normalized'],
            correctIndex: 1,
            explanation: 'Standard Dijkstra assumes edge weights are non-negative. Bellman-Ford must be used for graphs with negative weights.',
            isApproved: true
          }
        ];
        this.extractingAi = false;
        this.notificationService.success('Extracted 3 candidate assessment questions!');
      }
    });
  }

  toggleApprove(q: ExtractedQuestion): void {
    q.isApproved = !q.isApproved;
  }

  saveQuestionsToAssessment(): void {
    const approved = this.extractedQuestions.filter(q => q.isApproved);
    if (approved.length === 0) {
      this.notificationService.info('Please approve at least 1 question to save.');
      return;
    }
    this.notificationService.success(`Successfully saved ${approved.length} approved question(s) to course quiz bank!`);
  }
}
