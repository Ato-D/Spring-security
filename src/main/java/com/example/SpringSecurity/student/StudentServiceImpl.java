package com.example.SpringSecurity.student;

import com.example.SpringSecurity.dto.ResponseDTO;
import com.example.SpringSecurity.student.dto.StudentDto;
import com.example.SpringSecurity.utility.ObjectNotValidException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

            if (params == null || params.getOrDefault("paginate", "false").equalsIgnoreCase("false")) {
                List<Student> students;
                    students = studentRepository.findAll();
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
    public ResponseEntity<ResponseDTO> saveStudent(StudentDto studentDto) {
        log.info("Inside the Save Student method");
        ResponseDTO respose;

        try {

            boolean isAdmin = hasAdminRole(getUserRoles());
            if (isAdmin) {
                var student = Student.builder()
                        .id(studentDto.getId())
                        .firstName(studentDto.getFirstName())
                        .lastName(studentDto.getLastName())
                        .email(studentDto.getEmail())
                        .build();
                var record = studentRepository.save(student);
                log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.CREATED, record);
                respose = getResponseDTO("Record Saved Successfully", HttpStatus.OK, record);
            } else {
                respose = getResponseDTO("No authorization to create student", HttpStatus.FORBIDDEN);
            }

        } catch (ResponseStatusException e) {
            log.error("Error Occured! statusCode -> {}, Message -> {}, Reason -> {}", e.getStatusCode(), e.getMessage(), e.getReason());
            respose = getResponseDTO(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (ObjectNotValidException e) {
            var message = String.join("\n", e.getErrorMessages());
            log.info("Exception Occured! Reason -> {}", message);
            respose = getResponseDTO(message, HttpStatus.BAD_REQUEST);

        } catch (DataIntegrityViolationException e) {
            log.error("Exception Occured! Message -> {} and Cause -> {}", e.getMostSpecificCause(), e.getCause());
            respose = getResponseDTO(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Exception Occured! statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            respose = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respose, HttpStatus.valueOf(respose.getStatusCode()));
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
