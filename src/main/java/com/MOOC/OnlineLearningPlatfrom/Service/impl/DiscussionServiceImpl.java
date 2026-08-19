package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionPost;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DiscussionPostRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.DiscussionService;
import com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionPostRepository discussionPostRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService profanityFilterService;
    private final com.MOOC.OnlineLearningPlatfrom.Repository.FlaggedContentRepository flaggedContentRepository;

    public DiscussionServiceImpl(DiscussionPostRepository discussionPostRepository,
                                  CourseRepository courseRepository,
                                  LectureRepository lectureRepository,
                                  com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService profanityFilterService,
                                  com.MOOC.OnlineLearningPlatfrom.Repository.FlaggedContentRepository flaggedContentRepository) {
        this.discussionPostRepository = discussionPostRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.profanityFilterService = profanityFilterService;
        this.flaggedContentRepository = flaggedContentRepository;
    }

    @Override
    public List<DiscussionPostDto> getCourseDiscussion(Long courseId) {
        return discussionPostRepository.findByCourse_IdAndParentPostIsNullOrderByCreatedAtDesc(courseId).stream()
                .map(this::toDtoWithReplies)
                .toList();
    }

    @Override
    public List<DiscussionPostDto> getLectureDiscussion(Long lectureId) {
        return discussionPostRepository.findByLecture_IdAndParentPostIsNullOrderByCreatedAtDesc(lectureId).stream()
                .map(this::toDtoWithReplies)
                .toList();
    }

    @Override
    public DiscussionPostDto createPost(DiscussionPostRequestDto request, CustomUserDetails principal) {
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new BadRequestException("body is required");
        }

        ProfanityFilterService.CensorResult censorResult = profanityFilterService.censor(request.getBody());

        DiscussionPost post = new DiscussionPost();
        post.setAuthor(principal.getUser());
        post.setBody(censorResult.maskedText());
        if (!censorResult.isClean()) {
            post.setFlagged(true);
        }

        DiscussionPost parent = null;
        if (request.getParentPostId() != null) {
            parent = discussionPostRepository.findById(request.getParentPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + request.getParentPostId()));
            post.setParentPost(parent);
        }

        Course course;
        Lecture lecture = null;
        if (request.getLectureId() != null) {
            lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + request.getLectureId()));
            course = lecture.getCourse();
        } else if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        } else if (parent != null) {
            course = parent.getCourse();
            lecture = parent.getLecture();
        } else {
            throw new BadRequestException("courseId or lectureId is required");
        }

        post.setCourse(course);
        post.setLecture(lecture);

        DiscussionPost saved = discussionPostRepository.save(post);

        // If toxicity or prohibited phrases were found, automatically register in FlaggedContent for admin review
        if (!censorResult.isClean()) {
            com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent flag = new com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent();
            flag.setContentType(com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent.ContentType.DISCUSSION_POST);
            flag.setContentId(saved.getId());
            flag.setReason(censorResult.detectedReason() != null ? censorResult.detectedReason() : com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent.Reason.BULLYING);
            flag.setReporterId(null); // System AI auto-flagged
            flag.setStatus(com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent.Status.PENDING);
            flaggedContentRepository.save(flag);
        }

        return DiscussionPostDto.from(saved);
    }

    @Override
    public void deletePost(Long id, CustomUserDetails principal) {
        DiscussionPost post = discussionPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        boolean isAdmin = principal.getRoles().contains("ADMIN");
        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getUserId().equals(principal.getUser().getUserId());
        if (!isAdmin && !isAuthor) {
            throw new AccessDeniedException("Only the author or an admin can delete this post");
        }
        // Delete replies first to avoid FK constraint issues, then the post itself.
        List<DiscussionPost> replies = discussionPostRepository.findByParentPost_IdOrderByCreatedAtAsc(id);
        discussionPostRepository.deleteAll(replies);
        discussionPostRepository.delete(post);
    }

    private DiscussionPostDto toDtoWithReplies(DiscussionPost post) {
        List<DiscussionPostDto> replies = discussionPostRepository.findByParentPost_IdOrderByCreatedAtAsc(post.getId()).stream()
                .map(DiscussionPostDto::from)
                .toList();
        return DiscussionPostDto.from(post, replies);
    }
}
