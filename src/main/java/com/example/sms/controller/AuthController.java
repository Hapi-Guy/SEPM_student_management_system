package com.example.sms.controller;

import com.example.sms.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Returns the logged-in user. The frontend calls this to check the credentials
     * and to decide which buttons to show for the user's role.
     */
    @GetMapping("/me")
    public UserDTO currentUser(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        return new UserDTO(authentication.getName(), role);
    }
}
