package com.domuspacis.customer.domain;

import com.domuspacis.auth.domain.User;
import com.domuspacis.shared.domain.Address;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customer_user_id", columnList = "user_id"),
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_phone", columnList = "phone"),
        @Index(name = "idx_customer_fullname", columnList = "full_name")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Embedded
    private Address address;

    @Column(name = "segment", length = 50)
    private String segment;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
