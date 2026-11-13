package com.example.sms.controller;

import com.example.sms.dto.TeacherDTO;
import com.example.sms.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Who can call which endpoint is configured in {@link com.example.sms.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public List<TeacherDTO> getAllTeachers() {
        return teacherService.getAllTeachers();
    }

    @GetMapping("/{id}")
    public TeacherDTO getTeacher(@PathVariable Long id) {
        return teacherService.getTeacherById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDTO createTeacher(@Valid @RequestBody TeacherDTO teacherDTO) {
        return teacherService.createTeacher(teacherDTO);
    }

    @PutMapping("/{id}")
    public TeacherDTO updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherDTO teacherDTO) {
        return teacherService.updateTeacher(id, teacherDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }
}
