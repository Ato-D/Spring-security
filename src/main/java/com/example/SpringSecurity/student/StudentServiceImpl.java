package com.example.SpringSecurity.student;

import com.example.SpringSecurity.dto.ResponseDTO;
import com.example.SpringSecurity.student.dto.StudentDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

            if (params == null || params.getOrDefault("paginate", "false").equalsIgnoreCase("false")) {
                List<Student> students;
                if (isAdmin) {
                    students = studentRepository.findAll();
                } else {
                    log.info("Unauthorized access! statusCode -> {} and Cause -> {} and Message -> {}", HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, "Unauthorized access");
                    return new ResponseEntity<>(getResponseDTO("No authorization to view students", HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
                }
                if (!students.isEmpty()) {
                    log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.OK, students);
                    List<StudentDto> studentDtos = students.stream()
                            .map(this::mapToStudentDto)
                            .collect(Collectors.toList());
                    response = getResponseDTO("Successfully retrieved all students", HttpStatus.OK, studentDtos);
                    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
                } else {
                    response = getResponseDTO("No record found", HttpStatus.NOT_FOUND);
                    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
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

    @Override
    public ResponseEntity<ResponseDTO> findById(UUID id) {
        log.info("Inside find Find Student by Id ::: Trying to find student type id -> {}", id);
        ResponseDTO response;
        try {

            var roles = getUserRoles();
            boolean isAdmin = hasAdminRole(roles);
            boolean isStudent = hasRole(roles, List.of("STUDENT"));



            if (isAdmin) {
                var res = studentRepository.findById(id);
                if (res.isPresent()) {
                    log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.OK, res);
                    response = getResponseDTO("Successfully retreived the student with id " + id, HttpStatus.OK, res);
                    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
                }
                log.info("No record found! statusCode -> {} and Message -> {}", HttpStatus.NOT_FOUND, res);
                response = (getResponseDTO("Not Found!", HttpStatus.NOT_FOUND));
                return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));

            }
            else {
                log.info("Unauthorized access! statusCode -> {} and Cause -> {} and Message -> {}", HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, "Unauthorized access");
                return new ResponseEntity<>(getResponseDTO("No authorization to view students", HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
            }
        }
        catch (ResponseStatusException e) {
            log.error("Exception Occured! Reason -> {} and Message -> {}", e.getCause(), e.getReason());
            response = getResponseDTO(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Exception Occured! statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
          return new ResponseEntity<>(response,HttpStatusCode.valueOf(response.getStatusCode()));
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
