package com.MOOC.OnlineLearningPlatfrom.Config;

import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        List.of("STUDENT", "TEACHER", "ADMIN").forEach(name -> {
            if (roleRepository.findByName(name) == null) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
            }
        });
    }
}
