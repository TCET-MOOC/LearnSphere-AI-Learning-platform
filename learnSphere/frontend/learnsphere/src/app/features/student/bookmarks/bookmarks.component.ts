import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type BookmarkFilter = 'all' | 'lecture' | 'note' | 'resource';
type BookmarkTone = 'purple' | 'green' | 'amber' | 'blue';

interface BookmarkItem {
  id: string;
  type: Exclude<BookmarkFilter, 'all'>;
  typeLabel: string;
  title: string;
  course: string;
  context: string;
  savedAt: string;
  meta: string;
  actionLabel: string;
  status: string;
  tone: BookmarkTone;
  initials: string;
  color: string;
  background: string;
  progress?: number;
  note: string;
}

interface BookmarkStat {
  label: string;
  value: string;
  sub: string;
  color: string;
}

@Component({
  selector: 'app-bookmarks',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bookmarks.component.html',
  styleUrls: ['./bookmarks.component.scss']
})
export class BookmarksComponent {
  searchTerm = '';
  activeFilter: BookmarkFilter = 'all';
  selectedBookmarkId = 'bm-laplace';

  readonly filters: Array<{ label: string; value: BookmarkFilter }> = [
    { label: 'All', value: 'all' },
    { label: 'Lectures', value: 'lecture' },
    { label: 'Notes', value: 'note' },
    { label: 'Resources', value: 'resource' }
  ];

  readonly stats: BookmarkStat[] = [
    { label: 'Saved items', value: '18', sub: '6 added this week', color: '#534AB7' },
    { label: 'Lecture marks', value: '11', sub: '7 ready to resume', color: '#0F6E56' },
    { label: 'Notes saved', value: '4', sub: '2 from DSA', color: '#854F0B' },
    { label: 'Resources', value: '3', sub: 'PDFs and sheets', color: '#0C447C' }
  ];

  bookmarks: BookmarkItem[] = [
    {
      id: 'bm-laplace',
      type: 'lecture',
      typeLabel: 'Lecture',
      title: 'Laplace Transforms - Part 2',
      course: 'Engineering Mathematics III',
      context: 'Lecture 8',
      savedAt: 'Saved today',
      meta: 'Timestamp 22:14',
      actionLabel: 'Resume',
      status: 'In progress',
      tone: 'purple',
      initials: 'MA',
      color: '#534AB7',
      background: '#EEEDFE',
      progress: 64,
      note: 'Partial fractions example begins here; revisit before the Unit 2 quiz.'
    },
    {
      id: 'bm-graphs',
      type: 'lecture',
      typeLabel: 'Lecture',
      title: 'Graph Algorithms - BFS and DFS',
      course: 'Data Structures and Algorithms',
      context: 'Lecture 12',
      savedAt: 'Saved yesterday',
      meta: 'Timestamp 05:30',
      actionLabel: 'Resume',
      status: 'Watch next',
      tone: 'amber',
      initials: 'DS',
      color: '#BA7517',
      background: '#FAEEDA',
      progress: 38,
      note: 'Good comparison of queue and stack traversal patterns.'
    },
    {
      id: 'bm-bst-note',
      type: 'note',
      typeLabel: 'Note',
      title: 'BST insertion logic - my note',
      course: 'Data Structures and Algorithms',
      context: 'Personal note',
      savedAt: 'Saved yesterday',
      meta: 'Edited 18 Jun',
      actionLabel: 'Open',
      status: 'Pinned note',
      tone: 'blue',
      initials: 'NT',
      color: '#0C447C',
      background: '#E6F1FB',
      note: 'Remember to recurse left for smaller values and bubble the unchanged root upward.'
    },
    {
      id: 'bm-thermo-sheet',
      type: 'resource',
      typeLabel: 'Resource',
      title: 'Thermodynamics reference sheet',
      course: 'Thermodynamics and Heat Transfer',
      context: 'Resource PDF',
      savedAt: 'Saved 3 days ago',
      meta: '12-page PDF',
      actionLabel: 'Download',
      status: 'Offline ready',
      tone: 'green',
      initials: 'PDF',
      color: '#1D9E75',
      background: '#E1F5EE',
      note: 'Formula sheet uploaded by Prof. Patil for cycle efficiency problems.'
    },
    {
      id: 'bm-carnot',
      type: 'lecture',
      typeLabel: 'Lecture',
      title: 'Carnot Cycle explained',
      course: 'Thermodynamics and Heat Transfer',
      context: 'Lecture 3',
      savedAt: 'Saved 5 days ago',
      meta: 'Timestamp 15:00',
      actionLabel: 'Resume',
      status: 'Revision',
      tone: 'green',
      initials: 'TH',
      color: '#1D9E75',
      background: '#E1F5EE',
      progress: 82,
      note: 'Use this segment while reviewing reversible and irreversible process examples.'
    },
    {
      id: 'bm-matrices',
      type: 'resource',
      typeLabel: 'Resource',
      title: 'Eigenvalues practice set',
      course: 'Engineering Mathematics III',
      context: 'Worksheet',
      savedAt: 'Saved last week',
      meta: '20 solved problems',
      actionLabel: 'Open',
      status: 'Practice',
      tone: 'purple',
      initials: 'WS',
      color: '#534AB7',
      background: '#EEEDFE',
      note: 'Useful before attempting the remedial unlock quiz.'
    }
  ];

  get visibleBookmarks(): BookmarkItem[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.bookmarks.filter((item) => {
      const matchesFilter = this.activeFilter === 'all' || item.type === this.activeFilter;
      const matchesSearch =
        term.length === 0 ||
        item.title.toLowerCase().includes(term) ||
        item.course.toLowerCase().includes(term) ||
        item.context.toLowerCase().includes(term);

      return matchesFilter && matchesSearch;
    });
  }

  get selectedBookmark(): BookmarkItem | undefined {
    return this.bookmarks.find((item) => item.id === this.selectedBookmarkId) ?? this.visibleBookmarks[0];
  }

  setFilter(filter: BookmarkFilter): void {
    this.activeFilter = filter;
    this.ensureVisibleSelection();
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm = input.value;
    this.ensureVisibleSelection();
  }

  selectBookmark(id: string): void {
    this.selectedBookmarkId = id;
  }

  removeBookmark(id: string): void {
    this.bookmarks = this.bookmarks.filter((item) => item.id !== id);
    this.ensureVisibleSelection();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.activeFilter = 'all';
    this.ensureVisibleSelection();
  }

  trackByBookmark(_: number, item: BookmarkItem): string {
    return item.id;
  }

  private ensureVisibleSelection(): void {
    const selectedIsVisible = this.visibleBookmarks.some((item) => item.id === this.selectedBookmarkId);
    if (!selectedIsVisible) {
      this.selectedBookmarkId = this.visibleBookmarks[0]?.id ?? this.bookmarks[0]?.id ?? '';
    }
  }
}
