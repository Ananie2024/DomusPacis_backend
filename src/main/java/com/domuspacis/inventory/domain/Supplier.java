package com.domuspacis.inventory.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "suppliers", indexes = {
    @Index(name = "idx_supplier_name",  columnList = "name"),
    @Index(name = "idx_supplier_email", columnList = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier extends BaseEntity {
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Column(name = "contact_person", length = 255) private String contactPerson;
    @Column(name = "phone", length = 50) private String phone;
    @Column(name = "email", length = 255) private String email;
    @Column(name = "address", columnDefinition = "TEXT") private String address;
    @Column(name = "tax_identification_number", length = 50) private String taxIdentificationNumber;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
