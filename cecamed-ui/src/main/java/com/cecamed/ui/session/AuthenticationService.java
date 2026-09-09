package com.cecamed.ui.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserSession userSession;

    private static final Map<String, AuthUser> PRESET_ACCOUNTS = new HashMap<>();

    static {
        PRESET_ACCOUNTS.put("medico", new AuthUser("medico", "medico123", "Dr. Carlos Eduardo Morales", UserRole.MEDICO));
        PRESET_ACCOUNTS.put("admin", new AuthUser("admin", "admin123", "Dra. Ana Lucía Gómez", UserRole.MEDICO));
        PRESET_ACCOUNTS.put("recepcion", new AuthUser("recepcion", "recepcion123", "Licda. Sofía Méndez", UserRole.RECEPCION));
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String normalizedUser = username.trim().toLowerCase();
        AuthUser user = PRESET_ACCOUNTS.get(normalizedUser);

        if (user != null && user.password().equals(password.trim())) {
            userSession.setSession(user.username(), user.fullName(), user.role());
            return true;
        }

        return false;
    }

    public void logout() {
        userSession.clear();
    }

    private record AuthUser(String username, String password, String fullName, UserRole role) {}
}
