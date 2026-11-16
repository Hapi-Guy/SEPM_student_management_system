package com.example.sms.integration;

import com.example.sms.entity.Student;
import com.example.sms.entity.Teacher;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.TeacherRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the whole application starts (security, controllers, services, JPA,
 * error handling, demo data) against an in-memory H2 database. Each test walks through
 * everything one kind of user can and cannot do, so together they cover every endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    // A fresh student and teacher for each test, so tests don't depend on each other's changes
    private Long studentId;
    private Long teacherId;

    @BeforeEach
    void setUp() {
        studentId = studentRepository.save(new Student(null, "Test Student", "CSE")).getId();
        teacherId = teacherRepository.save(new Teacher(null, "Test Teacher", "EEE")).getId();
    }

    // ADMIN: full add -> view -> edit -> validate -> delete -> 404 lifecycle for students and teachers
    @Test
    void admin_canManageStudentsAndTeachersFromStartToFinish() throws Exception {
        RequestPostProcessor admin = httpBasic("admin", "admin123");

        mockMvc.perform(get("/api/auth/me").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // ---- students ----
        Long newStudentId = createAndReturnId(post("/api/students").with(admin), "Sadia Islam", "CSE");

        mockMvc.perform(get("/api/students/" + newStudentId).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sadia Islam"));
        mockMvc.perform(get("/api/students").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Sadia Islam")));

        mockMvc.perform(json(put("/api/students/" + newStudentId).with(admin), "Sadia Rahman", "EEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sadia Rahman"))
                .andExpect(jsonPath("$.department").value("EEE"));

        mockMvc.perform(json(post("/api/students").with(admin), "", "CSE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("name")));

        mockMvc.perform(delete("/api/students/" + newStudentId).with(admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/students/" + newStudentId).with(admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id " + newStudentId));
        mockMvc.perform(get("/api/students").with(admin))
                .andExpect(jsonPath("$[*].name", not(hasItem("Sadia Rahman"))));

        // ---- teachers ----
        Long newTeacherId = createAndReturnId(post("/api/teachers").with(admin), "Dr. Farhana Akter", "BBA");

        mockMvc.perform(get("/api/teachers/" + newTeacherId).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Farhana Akter"));
        mockMvc.perform(get("/api/teachers").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Dr. Farhana Akter")));

        mockMvc.perform(json(put("/api/teachers/" + newTeacherId).with(admin), "Dr. Farhana Akter", "CSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("CSE"));

        mockMvc.perform(json(post("/api/teachers").with(admin), "Dr. No Department", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("department")));

        mockMvc.perform(delete("/api/teachers/" + newTeacherId).with(admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/teachers/" + newTeacherId).with(admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Teacher not found with id " + newTeacherId));
    }

    // TEACHER: can view and edit students and teachers, but every add or remove is refused
    @Test
    void teacher_canViewAndEditEverything_butCannotAddOrRemove() throws Exception {
        RequestPostProcessor teacher = httpBasic("teacher", "teacher123");

        mockMvc.perform(get("/api/auth/me").with(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        // view
        mockMvc.perform(get("/api/students").with(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/students/" + studentId).with(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Student"));
        mockMvc.perform(get("/api/teachers").with(teacher))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/" + teacherId).with(teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Teacher"));

        // edit, and the change is really saved
        mockMvc.perform(json(put("/api/students/" + studentId).with(teacher), "Edited Student", "BBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Edited Student"))
                .andExpect(jsonPath("$.department").value("BBA"));
        mockMvc.perform(get("/api/students/" + studentId).with(teacher))
                .andExpect(jsonPath("$.department").value("BBA"));
        mockMvc.perform(json(put("/api/teachers/" + teacherId).with(teacher), "Edited Teacher", "CSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Edited Teacher"));

        // edits are still validated and still 404 for unknown ids
        mockMvc.perform(json(put("/api/students/" + studentId).with(teacher), "", "CSE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(put("/api/teachers/999999").with(teacher), "Nobody", "CSE"))
                .andExpect(status().isNotFound());

        // add and remove are admin-only
        mockMvc.perform(json(post("/api/students").with(teacher), "New Student", "CSE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/teachers").with(teacher), "New Teacher", "CSE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/" + studentId).with(teacher))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/teachers/" + teacherId).with(teacher))
                .andExpect(status().isForbidden());

        // nothing was removed
        mockMvc.perform(get("/api/students/" + studentId).with(teacher)).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/" + teacherId).with(teacher)).andExpect(status().isOk());
    }

    // STUDENT and anonymous visitors: public frontend, login errors, and read-only access to students
    @Test
    void studentAndAnonymousUser_onlyGetWhatTheyAreAllowed() throws Exception {
        RequestPostProcessor student = httpBasic("student", "student123");

        // the frontend files are public
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/style.css")).andExpect(status().isOk());

        // the API is not: no login, wrong password and unknown user all get 401,
        // without the "WWW-Authenticate: Basic" header that would make the browser show a popup
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mockMvc.perform(get("/api/students").with(httpBasic("student", "wrong-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/students").with(httpBasic("nobody", "student123")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").with(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // a student can view students, including the demo data from DataInitializer
        mockMvc.perform(get("/api/students").with(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Rahim Uddin")));
        mockMvc.perform(get("/api/students/" + studentId).with(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Student"));

        // ...but cannot view teachers
        mockMvc.perform(get("/api/teachers").with(student)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers/" + teacherId).with(student)).andExpect(status().isForbidden());

        // ...and cannot change anything
        mockMvc.perform(json(put("/api/students/" + studentId).with(student), "Hacked", "CSE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/students").with(student), "New Student", "CSE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/" + studentId).with(student))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(put("/api/teachers/" + teacherId).with(student), "Hacked", "CSE"))
                .andExpect(status().isForbidden());

        // the refused edit really did nothing
        mockMvc.perform(get("/api/students/" + studentId).with(student))
                .andExpect(jsonPath("$.name").value("Test Student"));
    }

    // Adds a JSON body {"name": ..., "department": ...} to a request
    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String name, String department) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\", \"department\": \"" + department + "\"}");
    }

    // Sends a create request, checks it returned 201, and returns the new record's id
    private Long createAndReturnId(MockHttpServletRequestBuilder request, String name, String department) throws Exception {
        String body = mockMvc.perform(json(request, name, department))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.department").value(department))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
