package uk.gov.cabinetoffice.csl.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.RandomStringUtils.random;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class EmailUpdate {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    private String code = random(40, true, true);

    private String email;

    @ManyToOne
    private Identity identity;

    private Instant timestamp;
}
