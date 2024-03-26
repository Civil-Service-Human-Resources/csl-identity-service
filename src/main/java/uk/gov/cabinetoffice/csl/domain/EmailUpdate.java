package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.RandomStringUtils.random;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class EmailUpdate {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @Column
    private String code = random(40, true, true);

    @Column
    private String email;

    @ManyToOne
    private Identity identity;

    @Column
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private String newEmail;

    @Column(length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailUpdateStatus emailUpdateStatus;
}
