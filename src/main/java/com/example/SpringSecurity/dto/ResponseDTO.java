package com.example.SpringSecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

/**
 * This is a DTO class for the response model
 *
 *@author Derrick DOnkoh
 *@createdAt 24th April 2023
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDTO {

    private int statusCode;
    private String message;
    private Object data;
    private ZonedDateTime date;

    public ResponseDTO(ResponseDTO responseDTO, HttpStatus badRequest){

    }



}
