package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;

import java.util.List;

public interface DiscussionThreadService {
    List<DiscussionThread> getThreadsForCourse(Long courseId);
    DiscussionThread getThreadById(Long threadId);
    DiscussionThread createThread(Long courseId, Long userId, DiscussionThread thread);
    DiscussionThread updateThread(Long threadId, Long userId, boolean isAdmin, DiscussionThread thread);
    void deleteThread(Long threadId, Long userId, boolean isAdmin);

    List<Post> getPostsForThread(Long threadId);
    Post createPost(Long threadId, Long userId, Post post);
    Post updatePost(Long postId, Long userId, boolean isAdmin, Post post);
    void deletePost(Long postId, Long userId, boolean isAdmin);
}
