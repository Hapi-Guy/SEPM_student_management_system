package com.example.sms.service;

import com.example.sms.dto.StudentDTO;
import com.example.sms.entity.Student;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests: the repository is a Mockito mock, so no Spring context or database is started.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, new ModelMapper());
    }

    // Looking up an existing id returns that student's data as a DTO
    @Test
    void getStudentById_returnsStudent_whenIdExists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student(1L, "Rahim Uddin", "CSE")));

        StudentDTO result = studentService.getStudentById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Rahim Uddin");
        assertThat(result.getDepartment()).isEqualTo("CSE");
    }

    // A client-supplied id is discarded so the database always assigns a new one
    @Test
    void createStudent_ignoresIdSentByClient() {
        // like the real database: return a copy with a generated id
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> {
            Student toSave = call.getArgument(0);
            return new Student(10L, toSave.getName(), toSave.getDepartment());
        });

        StudentDTO result = studentService.createStudent(new StudentDTO(999L, "Sadia Islam", "CSE"));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).as("id sent to the database").isNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    // Updating copies both the new name and the new department onto the student
    @Test
    void updateStudent_changesNameAndDepartment() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student(1L, "Rahim Uddin", "CSE")));
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));

        StudentDTO result = studentService.updateStudent(1L, new StudentDTO(null, "Rahim Hossain", "EEE"));

        assertThat(result.getName()).isEqualTo("Rahim Hossain");
        assertThat(result.getDepartment()).isEqualTo("EEE");
    }

    // Deleting an unknown id throws 404-style ResourceNotFoundException and deletes nothing
    @Test
    void deleteStudent_throwsNotFound_whenIdDoesNotExist() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deleteStudent(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student not found with id 99");
        verify(studentRepository, never()).delete(any(Student.class));
    }
}
