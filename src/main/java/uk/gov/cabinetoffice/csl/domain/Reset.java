package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class Reset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, length = 40, nullable = false)
    private String code;

    @Column(length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private ResetStatus resetStatus;

    @Column(nullable = false)
    private Date requestedAt;

    @Column
    private Date resetAt;

    @Column(length = 150, nullable = false)
    private String email;
}
