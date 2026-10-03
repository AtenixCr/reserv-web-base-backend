package com.tourism.business.auth.repository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
@Repository
public class UserRepository implements UserDetailsService {
    public static final class Account extends User {
        public final long id;
        public final String displayName;
        public final String role;
        public Account(long id, String username, String hash, String name, String role, boolean active) {
            super(username, hash, active, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            this.id = id; this.displayName = name; this.role = role;
        }
    }
    private final AppUserJpaRepository users;
    public UserRepository(AppUserJpaRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) {
        var user=users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new Account(user.getId(),user.getUsername(),user.getPasswordHash(),user.getName(),user.getRole(),user.isActive());
    }
    public boolean isCurrent(Account account) {
        return users.existsByIdAndRoleAndActiveTrue(account.id,account.role);
    }
}
