package br.com.example.senac.businessDocsAi.security;

import br.com.example.senac.businessDocsAi.user.Enum.UserEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public AppUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto atual");
        }

        return appUserDetails;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentUserName() {
        return getCurrentUser().getUser().getName();
    }

    public UserEnum getCurrentRole() {
        return getCurrentUser().getUser().getRole();
    }

    public boolean isEditorOuAdmin() {
        UserEnum role = getCurrentRole();
        return role == UserEnum.EDITOR || role == UserEnum.ADMIN;
    }
}
