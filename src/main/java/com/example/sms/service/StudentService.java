package com.example.sms.service;

import com.example.sms.dto.StudentDTO;
import com.example.sms.entity.Student;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ModelMapper modelMapper;

    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll(Sort.by("id")).stream()
                .map(this::toDTO)
                .toList();
    }

    public StudentDTO getStudentById(Long id) {
        return toDTO(findStudent(id));
    }

    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = modelMapper.map(studentDTO, Student.class);
        student.setId(null); // the database assigns the id
        return toDTO(studentRepository.save(student));
    }

    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student student = findStudent(id);
        student.setName(studentDTO.getName());
        student.setDepartment(studentDTO.getDepartment());
        return toDTO(studentRepository.save(student));
    }

    public void deleteStudent(Long id) {
        studentRepository.delete(findStudent(id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    private StudentDTO toDTO(Student student) {
        return modelMapper.map(student, StudentDTO.class);
    }
}
