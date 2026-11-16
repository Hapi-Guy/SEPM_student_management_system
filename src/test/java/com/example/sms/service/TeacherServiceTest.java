package com.example.sms.service;

import com.example.sms.dto.TeacherDTO;
import com.example.sms.entity.Teacher;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests: the repository is a Mockito mock, so no Spring context or database is started.
 */
@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(teacherRepository, new ModelMapper());
    }

    // Listing teachers returns every teacher in id order, converted to DTOs
    @Test
    void getAllTeachers_mapsEveryTeacherToDto() {
        when(teacherRepository.findAll(Sort.by("id"))).thenReturn(List.of(
                new Teacher(1L, "Dr. Ayesha Rahman", "CSE"),
                new Teacher(2L, "Prof. Tanvir Ahmed", "EEE")));

        List<TeacherDTO> result = teacherService.getAllTeachers();

        assertThat(result).extracting(TeacherDTO::getName)
                .containsExactly("Dr. Ayesha Rahman", "Prof. Tanvir Ahmed");
    }

    // Looking up an unknown teacher id throws ResourceNotFoundException with a clear message
    @Test
    void getTeacherById_throwsNotFound_whenIdDoesNotExist() {
        when(teacherRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Teacher not found with id 42");
    }
}
