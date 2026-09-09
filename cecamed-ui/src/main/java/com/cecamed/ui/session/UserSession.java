package com.cecamed.ui.session;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Component
public class UserSession {

    private String username;
    private String fullName;
    private UserRole role;
    private LocalDateTime loginTime;
    private boolean authenticated;

    public void setSession(String username, String fullName, UserRole role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.loginTime = LocalDateTime.now();
        this.authenticated = true;
    }

    public void clear() {
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.loginTime = null;
        this.authenticated = false;
    }

    public boolean isMedico() {
        return role == UserRole.MEDICO;
    }

    public boolean isRecepcion() {
        return role == UserRole.RECEPCION;
    }
}
