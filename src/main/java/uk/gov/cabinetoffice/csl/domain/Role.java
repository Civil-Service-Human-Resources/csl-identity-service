package uk.gov.cabinetoffice.csl.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, unique = true, nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "roles")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Identity> identities;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "roles")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Identity> invites;

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
