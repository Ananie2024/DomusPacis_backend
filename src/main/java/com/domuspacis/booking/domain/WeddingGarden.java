package com.domuspacis.booking.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("WEDDING_GARDEN")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class WeddingGarden extends ServiceAsset {

    @Column(name = "is_indoor")
    private Boolean isIndoor;

    @Column(name = "has_stage")
    private Boolean hasStage;

    @ElementCollection
    @CollectionTable(name = "wedding_decoration_packages",
                     joinColumns = @JoinColumn(name = "garden_id"))
    @Column(name = "package_name")
    @Builder.Default
    private List<String> decorationPackages = new ArrayList<>();
}
