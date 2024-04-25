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
            log.error("Exception Occured! Message -> {} and Cause -> {}", e.getMostSpecificCause(), e.getMessage());
            respose = getResponseDTO(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Exception Occured! statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            respose = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respose, HttpStatus.valueOf(respose.getStatusCode()));
    }

    @Override
    public ResponseEntity<ResponseDTO> updateStudent(UUID id, StudentDto studentDto) {
        ResponseDTO response;

        try {
            var isAdmin = hasAdminRole(getUserRoles());
            if (isAdmin) {
                Student existingStudent = studentRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with Id " + id + "Does Not Exist"));
                existingStudent.setFirstName(studentDto.getFirstName());
                existingStudent.setLastName(studentDto.getLastName());
                existingStudent.setEmail(studentDto.getEmail());

                var record = studentRepository.save(existingStudent);
                log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.ACCEPTED, record);
                response = getResponseDTO("Record Updated Successfully", HttpStatus.ACCEPTED, record);
            } else {
                response = getResponseDTO("No Authorization to Update Student", HttpStatus.FORBIDDEN);
            }
        } catch (ResponseStatusException e) {
            log.error("Exception Occured! statusCode -> {} and Message -> {} and Reason -> {}", e.getStatusCode(), e.getMessage(), e.getReason());
            response = getResponseDTO(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (ObjectNotValidException e) {
            var message = String.join("\n", e.getErrorMessages());
            log.info("Exception Occured! Reason -> {}", message);
            response = getResponseDTO(message, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error Occured! statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }

//    @Override
//    public ResponseEntity<ResponseDTO> deleteStudent(UUID id, StudentDto studentDto) {
//        log.info("Inside Delete Student Method ::: Trying To Delete Student Per Given Params");
//        ResponseDTO response;
//
//        try {
//            boolean isAdmin = hasAdminRole(getUserRoles());
//            if (isAdmin) {
//                var res = studentRepository.findById(id)
//                        .orElseThrow(()
//                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with Id " + id + "Does Not Exist"));
//               studentRepository.deleteById(id);
//
//
//            }
//
//        } catch (ResponseStatusException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }

    public ResponseEntity<ResponseDTO> deleteStudent(UUID id) {
        log.info("Inside Delete Student Method ::: Trying To Delete Student Per Given Params");
        ResponseDTO response;

        try {
            boolean isAdmin = hasAdminRole(getUserRoles());
            if (isAdmin) {
                var existingStudent = studentRepository.findById(id);
                if (existingStudent.isPresent()) {
                    studentRepository.deleteById(id);
                }
                log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.OK, existingStudent);
                response = getResponseDTO("Student deleted successfully", HttpStatus.OK);
            }
            log.info("Not Authorized to Delete Student", HttpStatus.FORBIDDEN);
            response = getResponseDTO("Not Authorized to Delete Student", HttpStatus.FORBIDDEN);

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



//    @Override
//    public void deleteById(UUID id) {
//          studentRepository.deleteById(id);
//    }

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
