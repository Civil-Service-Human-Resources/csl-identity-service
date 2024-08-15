package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Invite implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, length = 40, nullable = false)
    private String code;

    @Column(length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private InviteStatus status;

    @OneToOne()
    private Identity inviter;

    @Column(nullable = false)
    private LocalDateTime invitedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column(length = 150, nullable = false)
    private String forEmail;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "invite_role",
            joinColumns = @JoinColumn(name = "invite_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> forRoles;

    @Column
    private boolean isAuthorisedInvite = true;

    @Override
    public String toString() {
        return "Identity{" +
                "id=" + id +
                ", code=" + code +
                ", status=" + status +
                ", forEmail=" + forEmail +
                ", isAuthorisedInvite=" + isAuthorisedInvite +
                ", invitedAt=" + invitedAt +
                ", acceptedAt=" + acceptedAt +
                ", inviter=" + inviter +
                '}';
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
