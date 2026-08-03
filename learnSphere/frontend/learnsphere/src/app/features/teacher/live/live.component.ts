import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-live',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './live.component.html',
  styleUrls: ['./live.component.scss']
})
export class LiveComponent {
  scheduleSession() {
    alert('Session scheduled and students notified!');
  }

  saveDraft() {
    alert('Draft saved.');
  }

  startSession() {
    alert('Starting live session...');
  }
}
