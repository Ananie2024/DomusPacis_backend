package com.domuspacis.testmonials.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@Entity
@SuperBuilder
@Table(name = "testimonials")
public class Testimonial extends BaseEntity {

    private String quote;
    private String authorName;
    private String authorRole;

    private boolean approved;

}