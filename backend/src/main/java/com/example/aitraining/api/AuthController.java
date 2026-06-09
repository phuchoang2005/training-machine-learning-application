package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.dto.ApiDtos.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @GetMapping("/auth/me")
    CurrentUser me() {
        User user = CurrentUserContext.require();
        return new CurrentUser(user.userId(), user.email(), user.fullName(), user.role(), user.status(), user.lastLoginAt());
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
