import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-flagged',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './flagged.component.html',
  styleUrls: ['./flagged.component.scss']
})
export class FlaggedComponent {

  activeTab = 'all';

  tabs = [
    { id: 'all',         label: 'All (14)' },
    { id: 'high-risk',   label: 'High risk (4)' },
    { id: 'bullying',    label: 'Bullying (3)' },
    { id: 'spam',        label: 'Spam (5)' },
    { id: 'suspicious',  label: 'Suspicious (2)' },
  ];

  setTab(id: string) {
    this.activeTab = id;
  }
}
