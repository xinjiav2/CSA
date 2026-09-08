package com.opencodingsociety.studytracker.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "study_records")
public class StudyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String topic;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String subtopic;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String studentName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyStatus status = StudyStatus.PLANNED;

    @PositiveOrZero
    @Column(nullable = false)
    private int minutesStudied;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int confidence = 1;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
