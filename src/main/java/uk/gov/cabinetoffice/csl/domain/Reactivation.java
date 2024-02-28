package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Reactivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, length = 40, nullable = false)
    private String code;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReactivationStatus reactivationStatus;

    @Column(nullable = false)
    private Date requestedAt;

    private Date reactivatedAt;

    @Column(length = 150, nullable = false)
    private String email;

    public Reactivation(String code, ReactivationStatus reactivationStatus, Date requestedAt, String email) {
        this.code = code;
        this.reactivationStatus = reactivationStatus;
        this.requestedAt = requestedAt;
        this.email = email;
    }
}
