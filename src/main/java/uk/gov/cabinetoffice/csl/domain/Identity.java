package uk.gov.cabinetoffice.csl.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Identity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 36)
    private String uid;

    @Column(unique = true, length = 150)
    @Email
    private String email;

    @Column(length = 100)
    private String password;

    @Column
    private String agencyTokenUid;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "identity_role",
            joinColumns = @JoinColumn(name = "identity_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles;

    private boolean active;

    private boolean locked;

    private Instant lastLoggedIn;

    private boolean deletionNotificationSent;

    public Identity(String uid, String email, String password, boolean active, boolean locked, Set<Role> roles, Instant lastLoggedIn, boolean deletionNotificationSent, String agencyTokenUid) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.active = active;
        this.roles = roles;
        this.locked = locked;
        this.lastLoggedIn = lastLoggedIn;
        this.deletionNotificationSent = deletionNotificationSent;
        this.agencyTokenUid = agencyTokenUid;
    }

    @Override
    public String toString() {
        return "Identity{" +
                "id=" + id +
                ", uid='" + uid + '\'' +
                ", email=" + email +
                ", active=" + active +
                ", locked=" + locked +
                ", deletionNotificationSent=" + deletionNotificationSent +
                ", agencyTokenUid=" + agencyTokenUid +
                ", roles=" + roles +
                '}';
    }
}
