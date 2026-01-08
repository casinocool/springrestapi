package ru.itmentor.spring.boot_security.demo.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.itmentor.spring.boot_security.demo.model.Role;
import ru.itmentor.spring.boot_security.demo.model.User;
import ru.itmentor.spring.boot_security.demo.repository.RoleRepository;
import ru.itmentor.spring.boot_security.demo.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("=== Инициализация данных ===");

        // Удаляем старого админа если он есть
        userRepository.findByUsername("admin").ifPresent(user -> {
            System.out.println("Удаляем старого администратора...");
            userRepository.delete(user);
        });

        // Создаем роли
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    return roleRepository.save(role);
                });

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });

        // Создаем НОВОГО администратора с правильным паролем
        User admin = new User();
        admin.setUsername("admin");
        admin.setName("Admin");
        admin.setLastName("Adminov");
        admin.setAge(30);

        // Кодируем пароль ТУТ ЖЕ
        String rawPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        admin.setPassword(encodedPassword);

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        admin.setRoles(roles);

        userRepository.save(admin);

        System.out.println("Создан НОВЫЙ администратор:");
        System.out.println("Логин: " + admin.getUsername());
        System.out.println("Пароль (сырой): " + rawPassword);
        System.out.println("Пароль (хеш): " + encodedPassword);

        // Проверяем сразу
        boolean check = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("Проверка после создания: " + check);

        System.out.println("=== Инициализация завершена ===");
    }
}