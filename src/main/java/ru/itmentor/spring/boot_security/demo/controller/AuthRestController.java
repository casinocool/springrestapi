package ru.itmentor.spring.boot_security.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.itmentor.spring.boot_security.demo.model.Role;
import ru.itmentor.spring.boot_security.demo.model.User;
import ru.itmentor.spring.boot_security.demo.service.RoleService;
import ru.itmentor.spring.boot_security.demo.service.UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthRestController {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // Конструктор с инъекцией зависимостей
    @Autowired
    public AuthRestController(UserService userService,
                              RoleService roleService,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // Регистрация нового пользователя
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> signupRequest) {
        try {
            String username = signupRequest.get("username");
            String password = signupRequest.get("password");
            String name = signupRequest.get("name");
            String lastName = signupRequest.get("lastName");
            String ageStr = signupRequest.get("age");

            // Проверка обязательных полей
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Username and password are required"
                ));
            }

            // Проверка существования пользователя
            if (userService.getUserByUsername(username) != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Username is already taken"
                ));
            }

            // Создание пользователя
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setName(name != null ? name : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setAge(ageStr != null ? Integer.parseInt(ageStr) : 0);

            // Назначение роли USER по умолчанию
            Role userRole = roleService.findByName("ROLE_USER");
            user.setRoles(Collections.singleton(userRole));

            // Сохранение пользователя
            userService.saveUser(user);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User registered successfully",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    // Логин
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Username and password are required"
                ));
            }

            // Аутентификация
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Получение информации о пользователе
            User user = userService.getUserByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User logged in successfully");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "name", user.getName(),
                    "lastName", user.getLastName(),
                    "age", user.getAge(),
                    "roles", user.getRoles().stream()
                            .map(Role::getName)
                            .toList()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", "error",
                            "message", "Invalid username or password"
                    ));
        }
    }

    // Получение текущего пользователя
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Not authenticated"));
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("name", user.getName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("age", user.getAge());
        userInfo.put("roles", user.getRoles().stream()
                .map(Role::getName)
                .toList());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "user", userInfo
        ));
    }

    // Выход
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Logged out successfully"
        ));
    }
}