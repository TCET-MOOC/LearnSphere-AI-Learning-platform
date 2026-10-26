# E — Analysis: engagement, community & messaging

**Project:** LearnSphere AI
**Phase:** UNDERSTAND. This file feeds the development plan, so downstream agents can rely on it without re-reading the sources.
**Scope of this file:** `spec §3.3`, `spec §3.6`, `spec §5` rows 17–19 + preserved row, `spec §6` (WebSocket), `spec §9` · `AG §4` (preserved features), `AG §8–10` · `REF p17–22` · backend `Entity/{Feedback,DiscussionThread,Post,DiscussionPost,Note,Bookmark,Announcement,Notification,Conversation,Message,LiveSession,FlaggedContent}`, `Service/impl/{DiscussionThread,Discussion,Feedback,Note,Bookmark,Announcement,Notification,Messaging,LiveSession,Moderation,Sentiment,ProfanityFilter}ServiceImpl`, `Controller/{DiscussionThread,Discussion,Feedback,Note,Bookmark,Announcement,Notification,Messaging,LiveSession,Moderation,Sentiment,Censor,LiveChatWebSocket,Student}Controller`, `Config/{SecurityConfig,WebSocketConfig}`, `Security/WebSocketAuthInterceptor` · Angular `core/services/{discussion,notification-api,websocket}.service.ts`, `features/{student,teacher,admin}/{discussion,messages,announcements,notes,bookmarks,live,flagged,sentiment}/**`
**Date of analysis:** 2026-10-22

---

## 0. Conventions, sources and coverage

### 0.1 Citation keys

| Key | Source |
|---|---|
| `spec §x` | `Docs/source/spec.md` |
| `REF p<n>` | `Docs/LMS_Entity_Reference.pdf` page n |
| `AG §n` | root `AGENTS.md` section n |
| `SYS §6` / `ROAD` | `Docs/learnsphere_system_design.md` §6 · `Docs/Future_Plans.md` |
| `code <path>:<line>` | implementation evidence |
| `derived` | analyst inference, not stated by a source |

### 0.2 Source coverage

| Source | Coverage |
|---|---|
| `REF p17–22` | Read in full: Leaderboard/AssignmentSubmission, Feedback, DiscussionThread, Post. |
| `AG §8–10` | Read in full, incl. "cascade-deletes or soft-deletes its posts", "replies sorted by `createdAt ASC` and nested under `parentPost`", and the one-feedback-per-course rule. |
| `AG §4` | Read in full (preserved notes/bookmarks/live sessions). |
| `spec §3.3, §3.6, §5, §6, §9` | Read in full. |
| Backend | All twelve entities; all engagement services (full); all engagement controllers (full); `SecurityConfig`; `WebSocketConfig`; `WebSocketAuthInterceptor`; `ConversationRepository`. |
| Frontend | `discussion.service.ts`, `notification-api.service.ts`, `websocket.service.ts`; route table; student discussion, live-room, notes, bookmarks; admin flagged/sentiment. |
| Does NOT exist | course-scoped announcements, notification event fan-out, STOMP SUBSCRIBE authorization, thread pagination, user report button, live attendance, recording, message retention policy, review eligibility check. |

---

## 1. The reference contract

1. **DiscussionThread** — `thread_id`, `course_id`, `user_id`, `title` not null, `content` not null, `created_at`, `updated_at` (`REF p21`; `AG §10`).
2. **Post** — `post_id`, `thread_id` not null, `user_id` not null, `body` not null, nullable `parent_post_id` self-reference, `created_at`. Thread deletion cascades or soft-deletes its posts; replies are ordered `createdAt ASC` and nested under `parentPost` (`REF p22`; `AG §10`).
3. **Feedback** — `feedback_id`, `user_id` not null, `course_id` not null, `rating` `Short` 1–5, optional `message`, `created_at`; unique `(user_id, course_id)`; average via repository query (`REF p19`; `AG §9`).
4. **Preserved engagement entities** — `note`, `bookmark`, `live_session`, `announcement`, `conversation`, `message`, `notification`, `flagged_content` may not be deleted; notes and bookmarks attach to `content_id` (`CourseContent`), live sessions attach to `course_id` + instructor (`AG §4`; `spec §5` preserved row). None of these appear in `REF` (which ends at Section 10, p22), so `AG §4` + `spec` are the only schema authority. Endpoint contracts for threads, posts, feedback and submissions are fixed in `AG §8–10`.

---

## 2. There are two community substrates

5. The reference model is implemented: `DiscussionThread` (`discussion_thread`), `Post` (`post`, self-referencing `parent_post_id`), `DiscussionThreadController` under `/api/courses/{id}/threads`, `/api/threads/{id}`, `/api/threads/{id}/posts`, `/api/posts/{id}` (`Entity/DiscussionThread.java`, `Entity/Post.java`, `Controller/DiscussionThreadController.java`).
6. A second, older model also exists: `DiscussionPost` (`discussion_posts`) scoped to a `course` **or** `lecture`, with `parentPost`, `flagged`, and `author` (`Entity/DiscussionPost.java`). It is served by `DiscussionController` under `/api/courses/{id}/discussion`, `/api/lectures/{id}/discussion`, `/api/discussion` (`Controller/DiscussionController.java`).
7. **The UI uses the legacy model.** `DiscussionService` exposes both, but only `getCourseDiscussion`, `getLectureDiscussion`, `createPost`, `deletePost` are called by components; the thread methods (`getThreads`, `createThread`, `getThreadPosts`, `postReply`) have **zero call sites** outside the service (`core/services/discussion.service.ts:31–57`; repo-wide search). Student and teacher discussion pages both render `DiscussionPostDto` from `getCourseDiscussion` (`features/student/discussion/discussion.component.ts:64–72`).
8. **derived:** the Section 10 backend is largely built but not delivered. The user-facing forum is the older one-level model, so the reference's thread/reply contract has no UI acceptance path. This is a "dark API" plus a parallel-model drift, not a missing feature.

---

## 3. Threads and posts: correct ownership, weak tenancy

9. Update/delete are guarded: `updateThread` / `deleteThread` / `updatePost` / `deletePost` allow only the author or an `ADMIN` (`Service/impl/DiscussionThreadServiceImpl.java:74–125`). This satisfies AC-22 for the two-tier surface.
10. Create is authentication-only. `DiscussionThreadController` resolves the caller's id and falls back to `1L` when the principal is null (`Controller/DiscussionThreadController.java:35,78`); the service then loads that user and saves (`DiscussionThreadServiceImpl.createThread`, `createPost`).
11. There is **no course-membership or publication check**. `getThreadsForCourse` only asserts the course exists (`DiscussionThreadServiceImpl.java:38–42`); any authenticated user can create a thread or post in any course id, including an unpublished course.
12. `createPost` validates that the parent post exists but **not that the parent belongs to the same thread** (`DiscussionThreadServiceImpl.createPost`). **derived:** a reply can be attached to a post in another thread, producing a cross-thread tree and confusing cascade deletes.
13. There is no pagination, no count, no "solved" marker, and no edit window. `getPostsForThread` returns a flat `List<Post>` ordered by `createdAt ASC` (`:110–113`); nesting is reconstructed only by the (unused) `parentPostId` field. Thread deletion cascades posts through `cascade = CascadeType.ALL, orphanRemoval = true` (`Entity/DiscussionThread.java:66–69`), satisfying `AG §10`.

---

## 4. Legacy discussion: one-level replies and uneven moderation

14. `getCourseDiscussion` / `getLectureDiscussion` return only top-level posts, each with **one** level of replies (`DiscussionServiceImpl.toDtoWithReplies`). `DiscussionPost` has no `replies` collection, so a reply-to-a-reply is never returned under its parent. **derived:** nested conversation is silently flattened.
15. `deletePost` deletes direct replies first, then the post (`DiscussionServiceImpl.java:123–126`). **derived:** a grandchild reply still references a deleted direct reply, so the delete can fail on the FK; there is no recursive delete.
16. The legacy create path is the **only** engagement write that runs the profanity filter: `censor()` masks the body, sets `flagged = true`, and inserts a `FlaggedContent` row when the text is dirty (`DiscussionServiceImpl.java:62–68,100–106`). Threads, posts, messages and announcements bypass it.
17. Read access is inconsistent. `GET /api/courses/**` is `permitAll` (`Config/SecurityConfig.java:83`), so `GET /api/courses/{id}/discussion` is public and exposes author names, avatars and post bodies. `GET /api/lectures/{id}/discussion` is not matched by that rule and therefore requires authentication (`SecurityConfig.java:87`). **derived:** the same forum content is public in one route shape and private in another.
18. No discussion write checks enrollment, course publication or role. Any authenticated account can post into any course's forum; a `STUDENT` can post into a course they never bought.

---

## 5. Notes and bookmarks: owned, but attached to the wrong entity

19. `Note` binds `user`, `course`, `lecture`, `timestampSeconds`, `title`, `content`, `tags` (`Entity/Note.java:20–40`); `Bookmark` binds `user`, `lecture`, `timestampSeconds`, `label` (`Entity/Bookmark.java:18–29`).
20. Ownership is enforced on update/delete: a non-owner receives "not found" for notes (`NoteServiceImpl.getOwnedNote`) and bookmarks (`BookmarkServiceImpl.deleteBookmark`). Reads are always filtered by the principal's user id (`NoteServiceImpl.getNotes`, `BookmarkServiceImpl.getBookmarks`). This is the strongest ownership pattern in the engagement area.
21. Both attach to **`Lecture`**, not `CourseContent`. `AG §4` says notes and bookmarks attach to `content_id` (`CourseContent`). Because `Lecture` and `CourseContent` are parallel models with no link (see B analysis), the preserved feature is wired to the model that the player uses, not the model the reference names.
22. `NoteServiceImpl.applyRequest` sets `courseId` and `lectureId` independently with no consistency check (`NoteServiceImpl.java:59–75`). **derived:** a note can point at course A and a lecture from course B. `timestampSeconds` is never validated against the lecture duration and may be negative. `Bookmark` has no uniqueness constraint, so the same timestamp can be bookmarked repeatedly; `update` for notes ignores `null`, so a field can never be cleared.
23. There is no enrollment check on create or list, and `DELETE /api/bookmarks/lecture/{lectureId}` bulk-deletes every bookmark for that lecture for the caller (`BookmarkServiceImpl.deleteBookmarkByLecture`). That is convenient, but it is an unconfirmed destructive action on a GET-adjacent screen.

---

## 6. Reviews (Feedback): correct uniqueness, missing eligibility

24. `Feedback` has the reference unique constraint `(user_id, course_id)` and a 1–5 rating check (`Entity/Feedback.java:14–16`; `FeedbackServiceImpl.java:37–39`). Submit is an upsert: an existing row is updated, not duplicated (`FeedbackServiceImpl.java:46–53`). Average rating is computed in the repository and rounded to one decimal (`FeedbackServiceImpl.getAverageRating`).
25. **Anyone authenticated can review any course.** `submitFeedback` checks that the course and user exist, not that the user is enrolled or has completed the course (`FeedbackServiceImpl.submitFeedback`). The route is `POST /api/courses/{courseId}/feedback` (`Controller/FeedbackController.java:23`).
26. The controller parses an untyped `Map`: `body.get("rating").toString()` throws `NullPointerException` when `rating` is absent, and `Short.valueOf` throws `NumberFormatException` for `"abc"` (`Controller/FeedbackController.java:29–32`). **derived:** both surface as 500 rather than 400.
27. Delete is author-or-admin (`FeedbackServiceImpl.deleteFeedback`). `createdAt` is `updatable = false`, so editing a review keeps the original date; there is no "edited" marker.

---

## 7. Announcements and notifications: broadcast without events

28. `Announcement` has `audience ∈ {ALL, STUDENT, TEACHER, ADMIN}` and `pinned`; reads return `ALL` plus the caller's role (`AnnouncementServiceImpl.java:24–34`). Create requires `ADMIN` or `TEACHER`; update/delete require author or admin; pin is admin-only (`AnnouncementServiceImpl.java:39,76,90–104`). This matches SCR-59.
29. Announcements are **global**. There is no `course_id`, no faculty scoping, no scheduled publish, no expiry and no read receipt. A teacher can create an `ALL` announcement that every student on the platform sees. Publishing does **not** create `Notification` rows, so it is invisible until the user opens the announcements page.
30. `Notification` is per-user with `read`, `category`, `createdAt`; the API exposes list, mark-one-read, mark-all-read and unread count (`Controller/NotificationController.java`). `markRead` verifies ownership (`NotificationServiceImpl.markRead`). Delivery pushes to `/topic/notifications/{userId}` (`NotificationServiceImpl.java:68`).
31. The **only** creation path is the teacher "nudge" (`Controller/TeacherStudentsController.java:172`). There is no notification on enrollment, payment, grade release, assignment due, announcement, live-session start, thread reply or message. The entity has no `type` or entity reference, so a notification cannot deep-link to its subject (`Entity/Notification.java`).

---

## 8. Messaging, WebSocket and live sessions

32. REST messaging is participant-scoped: `getMessages` and `sendMessage` call `requireParticipant`, which accepts either participant and rejects everyone else (`MessagingServiceImpl.java:78,98,166–177`). `startConversation` allows any user to open a conversation with any other user, with no role, enrollment or course-relationship constraint (`MessagingServiceImpl.java:131–160`). `ConversationRepository.findExisting` checks both participant orderings, but there is no DB unique constraint, so two concurrent starts can create duplicate conversations. **derived:** duplicates split the message history.
33. **STOMP authorization stops at CONNECT.** `WebSocketAuthInterceptor` authenticates the handshake and sets the user (`Security/WebSocketAuthInterceptor.java:30–58`), but it never inspects `SUBSCRIBE` destinations. Messages are broadcast to `/topic/conversations/{id}` and `/topic/user/{id}/messages` (`MessagingServiceImpl.java:116,124`) and notifications to `/topic/notifications/{userId}`. **derived (critical):** any authenticated socket can subscribe to any conversation or user topic if it knows the numeric id, and read messages/notifications live, even though the REST endpoints would reject it. The same gap applies to live-session chat topics.
34. Live sessions are teacher-owned: create resolves `course.teacher` and requires it to equal the caller (`LiveSessionServiceImpl.assertOwnedCourse`); start/end require session ownership (`assertOwnedSession`). `start` sets status `LIVE` and a random placeholder `joinUrl` (`/live/{UUID}`); there is no video infrastructure (`Entity/LiveSession.java` comment; `LiveSessionServiceImpl.start`).
35. Two access bugs. `getSessionById` performs **no** enrollment or ownership check (`LiveSessionServiceImpl.java:67–71`), and `StudentController` calls it after only `requireUserId` (`Controller/StudentController.java:51–56`) — any authenticated user can fetch any session, including a live `joinUrl`. And `getStudentSessions` filters with `enrolledCourseIds.isEmpty() || ...` (`LiveSessionServiceImpl.java:56`), so a student with **zero** enrollments sees **every** session on the platform. There is no attendance, reminder, recording or replay.
36. Live chat is a raw relay: `LiveChatWebSocketController.sendLiveChatMessage` stamps a timestamp and forwards the payload to `/topic/live-sessions/{id}/chat` with no membership, rate-limit or persistence check, and `sendLiveReaction` does the same for reactions. **derived:** anyone with a socket can post into any session's chat.

---

## 9. Moderation and sentiment

37. `GET /api/admin/flagged` runs `scanAndFlag()` before reading (`ModerationServiceImpl.java:87–88`). The scan walks **legacy** `DiscussionPost` rows and `Message` rows, classifies against keyword lists, and inserts `FlaggedContent` rows (`:39–73`). It never scans `Post` (two-tier threads), so the reference forum is outside moderation.
38. `FlaggedContent` stores `(contentType, contentId)` with no FK (`Entity/FlaggedContent.java:19–22`). Resolving or dismissing only flips `status` (`ModerationServiceImpl.resolve/dismiss`); it does not mask, delete or restore the content, and `DiscussionPost.flagged` is never cleared. Deleted content leaves stale flags. There is no user-facing report action; the controller comment admits the auto-flagger exists because "no user-facing report button exists yet" (`Controller/ModerationController.java`).
39. `POST /api/censor` is available to any authenticated user and returns the masked text plus detection metadata (`Controller/CensorController.java`). It is a keyword heuristic, not an AI classifier (`Service/impl/ProfanityFilterServiceImpl`, `Service/support/KeywordLists`). Only the legacy discussion path calls it.
40. Sentiment is a deterministic keyword count over legacy posts and messages (`SentimentServiceImpl`, class comment: "not real sentiment analysis"). It produces totals, per-course-type sentiment, negative keywords and teacher scores on demand; nothing is persisted, so there is no trend history. `spec §8` explicitly defers a fine-tuned classifier and ships a keyword lexicon first, so this is scope-aligned. Two-tier `Post` rows are absent from the sample.

---

## 10. Findings summary

| # | Finding | Why it matters for the build |
|---|---|---|
| E1 | Two discussion models coexist; the reference thread/post API is dark and the UI uses legacy `discussion_posts` | pick one canonical model before adding features |
| E2 | `GET /api/courses/{id}/discussion` is public while `GET /api/lectures/{id}/discussion` requires auth | same content has two privacy levels |
| E3 | No enrollment/publication check on discussion, notes, bookmarks, reviews or threads | any account can write into any course |
| E4 | Thread/post create falls back to `userId = 1L` when the principal is null | silent impersonation if a route is ever opened |
| E5 | A reply can reference a parent in another thread | cross-thread trees; fragile cascades |
| E6 | Legacy replies are only one level deep; deeper replies are never returned | conversation appears to lose messages |
| E7 | Profanity filtering and auto-flagging apply only to legacy posts | threads/messages are unmoderated |
| E8 | Flag resolution does not remove or mask content; `flagged` is never cleared | the queue is advisory, not enforcement |
| E9 | `GET /api/admin/flagged` writes on read (`scanAndFlag`) | side-effecting GET; cache/retry hazards |
| E10 | Notes/bookmarks bind to `Lecture`, not `CourseContent` | violates `AG §4`; breaks when the curriculum model is unified |
| E11 | Note course and lecture are independently settable; timestamps unvalidated; bookmarks not unique | inconsistent data and duplicates |
| E12 | Reviews require no enrollment/completion; malformed rating returns 500 | fake ratings; poor API errors |
| E13 | Announcements are global; publishing creates no notifications | teacher broadcast can cross course boundaries |
| E14 | Notifications are only created by the teacher nudge | all other domain events are silent |
| E15 | STOMP SUBSCRIBE is unauthorized; topics are guessable by numeric id | live message/notification eavesdropping |
| E16 | `getSessionById` has no enrollment check; zero-enrollment students see all sessions | live join links leak |
| E17 | No pagination on threads, posts, messages or discussion | unbounded payloads as data grows |
| E18 | Moderation/sentiment ignore two-tier posts and have no history | blind spots in the canonical model |

## 11. Gaps — what the sources do NOT provide

- **NO RULE** on whether community reads/writes require enrollment, or whether forums are public for published courses.
- **NO RULE** on editing/deleting windows for threads and posts beyond author/admin ownership.
- **NO CONTRACT** for notification types, payloads or which domain events must notify which roles.
- **NO SPEC** for announcement scoping (global vs course vs department) or scheduling/expiry.
- **NO REQUIREMENT** for message retention, block lists, reporting or appeal flow.
- **NO SCHEMA** for live attendance, reminders or recordings.
- **NO RULE** on review eligibility (enrolled only, completed only, one per course already fixed).

## 12. Open questions

| ID | Question | Feeds |
|---|---|---|
| E-Q1 | Which discussion model is canonical — `discussion_thread`/`post` or legacy `discussion_posts`? | A-13 / Wave 5 |
| E-Q2 | Do community writes require enrollment (or course staff), and are reads enrollment-gated? | A-14 / Wave 5 |
| E-Q3 | Do notes/bookmarks bind to `Lecture` or `CourseContent`? | A-15 (extends A-05) / Wave 2 |
| E-Q4 | Should STOMP `SUBSCRIBE` be authorized per destination? | A-16 / Wave 1 |
| E-Q5 | Are announcements global or course-scoped, and which events create notifications? | A-17 / Wave 5 |
| E-Q6 | Is live-session join enrollment-gated, and is attendance in scope? | A-18 / Wave 5 |
