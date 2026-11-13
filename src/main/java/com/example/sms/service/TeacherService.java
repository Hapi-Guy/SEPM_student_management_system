package com.example.sms.service;

import com.example.sms.dto.TeacherDTO;
import com.example.sms.entity.Teacher;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final ModelMapper modelMapper;

    public List<TeacherDTO> getAllTeachers() {
        return teacherRepository.findAll(Sort.by("id")).stream()
                .map(this::toDTO)
                .toList();
    }

    public TeacherDTO getTeacherById(Long id) {
        return toDTO(findTeacher(id));
    }

    public TeacherDTO createTeacher(TeacherDTO teacherDTO) {
        Teacher teacher = modelMapper.map(teacherDTO, Teacher.class);
        teacher.setId(null); // the database assigns the id
        return toDTO(teacherRepository.save(teacher));
    }

    public TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO) {
        Teacher teacher = findTeacher(id);
        teacher.setName(teacherDTO.getName());
        teacher.setDepartment(teacherDTO.getDepartment());
        return toDTO(teacherRepository.save(teacher));
    }

    public void deleteTeacher(Long id) {
        teacherRepository.delete(findTeacher(id));
    }

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id " + id));
    }

    private TeacherDTO toDTO(Teacher teacher) {
        return modelMapper.map(teacher, TeacherDTO.class);
    }
}
