package com.example.sms.config;

import com.example.sms.entity.AppUser;
import com.example.sms.entity.Role;
import com.example.sms.entity.Student;
import com.example.sms.entity.Teacher;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.TeacherRepository;
import com.example.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates one login account per role, plus some sample data, the first time the app starts
 * on an empty database.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    new AppUser(null, "admin", passwordEncoder.encode("admin123"), Role.ADMIN),
                    new AppUser(null, "teacher", passwordEncoder.encode("teacher123"), Role.TEACHER),
                    new AppUser(null, "student", passwordEncoder.encode("student123"), Role.STUDENT)));
        }

        if (studentRepository.count() == 0) {
            studentRepository.saveAll(List.of(
                    new Student(null, "Rahim Uddin", "CSE"),
                    new Student(null, "Karim Hasan", "EEE"),
                    new Student(null, "Nusrat Jahan", "BBA")));
        }

        if (teacherRepository.count() == 0) {
            teacherRepository.saveAll(List.of(
                    new Teacher(null, "Dr. Ayesha Rahman", "CSE"),
                    new Teacher(null, "Prof. Tanvir Ahmed", "EEE")));
        }
    }
}
