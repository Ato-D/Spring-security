package com.example.SpringSecurity.student;

import com.example.SpringSecurity.student.dto.StudentDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.SpringSecurity.utility.AppUtils.*;

@Service
@AllArgsConstructor
public class StudentServiceImpl implements StudentService{

    private final StudentRepository studentRepository;
    @Override
    public List<StudentDto> findAll() {

        var roles = getUserRoles();

        boolean isAdmin = hasAdminRole(roles);
        boolean isStudent = hasRole(roles, List.of("STUDENT"));

        if (isAdmin) {
            var a = studentRepository.findAll();
            List<StudentDto> studentDto = a.stream()
                    .map(student -> mapToStudentDto(student))
                    .collect(Collectors.toList());

            return studentDto;


        }
        return null;
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
