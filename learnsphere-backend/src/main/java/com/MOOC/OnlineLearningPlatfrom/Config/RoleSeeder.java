package com.MOOC.OnlineLearningPlatfrom.Config;

import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the reference role vocabulary in every profile.
 *
 * <p>Roles are infrastructure data, not demo data: registration and
 * authorization look them up by name. Demo users, colleges, courses and
 * enrolments live in {@link DemoDataSeeder} and only run under the
 * {@code demo}/{@code test} profiles.</p>
 */
@Component
@Order(1)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        getOrCreateRole("STUDENT");
        getOrCreateRole("TEACHER");
        getOrCreateRole("ADMIN");
    }

    private Role getOrCreateRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role = roleRepository.save(role);
        }
        return role;
    }
}
