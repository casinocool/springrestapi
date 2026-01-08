package ru.itmentor.spring.boot_security.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.itmentor.spring.boot_security.demo.model.User;
import ru.itmentor.spring.boot_security.demo.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestRestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Вариант 1: Проверить пароль пользователя
    @GetMapping("/check-password")
    public ResponseEntity<?> checkPassword(
            @RequestParam String username,
            @RequestParam String password) {

        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean matches = passwordEncoder.matches(password, user.getPassword());

            response.put("found", true);
            response.put("username", user.getUsername());
            response.put("storedHash", user.getPassword());
            response.put("passwordProvided", password);
            response.put("matches", matches);
            response.put("message", matches ? "Password is correct" : "Password is incorrect");
        } else {
            response.put("found", false);
            response.put("message", "User not found: " + username);
        }

        return ResponseEntity.ok(response);
    }

    // Вариант 2: Проверить хеш
    @GetMapping("/check-hash")
    public ResponseEntity<?> checkHash(
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String hash) {

        Map<String, Object> response = new HashMap<>();

        if (password != null && hash != null) {
            boolean matches = passwordEncoder.matches(password, hash);

            response.put("password", password);
            response.put("hash", hash);
            response.put("matches", matches);
            response.put("message", matches ? "Hash matches password" : "Hash does not match password");
        } else {
            response.put("error", "Both 'password' and 'hash' parameters are required");
        }

        return ResponseEntity.ok(response);
    }

    // Вариант 3: Проверить пароль администратора
    @GetMapping("/check-admin-password")
    public ResponseEntity<?> checkAdminPassword(@RequestParam String password) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> adminOpt = userRepository.findByUsername("admin");

        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            boolean matches = passwordEncoder.matches(password, admin.getPassword());

            response.put("found", true);
            response.put("username", "admin");
            response.put("storedHash", admin.getPassword());
            response.put("passwordProvided", password);
            response.put("matches", matches);
            response.put("message", matches ? "Admin password is correct" : "Admin password is incorrect");
        } else {
            response.put("found", false);
            response.put("message", "Admin user not found in database");
        }

        return ResponseEntity.ok(response);
    }

    // Генерировать BCrypt хеш
    @GetMapping("/generate-hash")
    public ResponseEntity<?> generateHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);

        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("verification", passwordEncoder.matches(password, hash));
        response.put("sqlForInsert", "UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");

        return ResponseEntity.ok(response);
    }

    // Создать тестового администратора
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Удаляем старого если есть
            userRepository.findByUsername("admin").ifPresent(userRepository::delete);

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("Admin");
            admin.setLastName("Adminov");
            admin.setAge(30);

            userRepository.save(admin);

            response.put("status", "success");
            response.put("message", "Admin created: admin / admin123");
            response.put("username", "admin");
            response.put("password", "admin123");
            response.put("hash", admin.getPassword());

            // Проверяем сразу
            boolean check = passwordEncoder.matches("admin123", admin.getPassword());
            response.put("verification", check);
            response.put("verificationMessage", check ? "Password verification successful" : "Password verification failed");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // Получить информацию о всех пользователях
    @GetMapping("/users-info")
    public ResponseEntity<?> getAllUsersInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            var users = userRepository.findAll();

            response.put("status", "success");
            response.put("count", users.size());
            response.put("users", users.stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("name", user.getName());
                        userInfo.put("lastName", user.getLastName());
                        userInfo.put("age", user.getAge());
                        userInfo.put("passwordHash", user.getPassword());
                        userInfo.put("hashLength", user.getPassword().length());
                        userInfo.put("roles", user.getRoles().stream()
                                .map(role -> role.getName())
                                .toList());
                        return userInfo;
                    })
                    .toList());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}