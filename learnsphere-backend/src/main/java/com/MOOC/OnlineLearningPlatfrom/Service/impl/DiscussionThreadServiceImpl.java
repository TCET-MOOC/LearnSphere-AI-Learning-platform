package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DiscussionThreadRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PostRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.DiscussionThreadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DiscussionThreadServiceImpl implements DiscussionThreadService {

    private final DiscussionThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public DiscussionThreadServiceImpl(DiscussionThreadRepository threadRepository,
                                       PostRepository postRepository,
                                       CourseRepository courseRepository,
                                       UserAccountRepository userAccountRepository) {
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionThread> getThreadsForCourse(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return threadRepository.findByCourse_IdOrderByCreatedAtDesc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscussionThread getThreadById(Long threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found with id: " + threadId));
    }

    @Override
    public DiscussionThread createThread(Long courseId, Long userId, DiscussionThread thread) {
        if (thread.getTitle() == null || thread.getTitle().isBlank()) {
            throw new BadRequestException("Thread title is required");
        }
        if (thread.getContent() == null || thread.getContent().isBlank()) {
            throw new BadRequestException("Thread content is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        thread.setCourse(course);
        thread.setUser(user);
        return threadRepository.save(thread);
    }

    @Override
    public DiscussionThread updateThread(Long threadId, Long userId, boolean isAdmin, DiscussionThread updated) {
        DiscussionThread existing = getThreadById(threadId);
        if (!isAdmin && (existing.getUser() == null || !existing.getUser().getUserId().equals(userId))) {
            throw new BadRequestException("Only the thread author or admin can update this thread");
        }

        if (updated.getTitle() != null && !updated.getTitle().isBlank()) {
            existing.setTitle(updated.getTitle());
        }
        if (updated.getContent() != null && !updated.getContent().isBlank()) {
            existing.setContent(updated.getContent());
        }

        return threadRepository.save(existing);
    }

    @Override
    public void deleteThread(Long threadId, Long userId, boolean isAdmin) {
        DiscussionThread existing = getThreadById(threadId);
        if (!isAdmin && (existing.getUser() == null || !existing.getUser().getUserId().equals(userId))) {
            throw new BadRequestException("Only the thread author or admin can delete this thread");
        }
        threadRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsForThread(Long threadId) {
        getThreadById(threadId);
        return postRepository.findByThread_ThreadIdOrderByCreatedAtAsc(threadId);
    }

    @Override
    public Post createPost(Long threadId, Long userId, Post post) {
        if (post.getBody() == null || post.getBody().isBlank()) {
            throw new BadRequestException("Post body cannot be empty");
        }

        DiscussionThread thread = getThreadById(threadId);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        post.setThread(thread);
        post.setUser(user);

        if (post.getParentPostId() != null) {
            Post parent = postRepository.findById(post.getParentPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent post not found with id: " + post.getParentPostId()));
            post.setParentPost(parent);
        }

        return postRepository.save(post);
    }

    @Override
    public Post updatePost(Long postId, Long userId, boolean isAdmin, Post updated) {
        Post existing = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!isAdmin && (existing.getUser() == null || !existing.getUser().getUserId().equals(userId))) {
            throw new BadRequestException("Only the post author or admin can edit this post");
        }

        if (updated.getBody() != null && !updated.getBody().isBlank()) {
            existing.setBody(updated.getBody());
        }

        return postRepository.save(existing);
    }

    @Override
    public void deletePost(Long postId, Long userId, boolean isAdmin) {
        Post existing = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!isAdmin && (existing.getUser() == null || !existing.getUser().getUserId().equals(userId))) {
            throw new BadRequestException("Only the post author or admin can delete this post");
        }

        postRepository.delete(existing);
    }
}
