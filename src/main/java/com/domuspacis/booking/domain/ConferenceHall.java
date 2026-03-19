package com.domuspacis.booking.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("CONF_HALL")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class ConferenceHall extends ServiceAsset {

    @Column(name = "hall_code", length = 30)
    private String hallCode;

    @Column(name = "projector_available")
    private Boolean projectorAvailable;

    @Column(name = "audio_system_available")
    private Boolean audioSystemAvailable;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_seating_layout", length = 20)
    private SeatingLayout maxSeatingLayout;

    public enum SeatingLayout { THEATRE, CLASSROOM, BOARDROOM, BANQUET }
}
