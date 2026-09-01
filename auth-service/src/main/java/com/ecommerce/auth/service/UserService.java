package com.ecommerce.auth.service;

import com.ecommerce.auth.models.response.UserPageResponse;
import com.ecommerce.auth.models.response.UserResponse;
import com.ecommerce.auth.domain.AppUser;
import com.ecommerce.auth.domain.Role;
import com.ecommerce.auth.exception.AdminAccessRevokedException;
import com.ecommerce.auth.exception.AdminAccountProtectionException;
import com.ecommerce.auth.exception.DuplicateEmailException;
import com.ecommerce.auth.exception.LastEnabledAdminException;
import com.ecommerce.auth.exception.UserNotFoundException;
import com.ecommerce.auth.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(String email, String displayName, String rawPassword) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (repository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        try {
            AppUser user = new AppUser(
                    normalizedEmail,
                    displayName.strip(),
                    passwordEncoder.encode(rawPassword),
                    Role.USER
            );
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(normalizedEmail, exception);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserPageResponse listUsers(int page, int size, long currentAdminId) {
        requireEnabledAdmin(currentAdminId);
        Page<AppUser> users = repository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
        return UserPageResponse.from(users.map(UserResponse::from));
    }

    @Transactional
    public UserResponse changeRole(long targetUserId, Role newRole, long currentAdminId) {
        List<AppUser> enabledAdmins = lockEnabledAdminsAndRequireCurrent(currentAdminId);
        AppUser target = repository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        if (target.getRole() == newRole) {
            return UserResponse.from(target);
        }
        if (targetUserId == currentAdminId && newRole != Role.ADMIN) {
            throw new AdminAccountProtectionException("You cannot demote your own administrator account");
        }
        if (target.getRole() == Role.ADMIN && target.isEnabled()) {
            ensureAnotherEnabledAdminExists(enabledAdmins);
        }

        target.changeRole(newRole);
        return UserResponse.from(repository.saveAndFlush(target));
    }

    @Transactional
    public UserResponse changeEnabled(long targetUserId, boolean enabled, long currentAdminId) {
        List<AppUser> enabledAdmins = lockEnabledAdminsAndRequireCurrent(currentAdminId);
        AppUser target = repository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        if (target.isEnabled() == enabled) {
            return UserResponse.from(target);
        }
        if (targetUserId == currentAdminId && !enabled) {
            throw new AdminAccountProtectionException("You cannot disable your own administrator account");
        }
        if (target.getRole() == Role.ADMIN && target.isEnabled() && !enabled) {
            ensureAnotherEnabledAdminExists(enabledAdmins);
        }

        target.changeEnabled(enabled);
        return UserResponse.from(repository.saveAndFlush(target));
    }

    @Transactional
    public void delete(long targetUserId, long currentAdminId) {
        List<AppUser> enabledAdmins = lockEnabledAdminsAndRequireCurrent(currentAdminId);
        AppUser target = repository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        if (targetUserId == currentAdminId) {
            throw new AdminAccountProtectionException("You cannot delete your own administrator account");
        }
        if (target.getRole() == Role.ADMIN && target.isEnabled()) {
            ensureAnotherEnabledAdminExists(enabledAdmins);
        }
        repository.delete(target);
    }

    public boolean createSeedUserIfAbsent(String email, String displayName, String password, Role role) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (repository.existsByEmail(normalizedEmail)) {
            return false;
        }

        try {
            repository.saveAndFlush(new AppUser(
                    normalizedEmail,
                    displayName.strip(),
                    passwordEncoder.encode(password),
                    role
            ));
            return true;
        } catch (DataIntegrityViolationException exception) {
            if (repository.existsByEmail(normalizedEmail)) {
                return false;
            }
            throw exception;
        }
    }

    private AppUser findUser(long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void requireEnabledAdmin(long userId) {
        AppUser user = findUser(userId);
        if (!user.isEnabled() || user.getRole() != Role.ADMIN) {
            throw new AdminAccessRevokedException();
        }
    }

    private List<AppUser> lockEnabledAdminsAndRequireCurrent(long currentAdminId) {
        List<AppUser> enabledAdmins = repository.findEnabledByRoleForUpdate(Role.ADMIN);
        boolean currentAdminIsActive = enabledAdmins.stream()
                .anyMatch(admin -> admin.getId() == currentAdminId);
        if (!currentAdminIsActive) {
            throw new AdminAccessRevokedException();
        }
        return enabledAdmins;
    }

    private void ensureAnotherEnabledAdminExists(List<AppUser> enabledAdmins) {
        if (enabledAdmins.size() <= 1) {
            throw new LastEnabledAdminException();
        }
    }
}
