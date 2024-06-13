package com.example.SpringSecurity.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class StudentDto {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;
}
