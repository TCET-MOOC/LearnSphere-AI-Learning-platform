import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type CourseFilter = 'all' | 'math' | 'dsa' | 'stats';
type StatusFilter = 'all' | 'unanswered' | 'flagged' | 'answered';
type StatusTone = 'green' | 'amber' | 'red' | 'purple';

interface DiscussionPost {
  id: string;
  student: string;
  initials: string;
  avatarBg: string;
  avatarColor: string;
  courseKey: Exclude<CourseFilter, 'all'>;
  course: string;
  lecture: string;
  postedAt: string;
  likes: number;
  replies: number;
  status: Exclude<StatusFilter, 'all'>;
  statusLabel: string;
  tone: StatusTone;
  title: string;
  body: string;
  pinned: boolean;
  aiNote?: string;
  teacherReply?: string;
}

@Component({
  selector: 'app-teacher-discussion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discussion.component.html',
  styleUrls: ['./discussion.component.scss']
})
export class DiscussionComponent {
  activeCourse: CourseFilter = 'all';
  activeStatus: StatusFilter = 'all';
  selectedPostId = 'post-laplace';
  replyDraft = '';

  readonly courseFilters: Array<{ label: string; value: CourseFilter }> = [
    { label: 'All courses', value: 'all' },
    { label: 'Math III', value: 'math' },
    { label: 'DSA', value: 'dsa' },
    { label: 'Statistics', value: 'stats' }
  ];

  readonly statusFilters: Array<{ label: string; value: StatusFilter }> = [
    { label: 'All', value: 'all' },
    { label: 'Unanswered', value: 'unanswered' },
    { label: 'Flagged', value: 'flagged' },
    { label: 'Answered', value: 'answered' }
  ];

  posts: DiscussionPost[] = [
    {
      id: 'post-laplace',
      student: 'Priya Kulkarni',
      initials: 'PK',
      avatarBg: '#FAEEDA',
      avatarColor: '#633806',
      courseKey: 'math',
      course: 'Engineering Mathematics III',
      lecture: 'Lecture 8',
      postedAt: '1h ago',
      likes: 12,
      replies: 0,
      status: 'unanswered',
      statusLabel: 'Unanswered',
      tone: 'amber',
      title: 'Inverse Laplace at 22:14',
      body: 'Can someone explain the inverse Laplace step at 22:14? I am confused about the partial fractions decomposition.',
      pinned: false
    },
    {
      id: 'post-dfs',
      student: 'Raj Shah',
      initials: 'RS',
      avatarBg: '#EEEDFE',
      avatarColor: '#534AB7',
      courseKey: 'dsa',
      course: 'Data Structures and Algorithms',
      lecture: 'Lecture 12',
      postedAt: '2d ago',
      likes: 8,
      replies: 3,
      status: 'answered',
      statusLabel: 'Answered by you',
      tone: 'green',
      title: 'DFS recursion vs explicit stack',
      body: 'What is the practical difference between DFS with recursion and DFS using an explicit stack?',
      teacherReply: 'Both are equivalent in traversal order when neighbors are handled consistently. An explicit stack is safer for very large graphs.',
      pinned: true
    },
    {
      id: 'post-flagged',
      student: 'Vikram Desai',
      initials: 'VD',
      avatarBg: '#FCEBEB',
      avatarColor: '#791F1F',
      courseKey: 'math',
      course: 'Engineering Mathematics III',
      lecture: 'Lecture 5',
      postedAt: '3d ago',
      likes: 0,
      replies: 1,
      status: 'flagged',
      statusLabel: 'AI flagged',
      tone: 'red',
      title: 'Comment hidden pending review',
      body: 'Post content is hidden until moderation is complete.',
      aiNote: 'Detected offensive phrasing in a student-to-student reply.',
      pinned: false
    },
    {
      id: 'post-probability',
      student: 'Nisha Joshi',
      initials: 'NJ',
      avatarBg: '#E1F5EE',
      avatarColor: '#085041',
      courseKey: 'stats',
      course: 'Statistics for Engineers',
      lecture: 'Practice set 2',
      postedAt: '5h ago',
      likes: 5,
      replies: 1,
      status: 'unanswered',
      statusLabel: 'Unanswered',
      tone: 'amber',
      title: 'Conditional probability shortcut',
      body: 'Is there a quick way to identify when Bayes theorem should be used instead of the multiplication rule?',
      pinned: false
    }
  ];

  get visiblePosts(): DiscussionPost[] {
    return this.posts.filter((post) => {
      const courseMatches = this.activeCourse === 'all' || post.courseKey === this.activeCourse;
      const statusMatches = this.activeStatus === 'all' || post.status === this.activeStatus;
      return courseMatches && statusMatches;
    });
  }

  get activePost(): DiscussionPost | undefined {
    return this.posts.find((post) => post.id === this.selectedPostId) ?? this.visiblePosts[0];
  }

  get totalPosts(): number {
    return this.posts.length;
  }

  get unansweredPosts(): number {
    return this.posts.filter((post) => post.status === 'unanswered').length;
  }

  get flaggedPosts(): number {
    return this.posts.filter((post) => post.status === 'flagged').length;
  }

  setCourseFilter(filter: CourseFilter): void {
    this.activeCourse = filter;
    this.ensureVisibleSelection();
  }

  setStatusFilter(filter: StatusFilter): void {
    this.activeStatus = filter;
    this.ensureVisibleSelection();
  }

  selectPost(post: DiscussionPost): void {
    this.selectedPostId = post.id;
    this.replyDraft = post.teacherReply ?? '';
  }

  sendReply(): void {
    const message = this.replyDraft.trim();
    const post = this.activePost;
    if (!post || message.length === 0) {
      return;
    }

    this.updatePost(post.id, {
      teacherReply: message,
      status: 'answered',
      statusLabel: 'Answered by you',
      tone: 'green',
      replies: Math.max(post.replies, 1)
    });
  }

  markResolved(post: DiscussionPost): void {
    this.updatePost(post.id, {
      status: 'answered',
      statusLabel: 'Resolved',
      tone: 'green'
    });
    this.ensureVisibleSelection();
  }

  togglePin(post: DiscussionPost): void {
    this.updatePost(post.id, { pinned: !post.pinned });
  }

  dismissFlag(post: DiscussionPost): void {
    this.updatePost(post.id, {
      status: 'unanswered',
      statusLabel: 'Needs reply',
      tone: 'amber',
      aiNote: undefined
    });
    this.ensureVisibleSelection();
  }

  removePost(post: DiscussionPost): void {
    this.posts = this.posts.filter((item) => item.id !== post.id);
    this.ensureVisibleSelection();
  }

  trackByPost(_: number, post: DiscussionPost): string {
    return post.id;
  }

  private updatePost(id: string, patch: Partial<DiscussionPost>): void {
    this.posts = this.posts.map((post) => post.id === id ? { ...post, ...patch } : post);
  }

  private ensureVisibleSelection(): void {
    const selectedIsVisible = this.visiblePosts.some((post) => post.id === this.selectedPostId);
    if (!selectedIsVisible) {
      this.selectedPostId = this.visiblePosts[0]?.id ?? this.posts[0]?.id ?? '';
      this.replyDraft = this.activePost?.teacherReply ?? '';
    }
  }
}
