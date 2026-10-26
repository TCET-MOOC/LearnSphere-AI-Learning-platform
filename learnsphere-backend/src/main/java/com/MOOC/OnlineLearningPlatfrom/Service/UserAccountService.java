package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

public interface UserAccountService {

    UserAccount getUserById(Long id);

    /**
     * Updates only the fields a user may change on their own profile. E-mail,
     * department, college, status, roles and password are deliberately not
     * accepted here; those belong to the admin/auth flows.
     */
    UserAccount updateOwnProfile(Long id, String fullName, String bio, String avatarUrl);
}
