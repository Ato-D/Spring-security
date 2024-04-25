package com.example.SpringSecurity.student;

import com.example.SpringSecurity.dto.ResponseDTO;
import com.example.SpringSecurity.student.dto.StudentDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.SpringSecurity.utility.AppUtils.*;

@Service
@AllArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService{

    private final StudentRepository studentRepository;
    @Override
    public ResponseEntity<ResponseDTO> findAllStudents(Map<String, String> params) {

        log.info("Inside find All Students :::: Trying to fetch students per given pagination params");

        ResponseDTO response = new ResponseDTO();
        try {
            var roles = getUserRoles();
            boolean isAdmin = hasAdminRole(roles);
            boolean isStudent = hasRole(roles, List.of("STUDENT"));

            if (params == null || params.getOrDefault("paginate", "false").equalsIgnoreCase("false")){
                List<Student> students;
                if (isAdmin) {
                    students = studentRepository.findAll();
                } else {
                    log.info("Unauthorized access! statusCode -> {} and Cause -> {} and Message -> {}", HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, "Unauthorized access");
                    return new ResponseEntity<>(getResponseDTO("No authorization to view students", HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
                }
                if (!students.isEmpty()) {
                    List<StudentDto> studentDtos = students.stream()
                            .map(this::mapToStudentDto)
                            .collect(Collectors.toList());
                    return new ResponseEntity<>(getResponseDTO("Successfully Retrieved All Records", HttpStatus.OK, studentDtos), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(getResponseDTO("No students found", HttpStatus.NO_CONTENT), HttpStatus.NO_CONTENT);
            }
        }
        } catch (ResponseStatusException e) {
            log.error("Exception Occured! and Message -> {} and Cause -> {}", e.getMessage(), e.getReason());
            response = getResponseDTO(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Exception Occured! StatusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));

    }

        public Student findById(UUID id) {
        var res = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Student not found with ID: " + id));
        return res;
    }

    @Override
    public void saveStudent(StudentDto studentDto) {
        Student student = Student.builder()
                .firstName(studentDto.getFirstName())
                .lastName(studentDto.getLastName())
                .email(studentDto.getEmail())
                .build();
        studentRepository.save(student);
    }

    @Override
    public Student updateStudent(StudentDto studentDto) {
        var res = studentRepository.findById(studentDto.getId());
        if (res.isPresent()) {
            Student student = res.get();
            student.setFirstName(studentDto.getFirstName());
            student.setLastName(studentDto.getLastName());
            student.setEmail(studentDto.getEmail());
            return studentRepository.save(student);
        } else {
            throw new IllegalArgumentException("Student not found with ID: " + studentDto.getId());
        }
    }


    @Override
    public void deleteById(UUID id) {
          studentRepository.deleteById(id);
    }

    private StudentDto mapToStudentDto(Student student) {

        StudentDto studentDto = StudentDto.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .build();

        return studentDto;
    }
}
