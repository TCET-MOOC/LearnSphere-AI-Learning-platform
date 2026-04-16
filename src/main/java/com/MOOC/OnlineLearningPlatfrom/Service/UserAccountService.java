package com.MOOC.OnlineLearningPlatfrom.Service;

import java.util.List;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

public interface UserAccountService {
    List<UserAccount> getAllUsers();
    UserAccount getUserById(Long id);
    UserAccount createUser(UserAccount user);
    UserAccount updateUser(Long id, UserAccount user);
    void deleteUser(Long id);
}
