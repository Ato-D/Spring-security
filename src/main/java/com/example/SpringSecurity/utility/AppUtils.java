package com.example.SpringSecurity.utility;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class containing various methods for common tasks.
 *
 * @author Derrick Donkoh
 * @createdAt 23rd June, 2024
 */
@AllArgsConstructor
@Slf4j
public class AppUtils {

    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_PAGE_SORT = "createdAt";
    public static final String DEFAULT_PAGE_SORT_DIR = "desc";

    /**
     * This method is used to generate a pageable to make a paginated request
     * @param params This is a Map that has the page number, size, sortBy and sortDir for the pagination
     * @return
     */
    public static Pageable getPageRequest(Map params){
        if(!isNotNullOrEmpty(params)){
            params = new HashMap();
        }
        Sort sort = Sort.by(Sort.Direction.fromString(params.getOrDefault("sortDir", AppUtils.DEFAULT_PAGE_SORT_DIR).toString()),
                params.getOrDefault("sortBy", AppUtils.DEFAULT_PAGE_SORT).toString());

        Integer pageNo = getParamToInteger(params, AppUtils.DEFAULT_PAGE_NUMBER, "page");
        Integer pageSize = getParamToInteger(params, AppUtils.DEFAULT_PAGE_SIZE, "size");

        PageRequest page = PageRequest.of(  pageNo - 1, pageSize, sort);

        return page;
    }

    /**
     * This method maps a page of a model model to a Pagination object.
     *
     * @param page The Page object containing the IssueTypeModel entities.
     * @return A Pagination object containing the mapped entities, page information, and total element count.
     *         Returns null if the input Page is null.
     */
//    public static <T> Pagination mapToPagination(Page<T> page) {
//        if (page == null) {
//            return null;
//        }
//        return new Pagination(page.getContent(), page.getPageable(), (int) page.getTotalElements());
//    }

    /**
     * This  method is used to fetch an integer value from a Map with its default value
     * @param params The Map object
     * @param fieldName The name of the intended field
     * @param defaultVal The default value for the field to be extracted
     * @return Integer
     */
    public static Integer getParamToInteger(Map params, Integer defaultVal, String fieldName){
        Integer value = defaultVal;
        if(params != null && fieldName != null && params.get(fieldName) != null){
            try{
                var page2 = Integer.parseInt(params.get(fieldName).toString());
                if(page2 > 0){
                    value = page2;
                }
            }catch(Exception e){
                System.out.println("Invalid " + fieldName + " number");
            }
        }
        return value;
    }

    public static ZonedDateTime parseZoneDateTime(String date){
        if(date == null || date.equalsIgnoreCase("")){
            return null;
        }
        /**
         * Calculating if the Date was ZonedDateTime
         */
        try {
            ZonedDateTime parsedZone = ZonedDateTime.parse(date);
            return parsedZone;
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        /**
         * Calculating if the Date was LocalDateTime
         */
        try {
            LocalDateTime parsedDateTime = LocalDateTime.parse(date);
            return parsedDateTime.atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        /**
         * Calculating if the Date was LocalDate
         */
        try {
            LocalDate parsedLocal = LocalDate.parse(date);
            return parsedLocal.atStartOfDay(ZoneId.systemDefault());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return null;
    }

    /**
     * This method is used to split and transform T, the object in question, string separated by commas into List<UUID>
     * @param data
     * @return  List<T>
     */
    public static <T> List<T> getListFromString(String data, Function<String, T> mapper) {
        try {
            if (data == null || data.trim().isEmpty()) {
                return new ArrayList<>();
            }
            String[] values = data.split(",");
            return Arrays.stream(values)
                    .map(mapper)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * This method is used to generate a ResponseDTO with message, status, and data.
     *
     * @param message The message for the response.
     * @param status The HTTP status of the response.
     * @param data The data to be included in the response.
     * @return ResponseDTO object.
     */
//    public static ResponseDTO getResponseDTO(String message, HttpStatus status, Object data){
//        if(data == null){
//            ResponseDTO responseDTO = getResponseDTO(message, status);
//            return responseDTO;
//        }
//        ResponseDTO responseDTO = ResponseDTO.builder()
//                .message(message)
//                .statusCode(status.value())
//                .data(data)
//                .date(ZonedDateTime.now()).build();
//        return responseDTO;
//
//    }

    /**
     * This method is used to generate a ResponseDTO with message and status.
     *
     * @param message The message for the response.
     * @param status The HTTP status of the response.
     * @return ResponseDTO object.
     */
//    public static ResponseDTO getResponseDTO(String message, HttpStatus status){
//
//        ResponseDTO responseDTO = ResponseDTO.builder()
//                .message(message)
//                .statusCode(status.value())
//                .date(ZonedDateTime.now()).build();
//        return responseDTO;
//    }

    /**
     * This method is used to fetch the Client Roles from a Jwt
     * @param principal
     * @return List of client roles
     * @modified    Extracted a part to make a generic method to return roles from object
     * @modifiedBy  Ebenezer N.
     * @modifiedAt  1st Sept 2023
     */
    public static List<String> getClientRoles(Jwt principal, String clientName){
        if(!AppUtils.isNotNullOrEmpty(clientName)){
            clientName = "biggest-api";
        }
        Object resourceAccess = principal.getClaims().get("resource_access");
        if(AppUtils.isNotNullOrEmpty(resourceAccess)){
            Object clientMap = ((Map<String, Object>) resourceAccess).get(clientName);
            return getRolesFromObject(clientMap);
        }
        return new ArrayList<>();
    }

    /**
     * This method checks if an object is not null or an empty string.
     *
     * @param data The object to be checked.
     * @return True if the object is not null or an empty string, otherwise false.
     */
    public static Boolean isNotNullOrEmpty(Object data){
        if(data == null){
            return false;
        }
        return !data.toString().trim().equalsIgnoreCase("");
    }


    /**
     * This method is used to fetch the Realm Roles from a Jwt
     * @param principal
     * @return List of realm roles
     * @author Prince Amofah
     * @createdAt 1st Sept 2023
     * @modified
     * @modifiedBy
     * @modifiedAt
     */
    public static List<String> getRealmRoles(Jwt principal){
        Object realmAccess = principal.getClaims().get("realm_access");
        if(AppUtils.isNotNullOrEmpty(realmAccess)){
            return getRolesFromObject(realmAccess);
        }
        return new ArrayList<>();
    }

    /**
     * This method is used to fetch Roles from an Object by structuring it as a Map<String, Object>
     * @param data
     * @return List of roles
     */
    public static List<String> getRolesFromObject(Object data){
        if(AppUtils.isNotNullOrEmpty(data)){
            Object roleList = ((Map<String, Object>  ) data).get("roles");
            if(AppUtils.isNotNullOrEmpty(roleList)){
                return  (List<String>) roleList;
            }
        }
        return new ArrayList<>();
    }

    /**
     * This is used to fetch a value from a Map
     * @param data  Map
     * @return
     * @author Prince Amofah
     * @createdAt
     * @modified
     * @modifiedBy
     * @modifiedAt
     */
    public static Object getNameFromMap(Map data) {
        if(AppUtils.isNotNullOrEmpty(data)){
            return data.getOrDefault("name", null);
        }
        return null;
    }

    /**
     * This method is used to convert a camelcase String to snakeCase
     * @param value
     * @return
     * @author Prince Amofah
     * @createdAt 14th July 2023
     * @modified
     * @modifiedBy
     * @modifiedAt
     */
    public static String transformToSnake(String value){
        String str = "";
        var charList = value.toCharArray();
        for(int i=0; i < charList.length; i++){
            if(Character.isUpperCase(charList[i])){
                str += "_";
            }
            str += Character.toLowerCase(charList[i]);
        }
        return str;
    }

    /**
     /**
     * Retrieves the authenticated user's UUID.
     *
     * @return The UUID of the authenticated user, or null if not authenticated.
     */

    public static UUID getAuthenticatedUserId() {
        try {
            if (authentication().isAuthenticated()){
                return UUID.fromString(authentication().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method extracts the logged-in user's email from the Security Context
     * @return String
     * @author Prince Amofah
     * @createdAt 1st Oct 2023
     */
    public static String getAuthenticatedUserEmail(){
        try{
            log.info("inside getAuthenticatedUserEmail");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(isNotNullOrEmpty(authentication) &&
                    authentication.isAuthenticated() &&
                    isNotNullOrEmpty(authentication.getPrincipal())){
                var claims = ((Jwt) authentication.getPrincipal()).getClaims();
                if(isNotNullOrEmpty(claims)){
                    var email = claims.getOrDefault("email", null);
                    if(isNotNullOrEmpty(email)){
                        return email.toString();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static String getAuthenticatedUserFirstName(){
        try{
            log.info("inside getAuthenticatedUserName");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(isNotNullOrEmpty(authentication) &&
                    authentication.isAuthenticated() &&
                    isNotNullOrEmpty(authentication.getPrincipal())){
                var claims = ((Jwt) authentication.getPrincipal()).getClaims();
                if(isNotNullOrEmpty(claims)){
                    var firstName = claims.getOrDefault("family_name", null);
                    if(isNotNullOrEmpty(firstName)){
                        return firstName.toString();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Retrieves the roles granted to the authenticated user.
     *
     * @return A collection of GrantedAuthority objects representing the roles granted to the user,
     *         or an empty collection if the user is not authenticated or no roles are granted.
     */

    public static Collection<? extends GrantedAuthority> getUserRoles() {
        try {
            if (authentication().isAuthenticated()) {
                return authentication().getAuthorities();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    /**
     * Checks if the given collection of roles contains the admin role.
     *
     * @param roles A collection of GrantedAuthority objects representing the roles assigned to the user.
     * @return true if the collection contains the admin role, false otherwise.
     */

    public static boolean hasAdminRole(Collection<? extends GrantedAuthority> roles) {
        return roles.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.contains("ADMIN"));
    }

    public static boolean hasProjectManagerRole(Collection<? extends  GrantedAuthority> roles) {
        return roles.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.contains("PROJECT_MANAGER"));
    }


    /**
     * Checks if the given collection of roles contains the project manager role.
     *
     * @param roles A collection of GrantedAuthority objects representing the roles assigned to the user.

     * Use Java Stream API to map each GrantedAuthority object to its authority string
     * Then check if any of the authority strings contain "PROJECT_MANAGER"

     * @return true if the collection contains the project manager role, false otherwise.
     */

    public static boolean hasRole(Collection<? extends  GrantedAuthority> roles,List<String> str){
        return roles.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> str.stream().anyMatch(role::contains));
    }


    /**
     * Retrieves the Authentication object representing the current security context.
     * Retrieve the Authentication object from the SecurityContextHolder
     * @return The Authentication object representing the current security context.
     */


    public static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Helper method to get the name from a specific model by ID.
     *
     * @param id          The ID of the model.
     * @param entityClass The class of the model.
     * @param repository  The repository of the model.
     * @param <T>         The type of the model.
     * @return The name or null if not found.
     */
    public static <T> String getName(UUID id, Class<T> entityClass, JpaRepository<T,UUID> repository) {
        Optional<T> record = repository.findById(id);
        if (record.isPresent()) {
            try {
                Method getName = entityClass.getMethod("getName");
                return (String) getName.invoke(record.get());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
