package com.domuspacis.booking.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("RETREAT")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class RetreatCenter extends ServiceAsset {

    @Column(name = "number_of_beds")
    private Integer numberOfBeds;

    @Column(name = "includes_chapel")
    private Boolean includesChapel;

    @Column(name = "includes_catering")
    private Boolean includesCatering;
}
