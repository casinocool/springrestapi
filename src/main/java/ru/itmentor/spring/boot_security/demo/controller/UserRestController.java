package ru.itmentor.spring.boot_security.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmentor.spring.boot_security.demo.model.Role;
import ru.itmentor.spring.boot_security.demo.model.User;
import ru.itmentor.spring.boot_security.demo.service.RoleService;
import ru.itmentor.spring.boot_security.demo.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    // Получить всех пользователей (ВРЕМЕННО без проверки прав)
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> userList = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("name", user.getName());
                        userMap.put("lastName", user.getLastName());
                        userMap.put("age", user.getAge());
                        userMap.put("roles", user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList()));
                        return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", userList.size(),
                    "users", userList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    // Получить пользователя по ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("name", user.getName());
            userMap.put("lastName", user.getLastName());
            userMap.put("age", user.getAge());
            userMap.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "user", userMap
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", "error",
                            "message", "User not found"
                    ));
        }
    }

    // Создать нового пользователя
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData) {
        try {
            User user = new User();
            user.setUsername((String) userData.get("username"));
            user.setPassword((String) userData.get("password"));
            user.setName((String) userData.get("name"));
            user.setLastName((String) userData.get("lastName"));

            if (userData.get("age") != null) {
                user.setAge(Integer.parseInt(userData.get("age").toString()));
            }

            // Назначение ролей
            if (userData.get("roleIds") != null) {
                List<Long> roleIds = (List<Long>) userData.get("roleIds");
                for (Long roleId : roleIds) {
                    user.getRoles().add(roleService.findById(roleId));
                }
            }

            userService.saveUser(user);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User created successfully",
                    "userId", user.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    // Обновить пользователя
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            User user = userService.getUserById(id);

            if (userData.get("username") != null) {
                user.setUsername((String) userData.get("username"));
            }
            if (userData.get("password") != null && !((String) userData.get("password")).isEmpty()) {
                user.setPassword((String) userData.get("password"));
            }
            if (userData.get("name") != null) {
                user.setName((String) userData.get("name"));
            }
            if (userData.get("lastName") != null) {
                user.setLastName((String) userData.get("lastName"));
            }
            if (userData.get("age") != null) {
                user.setAge(Integer.parseInt(userData.get("age").toString()));
            }

            // Обновление ролей
            if (userData.get("roleIds") != null) {
                user.getRoles().clear();
                List<Long> roleIds = (List<Long>) userData.get("roleIds");
                for (Long roleId : roleIds) {
                    user.getRoles().add(roleService.findById(roleId));
                }
            }

            userService.saveUser(user);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    // Удалить пользователя
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }
}