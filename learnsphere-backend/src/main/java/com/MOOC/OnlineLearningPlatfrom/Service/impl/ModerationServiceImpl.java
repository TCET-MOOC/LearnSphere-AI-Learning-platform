package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.FlaggedContentDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Message;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.DiscussionThreadRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.FlaggedContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.MessageRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PostRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.ModerationService;
import com.MOOC.OnlineLearningPlatfrom.Service.support.KeywordLists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModerationServiceImpl implements ModerationService {

    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final FlaggedContentRepository flaggedContentRepository;
    private final DiscussionThreadRepository discussionThreadRepository;

    public ModerationServiceImpl(PostRepository postRepository,
                                  MessageRepository messageRepository,
                                  FlaggedContentRepository flaggedContentRepository,
                                  DiscussionThreadRepository discussionThreadRepository) {
        this.postRepository = postRepository;
        this.messageRepository = messageRepository;
        this.flaggedContentRepository = flaggedContentRepository;
        this.discussionThreadRepository = discussionThreadRepository;
    }

    @Override
    public FlaggedContent.Reason classify(String text) {
        if (text == null || text.isBlank()) return null;
        boolean bullying = KeywordLists.containsAny(text, KeywordLists.BULLYING_WORDS);
        boolean spam = KeywordLists.containsAny(text, KeywordLists.SPAM_WORDS);
        if (bullying) return FlaggedContent.Reason.BULLYING;
        if (spam) return FlaggedContent.Reason.SPAM;
        return null;
    }

    @Override
    @Transactional
    public void recordAutoFlag(FlaggedContent.ContentType type, Long contentId, FlaggedContent.Reason reason) {
        if (type == null || contentId == null || reason == null) return;
        if (flaggedContentRepository.existsByContentTypeAndContentId(type, contentId)) return;

        FlaggedContent flag = new FlaggedContent();
        flag.setContentType(type);
        flag.setContentId(contentId);
        flag.setReason(reason);
        flag.setReporterId(null);
        flag.setStatus(FlaggedContent.Status.PENDING);
        flaggedContentRepository.save(flag);
    }

    @Transactional
    public void scanAndFlag() {
        for (Post post : postRepository.findAll()) {
            if (post.getBody() == null) continue;
            if (flaggedContentRepository.existsByContentTypeAndContentId(FlaggedContent.ContentType.DISCUSSION_POST, post.getPostId())) continue;

            FlaggedContent.Reason reason = classify(post.getBody());
            if (reason == null) continue;

            FlaggedContent flag = new FlaggedContent();
            flag.setContentType(FlaggedContent.ContentType.DISCUSSION_POST);
            flag.setContentId(post.getPostId());
            flag.setReason(reason);
            flag.setReporterId(null);
            flag.setStatus(FlaggedContent.Status.PENDING);
            flaggedContentRepository.save(flag);

            post.setFlagged(true);
            postRepository.save(post);
        }

        for (DiscussionThread thread : discussionThreadRepository.findAll()) {
            String combined = (thread.getTitle() != null ? thread.getTitle() : "") + " " + (thread.getContent() != null ? thread.getContent() : "");
            if (flaggedContentRepository.existsByContentTypeAndContentId(FlaggedContent.ContentType.DISCUSSION_THREAD, thread.getThreadId())) continue;

            FlaggedContent.Reason reason = classify(combined);
            if (reason == null) continue;

            FlaggedContent flag = new FlaggedContent();
            flag.setContentType(FlaggedContent.ContentType.DISCUSSION_THREAD);
            flag.setContentId(thread.getThreadId());
            flag.setReason(reason);
            flag.setReporterId(null);
            flag.setStatus(FlaggedContent.Status.PENDING);
            flaggedContentRepository.save(flag);

            thread.setFlagged(true);
            discussionThreadRepository.save(thread);
        }

        for (Message message : messageRepository.findAll()) {
            if (message.getText() == null) continue;
            if (flaggedContentRepository.existsByContentTypeAndContentId(FlaggedContent.ContentType.MESSAGE, message.getId())) continue;

            FlaggedContent.Reason reason = classify(message.getText());
            if (reason == null) continue;

            FlaggedContent flag = new FlaggedContent();
            flag.setContentType(FlaggedContent.ContentType.MESSAGE);
            flag.setContentId(message.getId());
            flag.setReason(reason);
            flag.setReporterId(null);
            flag.setStatus(FlaggedContent.Status.PENDING);
            flaggedContentRepository.save(flag);
        }
    }

    @Override
    @Transactional
    public List<FlaggedContentDto> getFlagged(String category) {
        scanAndFlag();

        List<FlaggedContent> flags;
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            FlaggedContent.Reason reason = mapCategory(category);
            flags = reason == null ? List.of() : flaggedContentRepository.findByStatusAndReasonOrderByCreatedAtDesc(FlaggedContent.Status.PENDING, reason);
        } else {
            flags = flaggedContentRepository.findByStatusOrderByCreatedAtDesc(FlaggedContent.Status.PENDING);
        }

        return flags.stream().map(this::toDto).toList();
    }

    private FlaggedContent.Reason mapCategory(String category) {
        try {
            String normalized = category.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            if (normalized.equals("HIGH_RISK")) return FlaggedContent.Reason.HIGH_RISK;
            return FlaggedContent.Reason.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private FlaggedContentDto toDto(FlaggedContent flag) {
        String authorName = "Unknown user";
        String courseTitle = null;
        String snippet = "";

        if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_POST) {
            Post post = postRepository.findById(flag.getContentId()).orElse(null);
            if (post != null) {
                authorName = post.getUser() != null ? post.getUser().getFullName() : authorName;
                courseTitle = post.getThread() != null && post.getThread().getCourse() != null ? post.getThread().getCourse().getTitle() : null;
                snippet = truncate(post.getBody());
            }
        } else if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_THREAD) {
            DiscussionThread thread = discussionThreadRepository.findById(flag.getContentId()).orElse(null);
            if (thread != null) {
                authorName = thread.getUser() != null ? thread.getUser().getFullName() : authorName;
                courseTitle = thread.getCourse() != null ? thread.getCourse().getTitle() : null;
                snippet = truncate(thread.getTitle() + " - " + thread.getContent());
            }
        } else if (flag.getContentType() == FlaggedContent.ContentType.MESSAGE) {
            Message message = messageRepository.findById(flag.getContentId()).orElse(null);
            if (message != null) {
                authorName = message.getSender() != null ? message.getSender().getFullName() : authorName;
                courseTitle = message.getConversation() != null && message.getConversation().getCourse() != null
                        ? message.getConversation().getCourse().getTitle() : null;
                snippet = truncate(message.getText());
            }
        }

        return FlaggedContentDto.build(flag, authorName, courseTitle, snippet);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 160 ? text.substring(0, 160) + "..." : text;
    }

    @Override
    @Transactional
    public FlaggedContentDto resolve(Long id) {
        FlaggedContent flag = flaggedContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged content not found with id: " + id));
        flag.setStatus(FlaggedContent.Status.RESOLVED);

        if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_POST) {
            Post post = postRepository.findById(flag.getContentId()).orElse(null);
            if (post != null) {
                post.setBody("[Content removed by moderator]");
                post.setFlagged(false);
                postRepository.save(post);
            }
        } else if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_THREAD) {
            DiscussionThread thread = discussionThreadRepository.findById(flag.getContentId()).orElse(null);
            if (thread != null) {
                thread.setContent("[Content removed by moderator]");
                thread.setFlagged(false);
                discussionThreadRepository.save(thread);
            }
        } else if (flag.getContentType() == FlaggedContent.ContentType.MESSAGE) {
            Message message = messageRepository.findById(flag.getContentId()).orElse(null);
            if (message != null) {
                message.setText("[Content removed by moderator]");
                messageRepository.save(message);
            }
        }

        flaggedContentRepository.save(flag);
        return toDto(flag);
    }

    @Override
    @Transactional
    public FlaggedContentDto dismiss(Long id) {
        FlaggedContent flag = flaggedContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged content not found with id: " + id));
        flag.setStatus(FlaggedContent.Status.DISMISSED);

        if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_POST) {
            Post post = postRepository.findById(flag.getContentId()).orElse(null);
            if (post != null) {
                post.setFlagged(false);
                postRepository.save(post);
            }
        } else if (flag.getContentType() == FlaggedContent.ContentType.DISCUSSION_THREAD) {
            DiscussionThread thread = discussionThreadRepository.findById(flag.getContentId()).orElse(null);
            if (thread != null) {
                thread.setFlagged(false);
                discussionThreadRepository.save(thread);
            }
        }

        flaggedContentRepository.save(flag);
        return toDto(flag);
    }

    @Override
    @Transactional
    public FlaggedContentDto flagPost(Long postId, Long reporterId, String reasonStr) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        post.setFlagged(true);
        postRepository.save(post);

        FlaggedContent.Reason reason = mapCategory(reasonStr != null ? reasonStr : "SPAM");
        if (reason == null) reason = FlaggedContent.Reason.SPAM;

        FlaggedContent flag = new FlaggedContent();
        flag.setContentType(FlaggedContent.ContentType.DISCUSSION_POST);
        flag.setContentId(postId);
        flag.setReporterId(reporterId);
        flag.setReason(reason);
        flag.setStatus(FlaggedContent.Status.PENDING);
        flaggedContentRepository.save(flag);

        return toDto(flag);
    }

    @Override
    @Transactional
    public FlaggedContentDto flagThread(Long threadId, Long reporterId, String reasonStr) {
        DiscussionThread thread = discussionThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found with id: " + threadId));
        thread.setFlagged(true);
        discussionThreadRepository.save(thread);

        FlaggedContent.Reason reason = mapCategory(reasonStr != null ? reasonStr : "SPAM");
        if (reason == null) reason = FlaggedContent.Reason.SPAM;

        FlaggedContent flag = new FlaggedContent();
        flag.setContentType(FlaggedContent.ContentType.DISCUSSION_THREAD);
        flag.setContentId(threadId);
        flag.setReporterId(reporterId);
        flag.setReason(reason);
        flag.setStatus(FlaggedContent.Status.PENDING);
        flaggedContentRepository.save(flag);

        return toDto(flag);
    }
}
