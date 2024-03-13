package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.lang.String.format;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Identity implements Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(unique = true, length = 36)
    private String uid;

    @Column(unique = true, length = 150)
    @Email
    private String email;

    @Column(length = 100)
    private String password;

    @Column
    private boolean active;

    @Column
    private boolean locked;

    @JsonIgnore
    @ManyToMany(fetch = EAGER)
    @JoinTable(name = "identity_role",
            joinColumns = @JoinColumn(name = "identity_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles;

    @Column
    private Instant lastLoggedIn;

    @Column
    private boolean deletionNotificationSent;

    @Column
    private String agencyTokenUid;

    @Column
    private Integer failedLoginAttempts;

    public Identity(String uid, String email, String password, boolean active, boolean locked, Set<Role> roles,
                    Instant lastLoggedIn, boolean deletionNotificationSent, Integer failedLoginAttempts) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.active = active;
        this.locked = locked;
        this.roles = roles;
        this.lastLoggedIn = lastLoggedIn;
        this.deletionNotificationSent = deletionNotificationSent;
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Identity(String uid, String email, String password, boolean active, boolean locked, Set<Role> roles,
                    Instant lastLoggedIn, boolean deletionNotificationSent, String agencyTokenUid, Integer failedLoginAttempts) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.active = active;
        this.locked = locked;
        this.roles = roles;
        this.lastLoggedIn = lastLoggedIn;
        this.deletionNotificationSent = deletionNotificationSent;
        this.agencyTokenUid = agencyTokenUid;
        this.failedLoginAttempts = failedLoginAttempts;
    }

    @JsonIgnore
    public void removeRoles(Collection<String> roleNamesToRemove) {
        log.info(format("Removing roles: %s", roleNamesToRemove));
        Set<Role> newRoles = this.getRoles()
                .stream()
                .filter(role -> !roleNamesToRemove.contains(role.getName()))
                .collect(Collectors.toSet());
        this.setRoles(newRoles);
    }

    @JsonIgnore
    public boolean hasAnyRole(Collection<String> rolesToCheck) {
        return this.roles.stream().anyMatch(r -> rolesToCheck.contains(r.getName()));
    }

    @Override
    public String toString() {
        return "Identity{" +
                "id=" + id +
                ", uid=" + uid +
                ", email=" + email +
                ", active=" + active +
                ", locked=" + locked +
                ", deletionNotificationSent=" + deletionNotificationSent +
                ", agencyTokenUid=" + agencyTokenUid +
                ", failedLoginAttempts=" + failedLoginAttempts +
                '}';
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
