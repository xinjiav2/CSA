package com.opencodingsociety.studytracker.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    List<StudyRecord> findByStudentNameIgnoreCaseOrderByUpdatedAtDesc(String studentName);
    List<StudyRecord> findByStatusOrderByUpdatedAtDesc(StudyStatus status);
}
