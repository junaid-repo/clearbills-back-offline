package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "global_search_index",
        indexes = @Index(name = "idx_source_lookup", columnList = "source_id, source_type", unique = true))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Integer sourceId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "search_text", columnDefinition = "TEXT")
    private String searchText;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "relative_url")
    private String relativeUrl;

    @Column(name = "source_isactive")
    private Integer sourceIsActive;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}