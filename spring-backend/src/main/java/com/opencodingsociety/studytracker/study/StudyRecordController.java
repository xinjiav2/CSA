package com.opencodingsociety.studytracker.study;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/study-records")
@CrossOrigin(origins = {"http://localhost:4000", "http://127.0.0.1:4000", "https://xinjiav2.github.io"})
public class StudyRecordController {
    private final StudyRecordRepository repository;

    public StudyRecordController(StudyRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<StudyRecord> getAll(@RequestParam(required = false) String studentName,
                                    @RequestParam(required = false) StudyStatus status) {
        if (studentName != null && !studentName.isBlank()) {
            return repository.findByStudentNameIgnoreCaseOrderByUpdatedAtDesc(studentName);
        }
        if (status != null) {
            return repository.findByStatusOrderByUpdatedAtDesc(status);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyRecord> getOne(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StudyRecord> create(@Valid @RequestBody StudyRecord incoming) {
        incoming.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(incoming));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudyRecord> update(@PathVariable Long id,
                                               @Valid @RequestBody StudyRecord incoming) {
        return repository.findById(id).map(existing -> {
            existing.setTopic(incoming.getTopic());
            existing.setSubtopic(incoming.getSubtopic());
            existing.setStudentName(incoming.getStudentName());
            existing.setStatus(incoming.getStatus());
            existing.setMinutesStudied(incoming.getMinutesStudied());
            existing.setConfidence(incoming.getConfidence());
            existing.setNotes(incoming.getNotes());
            return ResponseEntity.ok(repository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
