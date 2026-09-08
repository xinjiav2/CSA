package com.opencodingsociety.studytracker.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyRecordController.class)
@Import(com.opencodingsociety.studytracker.config.ApiSecurityConfig.class)
class StudyRecordControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean StudyRecordRepository repository;

    @Test
    void getAllReturnsJsonArray() throws Exception {
        when(repository.findAll()).thenReturn(List.of(validRecord()));
        mvc.perform(get("/api/study-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Unit 2"));
    }

    @Test
    void getMissingRecordReturns404() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/study-records/99")).andExpect(status().isNotFound());
    }

    @Test
    void validPostReturns201() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/study-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRecord())))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidPostReturns400() throws Exception {
        StudyRecord invalid = validRecord();
        invalid.setTopic("");
        mvc.perform(post("/api/study-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.topic").exists());
    }

    @Test
    void deleteMissingRecordReturns404() throws Exception {
        when(repository.existsById(99L)).thenReturn(false);
        mvc.perform(delete("/api/study-records/99")).andExpect(status().isNotFound());
    }

    private StudyRecord validRecord() {
        StudyRecord record = new StudyRecord();
        record.setTopic("Unit 2");
        record.setSubtopic("String methods");
        record.setStudentName("Zhengji Li");
        record.setStatus(StudyStatus.IN_PROGRESS);
        record.setMinutesStudied(30);
        record.setConfidence(3);
        return record;
    }
}
