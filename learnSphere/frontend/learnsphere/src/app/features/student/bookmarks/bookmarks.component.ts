import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BookmarkService } from '../services/bookmark.service';
import { Bookmark } from '@core/models/course.model';
import { DurationPipe } from '@shared/pipes/duration.pipe';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';
import { NotificationService } from '@core/services/notification.service';

/**
 * BookmarksComponent lists the student's saved lecture timestamps and lets
 * them jump back into the lecture player or remove a bookmark.
 */
@Component({
  selector: 'app-bookmarks',
  standalone: true,
  imports: [CommonModule, RouterModule, DurationPipe, TimeAgoPipe],
  templateUrl: './bookmarks.component.html',
  styleUrls: ['./bookmarks.component.scss']
})
export class BookmarksComponent implements OnInit {
  bookmarks: Bookmark[] = [];
  loading = true;

  constructor(
    private bookmarkService: BookmarkService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.bookmarkService.getBookmarks().subscribe({
      next: (bookmarks) => {
        this.bookmarks = bookmarks;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  remove(bookmark: Bookmark): void {
    this.bookmarkService.deleteBookmark(bookmark.id).subscribe({
      next: () => {
        this.bookmarks = this.bookmarks.filter(b => b.id !== bookmark.id);
        this.notificationService.success('Bookmark removed.');
      },
      error: () => this.notificationService.error('Could not remove bookmark.')
    });
  }
}
