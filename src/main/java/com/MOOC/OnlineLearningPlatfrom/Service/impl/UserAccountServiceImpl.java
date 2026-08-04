package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import org.springframework.stereotype.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.UserAccountService;

import java.util.List;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public List<UserAccount> getAllUsers() {
        return userAccountRepository.findAll();
    }

    @Override
    public UserAccount getUserById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public UserAccount createUser(UserAccount user) {
        return userAccountRepository.save(user);
    }

    @Override
    public UserAccount updateUser(Long id, UserAccount user) {
        UserAccount existing = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existing.setFullName(user.getFullName());
        existing.setEmail(user.getEmail());
        existing.setDepartment(user.getDepartment());
        return userAccountRepository.save(existing);
    }

    @Override
    public void deleteUser(Long id) {
        userAccountRepository.deleteById(id);
    }
}
