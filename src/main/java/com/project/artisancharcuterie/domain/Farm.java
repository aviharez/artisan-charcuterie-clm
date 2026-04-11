package com.project.artisancharcuterie.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered farm of origin.
 * Every batch must reference a valid farm, a core data-integrity constraint.
 */
@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false, length = 100)
    private String breed;

    @Column(name = "contact_info", length = 200)
    private String contactInfo;

    @Column(length = 300)
    private String certifications;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "farm", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Batch> batches = new ArrayList<>();

}
