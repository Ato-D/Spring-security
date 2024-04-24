package com.stlghana.tbms.uchiha.service.serviceImpl;

import com.stlghana.tbms.uchiha.config.NotificationProperties;
import com.stlghana.tbms.uchiha.dto.*;
import com.stlghana.tbms.uchiha.model.*;
import com.stlghana.tbms.uchiha.repository.JDBCTemplateQueries;
import com.stlghana.tbms.uchiha.repository.JobCardRepository;
import com.stlghana.tbms.uchiha.repository.JobCardTableRepository;
import com.stlghana.tbms.uchiha.service.JobCardService;
import com.stlghana.tbms.uchiha.service.NotificationService;
import com.stlghana.tbms.uchiha.service.serviceImpl.kafka.KafkaProducerService;
import com.stlghana.tbms.uchiha.utility.ObjectNotValidException;
import com.stlghana.tbms.uchiha.utility.Pagination;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.stlghana.tbms.uchiha.utility.AppUtils.*;

/**
 * This class represents the implementation of JobCardService, providing methods for jobCard related operations.
 *
 * @Service Indicates that this class is a service component in the Spring application context.
 * @AllArgsConstructor Lombok's annotation to generate a constructor with all fields for dependency injection.
 * @Slf4j Lombok's annotation to generate a logger field for this class.
 * @Author Prince Amofah
 * @CreatedAt 6th October 2023
 * @Modified [Modified Details]
 * @ModifiedAt [Modification Date]
 * @ModifiedBy [Modifier Name]
 */
@Service
@AllArgsConstructor
@Slf4j
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository jobCardRepository;
    private final JobCardTableRepository jobCardTableRepository;
    private final ModelMapper modelMapper;
    private final JDBCTemplateQueries jdbcTemplateQueries;
    private final KafkaProducerService kafkaProducerService;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    /**
     * Retrieves paginated jobCards based on optional parameters and roles.
     *
     * @param params            Optional parameters for pagination.
     * @return                  A ResponseEntity containing the response data.
     */
    @Override
    public ResponseEntity<ResponseDTO> findAllJobCards(Map<String, String> params) {
        log.info("Inside find All JobCards :::::: Trying to fetch jobCards per given pagination params");
        ResponseDTO response;
        try {
            var roles = getUserRoles();
            boolean isProjectManager = hasProjectManagerRole(roles);
            boolean isAdmin = hasAdminRole(roles);
            boolean isVulcanizer = hasRole(roles,List.of("VULCANIZER"));
            boolean isWorkShopManager = hasRole(roles,List.of("WORKSHOP_MANAGER"));

            String searchValue = params != null ? params.getOrDefault("search","")
                    : "";

            String projects = params != null ? params.getOrDefault("projects","")
                    : "";

            List<UUID> projectIds = new ArrayList<>();

            if (!projects.isEmpty()){
                String[] uuidStrings = projects.split(",");
                for (String project : uuidStrings) {
                    projectIds.add(UUID.fromString(project));
                }
            }
            if (params == null || params.getOrDefault("paginate","false").equalsIgnoreCase("false")){

                List<JobCardTable> jobCardTableList;
                if (isAdmin || isWorkShopManager) {
                    jobCardTableList = jobCardTableRepository.findByNameContaining(searchValue);
                } else if (isProjectManager) {
                    jobCardTableList = jobCardTableRepository.findAllByProjectIds(searchValue,projectIds);
                } else if (isVulcanizer) {
                    jobCardTableList = jobCardTableRepository.findAllByVulcanizerId(getAuthenticatedUserId());
                } else {
                    log.info("Unauthorized access! statusCode -> {} and Cause -> {} and Message -> {}", HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, "Unauthorized access");
                    return new ResponseEntity<>(getResponseDTO("No authorization to view jobcards", HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
                }

                if (!jobCardTableList.isEmpty()) {
                    log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.OK, jobCardTableList);
                    return new ResponseEntity<>(getResponseDTO("Successfully retrieved all records", HttpStatus.OK, jobCardTableList), HttpStatus.OK);
                }

                log.info("Not Found! statusCode -> {} and Cause -> {} and Message -> {}", 204, HttpStatus.NO_CONTENT, "Records Not Found");
                return new ResponseEntity<>(getResponseDTO("No record found", HttpStatus.NO_CONTENT), HttpStatus.NO_CONTENT);

            } else {

                Pageable pageable = getPageRequest(params);
                Page<JobCardTable> res;

                if (isAdmin || isWorkShopManager || isProjectManager ||isVulcanizer) {
                    if (isAdmin || isWorkShopManager) {
                        res = jobCardTableRepository.findByNameContaining(searchValue, pageable);
                    } else if (isProjectManager) {
                        res = jobCardTableRepository.findAllByProjectIds(searchValue,projectIds,pageable);
                    } else {
                        res = jobCardTableRepository.findAllByVulcanizerId(getAuthenticatedUserId(), pageable);
                    }

                    Pagination pagination = mapToPagination(res);
                    HttpStatus successStatus = res.isEmpty() ? HttpStatus.NO_CONTENT : HttpStatus.OK;
                    String message = res.isEmpty() ? "Records Not Found" : "Successfully retrieved all records";

                    log.info("Response status -> {} and Message -> {}", successStatus, res);
                    response = getResponseDTO(message, successStatus, pagination);
                    return new ResponseEntity<>(response, successStatus);
                }

                response = getResponseDTO("No authorization to view jobcards", HttpStatus.FORBIDDEN);
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);

            }

        } catch (ResponseStatusException e) {
            log.error("Exception Occurred! and Message -> {} and Cause -> {}", e.getMessage(),e.getReason());
            response = getResponseDTO(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Exception Occurred! StatusCode -> {} and Cause -> {} and Message -> {}", 500,e.getCause(),e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response,HttpStatus.valueOf(response.getStatusCode()));
    }


    /**
     * Finds a jobCard by its UUID.
     *
     * @param id         The UUID of the jobCard to find.
     * @return           A ResponseEntity containing the response data.
     */
    @Override
    public ResponseEntity<ResponseDTO> findById(UUID id) {
        log.info("Inside find JobCard by Id ::: Trying to find jobCard by id -> {}", id);
        ResponseDTO response;
        try {
            var record = jobCardRepository.findById(id);
            if (record.isPresent()){
                var jobCardDTO = mapRecordForDetailedJobCard(record);
                log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.OK, jobCardDTO);
                response = getResponseDTO("Successfully retrieved record by id " + id, HttpStatus.OK, jobCardDTO);
                return new ResponseEntity<>(response,HttpStatus.valueOf(response.getStatusCode()));
            }
            log.info("Not Found! statusCode -> {} and Message -> {}", HttpStatus.NOT_FOUND, record);
            response = getResponseDTO("No Record Found!", HttpStatus.NOT_FOUND);

        } catch (ResponseStatusException e) {
            log.error("Exception Occurred!  Reason -> {} and Message -> {}",e.getReason(),e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Exception Occurred! statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(),e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response,HttpStatus.valueOf(response.getStatusCode()));
    }


    /**
     * Saves and publish new jobCard based on the provided JobCardDTO.
     * Calls the save  and sendJobCardNoticeAndPublish methods to
     * save the record in the database and publish emails
     *
     * @param jobCardDTO           The JobCardDTO containing jobCard information.
     * @return                  A ResponseEntity containing the response data.
     */
    @Override
    public ResponseEntity<ResponseDTO> saveAndPublish(JobCardDTO jobCardDTO) {
        var response =  save(jobCardDTO);
        if(isNotNullOrEmpty(response) &&
                response.getStatusCode().is2xxSuccessful() &&
                isNotNullOrEmpty(response.getBody())){
            try{
                log.info("Triggering jobCard notification and publisher");
                var jobCardResponse = (JobCardModel) response.getBody().getData();
                var jobCard = jobCardRepository.findById(jobCardResponse.getId());

                if(jobCard.isPresent()){
                    response.getBody().setData(jobCard.get());
                    sendJobCardNoticeAndPublish(jobCard.get(), jobCardResponse,"job-card-created");
                }
            }catch (Exception e){
                log.error(e.getMessage());
            }
        }
        return response;
    }


    /**
     * Saves a new jobCard based on the provided JobCardDTO.
     *
     * @param jobCardDTO           The JobCardDTO containing jobCard information.
     * @return                  A ResponseEntity containing the response data.
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDTO> save(JobCardDTO jobCardDTO) {
        log.info("Inside Save JobCard :::::: Trying to save jobCard");
        ResponseDTO response;
        try {
            var roles = getUserRoles();
            boolean isVulcanizer = hasRole(roles,List.of("VULCANIZER"));
            if (isVulcanizer) {
                var res = modelMapper.map(jobCardDTO, JobCardModel.class);
                Set<TyreDamageModel> tyreDamageModels = new HashSet<>();
                Set<BatteryDamageModel> batteryDamageModels = new HashSet<>();

                if (isNotNullOrEmpty(jobCardDTO.getTyreDamages())){
                    for (TyreDamageDTO tyreDamageDTO : jobCardDTO.getTyreDamages()){
                        TyreDamageModel tyreDamageModel = new TyreDamageModel();
                        tyreDamageModel.setDamageTypeId(tyreDamageDTO.getDamageTypeId());
                        tyreDamageModel.setOldTyre(tyreDamageDTO.getOldTyre());
                        tyreDamageModel.setNewTyre(tyreDamageDTO.getNewTyre());
                        tyreDamageModel.setJobCardModel(res);
                        tyreDamageModel.setCreatedAt(ZonedDateTime.now());
                        tyreDamageModel.setCreatedBy(getAuthenticatedUserId());
                        tyreDamageModels.add(tyreDamageModel);
                    }

                }

                if (isNotNullOrEmpty(jobCardDTO.getBatteryDamages())) {
                    for (BatteryDamageDTO batteryDamageDTO : jobCardDTO.getBatteryDamages()){
                        BatteryDamageModel batteryDamageModel = new BatteryDamageModel();
                        batteryDamageModel.setDamageTypeId(batteryDamageDTO.getDamageTypeId());
                        batteryDamageModel.setOldBattery(batteryDamageDTO.getOldBattery());
                        batteryDamageModel.setNewBattery(batteryDamageDTO.getNewBattery());
                        batteryDamageModel.setJobCardModel(res);
                        batteryDamageModel.setCreatedAt(ZonedDateTime.now());
                        batteryDamageModel.setCreatedBy(getAuthenticatedUserId());
                        batteryDamageModels.add(batteryDamageModel);
                    }

                }
                res.setJobCardStatusId(UUID.fromString("907f2c4f-438e-461b-bd83-e08b4943e635"));
                res.setTyreDamages(tyreDamageModels);
                res.setBatteryDamages(batteryDamageModels);
                res.setUpdatedAt(ZonedDateTime.now());
                res.setUpdatedBy(getAuthenticatedUserId());
                res.setCreatedAt(ZonedDateTime.now());
                res.setCreatedBy(getAuthenticatedUserId());

                UUID vehicleId = res.getVehicleId();
                List<TyreModel> vehicleTyres = jdbcTemplateQueries.getTyresByVehicleId(vehicleId);
                List<Integer> previousTyreMileages = new ArrayList<>();

                vehicleTyres
                        .forEach(tyre -> {
                            var previousMileage = jdbcTemplateQueries.getPreviousMileage(tyre.getId(),vehicleId);
                            previousTyreMileages.add(previousMileage);
                        });

                var previousMileage = previousTyreMileages
                        .stream()
                        .mapToInt(Integer::intValue)
                        .max();

                int maxPreviousMileage = previousMileage.orElse(0);


                //TODO - LOOK AT CASES WHERE TYRES FROM DIFFERENT VEHICLES ARE INSTALLED IN CURRENT VEHICLE
                if (Integer.parseInt(res.getMileage()) < maxPreviousMileage) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current mileage cannot be greater than previous mileage");
                }

                var record = jobCardRepository.save(res);

//                var jobCardEvent = jobCardTableRepository.findById(record.getId())
//                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + record.getId()+ "does not exist" ));
//
//                kafkaProducerService.sendJobCardEventToKafka(jobCardEvent,"Saved JobCard",getAuthenticatedUserFirstName());
                log.info("Success! statusCode -> {} and Message -> {}", HttpStatus.CREATED, record);
                response = getResponseDTO("Record saved successfully", HttpStatus.CREATED, record);
            } else {
                response = getResponseDTO("No authorization to create jobcards",HttpStatus.FORBIDDEN);

            }

        } catch (ResponseStatusException e) {
            log.error("Exception Occurred!, statusCode -> {} and Message -> {}", e.getStatusCode(), e.getReason());
            response = getResponseDTO(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (ObjectNotValidException e) {
            var message = String.join("\n", e.getErrorMessages());
            log.error("Exception occurred! Reason -> {}", message);
            response = getResponseDTO(message, HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException e) {
            log.error("Exception Occurred! Message -> {} and Cause -> {}", e.getMostSpecificCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Exception Occurred!, statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }

    /**
     * Updates an existing jobCard based on the provided JobCardDTO and UUID.
     *
     * @param jobCardDTO           The JobCardDTO containing updated jobCard information.
     * @param id                The UUID of the jobCard to update.
     * @return                  A ResponseEntity containing the response data.
     */
    @Transactional
    @Override
    public ResponseEntity<ResponseDTO> update(UUID id, JobCardDTO jobCardDTO) {
        log.info("Inside Update JobCard :::: Trying to save jobCard -> {}", id);
        ResponseDTO response;
        try {
            var roles = getUserRoles();
            boolean isWorkShopManager = hasRole(roles,List.of("WORKSHOP_MANAGER"));

            if (isWorkShopManager) {
                JobCardModel existingJobCard = jobCardRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Existing jobCard record " + id + "does not exist"));

                existingJobCard.setJobCardStatusId(jobCardDTO.getJobCardStatusId());
                existingJobCard.setMileage(jobCardDTO.getMileage());
                existingJobCard.setComments(jobCardDTO.getComments());
                existingJobCard.setUpdatedAt(ZonedDateTime.now());
                existingJobCard.setUpdatedBy(getAuthenticatedUserId());

                var record = jobCardRepository.save(existingJobCard);

                log.info("Success! statusCode -> {} and Message -> {} ", HttpStatus.ACCEPTED,record);
                response = getResponseDTO("Record updated successfully", HttpStatus.ACCEPTED, record);

                if (jobCardDTO.getJobCardStatusId().equals(UUID.fromString("37133943-0b68-4c20-be04-80508e933917"))) {
                    String createdBy = jdbcTemplateQueries.getIdName(record.getCreatedBy(),"user_");
//                    if (jdbcTemplateQueries.getIdName(jobCardDTO.getJobCardTypeId(),
//                            "job_card_type_category").equalsIgnoreCase("SWAP")) {
//                        if (isNotNullOrEmpty(record.getTyreDamages())) {
//
//                            for (TyreDamageModel tyreDamageModel : record.getTyreDamages()) {
//
//                                var oldTyreId = tyreDamageModel.getOldTyre();
//                                var newTyreId = tyreDamageModel.getNewTyre();
//                                var oldTyre = jdbcTemplateQueries.getTyreRecord(oldTyreId);
//                                var newTyre = jdbcTemplateQueries.getTyreRecord(newTyreId);
//
//                                var oldTyreLocation = oldTyre.getTyreLocation();
//                                var newTyreLocation = newTyre.getTyreLocation();
//
//                                if (isNotNullOrEmpty(newTyreId)) {
//                                    oldTyre.setInUse(true);
//                                    oldTyre.setVehicleId(record.getVehicleId());
//                                    oldTyre.setTyreLocation(newTyreLocation);
//                                    oldTyre.setUpdatedBy(record.getCreatedBy());
//                                    oldTyre.setUpdatedAt(ZonedDateTime.now());
//                                    jdbcTemplateQueries.updateTyre(oldTyre);
//
//                                    kafkaProducerService.sendTyreEventToKafka(oldTyre,"Swapped Tyre",createdBy);
//
//                                    newTyre.setInUse(true);
//                                    newTyre.setVehicleId(record.getVehicleId());
//                                    newTyre.setTyreLocation(oldTyreLocation);
//                                    newTyre.setUpdatedBy(record.getCreatedBy());
//                                    newTyre.setUpdatedAt(ZonedDateTime.now());
//                                    jdbcTemplateQueries.updateTyre(newTyre);
//
//                                    kafkaProducerService.sendTyreEventToKafka(newTyre,"Swapped Tyre",createdBy);
//
//                                }
//
//                            }
//                        }
//
//                    } else {
//                        if (isNotNullOrEmpty(record.getTyreDamages())) {
//
//                            for (TyreDamageModel tyreDamageModel : record.getTyreDamages()) {
//
//                                var oldTyreId = tyreDamageModel.getOldTyre();
//                                var newTyreId = tyreDamageModel.getNewTyre();
//                                var oldTyre = jdbcTemplateQueries.getTyreRecord(oldTyreId);
//                                var newTyre = jdbcTemplateQueries.getTyreRecord(newTyreId);
//
//                                if (isNotNullOrEmpty(newTyreId)) {
//                                    oldTyre.setInUse(false);
//                                    oldTyre.setVehicleId(null);
//                                    oldTyre.setDecommissioned(true);
//                                    oldTyre.setUpdatedBy(record.getCreatedBy());
//                                    oldTyre.setUpdatedAt(ZonedDateTime.now());
//                                    jdbcTemplateQueries.updateTyre(oldTyre);
//
//                                    kafkaProducerService.sendTyreEventToKafka(oldTyre,"Decommissioned Tyre",createdBy);
//
//
//                                    newTyre.setInUse(true);
//                                    newTyre.setTyreLocation(oldTyre.getTyreLocation());
//                                    newTyre.setVehicleId(record.getVehicleId());
//                                    newTyre.setUpdatedBy(record.getCreatedBy());
//                                    jdbcTemplateQueries.updateTyre(newTyre);
//
//                                    kafkaProducerService.sendTyreEventToKafka(newTyre,"Installed Tyre",createdBy);
//
//                                }
//
//                            }
//                        }
//
//                        if (isNotNullOrEmpty(record.getBatteryDamages())) {
//
//                            for (BatteryDamageModel batteryDamageModel : record.getBatteryDamages()) {
//                                var oldBatteryId = batteryDamageModel.getOldBattery();
//                                var newBatteryId = batteryDamageModel.getNewBattery();
//                                var oldBattery = jdbcTemplateQueries.getBatteryRecord(oldBatteryId);
//                                var newBattery = jdbcTemplateQueries.getBatteryRecord(newBatteryId);
//
//                                if (isNotNullOrEmpty(newBatteryId)) {
//                                    oldBattery.setInUse(false);
//                                    oldBattery.setVehicleId(null);
//                                    oldBattery.setDecommissioned(true);
//                                    oldBattery.setUpdatedBy(record.getCreatedBy());
//                                    oldBattery.setUpdatedAt(ZonedDateTime.now());
//                                    jdbcTemplateQueries.updateBattery(oldBattery);
//
//                                    kafkaProducerService.sendBatteryEventToKafka(oldBattery,"Decommissioned Battery",createdBy);
//
//
//                                    newBattery.setInUse(true);
//                                    newBattery.setVehicleId(record.getVehicleId());
//                                    newBattery.setUpdatedBy(record.getCreatedBy());
//                                    jdbcTemplateQueries.updateBattery(newBattery);
//
//                                    kafkaProducerService.sendBatteryEventToKafka(newBattery,"Installed Battery",createdBy);
//
//                                }
//                            }
//                        }
//                    }

                    if (isNotNullOrEmpty(record.getTyreDamages())) {
                        record.getTyreDamages().parallelStream()
                                .forEach(item -> {
                                    if (jdbcTemplateQueries.getDamageTypeTypeById(item.getDamageTypeId())
                                            .equalsIgnoreCase("SWAP")) {

                                        var oldTyreId = item.getOldTyre();
                                        var newTyreId = item.getNewTyre();
                                        var oldTyre = jdbcTemplateQueries.getTyreRecord(oldTyreId);
                                        var newTyre = jdbcTemplateQueries.getTyreRecord(newTyreId);

                                        var oldTyreLocation = oldTyre.getTyreLocation();
                                        var newTyreLocation = newTyre.getTyreLocation();

                                        if (isNotNullOrEmpty(newTyreId)) {
                                            oldTyre.setInUse(true);
                                            oldTyre.setVehicleId(record.getVehicleId());
                                            oldTyre.setTyreLocation(newTyreLocation);
                                            oldTyre.setUpdatedBy(record.getCreatedBy());
                                            oldTyre.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateTyre(oldTyre);

                                            try {
                                                kafkaProducerService.sendTyreEventToKafka(oldTyre, "Swapped Tyre", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }

                                            newTyre.setInUse(true);
                                            newTyre.setVehicleId(record.getVehicleId());
                                            newTyre.setTyreLocation(oldTyreLocation);
                                            newTyre.setUpdatedBy(record.getCreatedBy());
                                            newTyre.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateTyre(newTyre);

                                            try {
                                                kafkaProducerService.sendTyreEventToKafka(newTyre, "Swapped Tyre", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }

                                        }
                                    } else {
                                        var oldTyreId = item.getOldTyre();
                                        var newTyreId = item.getNewTyre();
                                        var oldTyre = jdbcTemplateQueries.getTyreRecord(oldTyreId);
                                        var newTyre = jdbcTemplateQueries.getTyreRecord(newTyreId);

                                        if (isNotNullOrEmpty(newTyreId)) {
                                            oldTyre.setInUse(false);
                                            oldTyre.setVehicleId(null);
                                            oldTyre.setDecommissioned(true);
                                            oldTyre.setUpdatedBy(record.getCreatedBy());
                                            oldTyre.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateTyre(oldTyre);

                                            try {
                                                kafkaProducerService.sendTyreEventToKafka(oldTyre, "Decommissioned Tyre", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }


                                            newTyre.setInUse(true);
                                            newTyre.setTyreLocation(oldTyre.getTyreLocation());
                                            newTyre.setVehicleId(record.getVehicleId());
                                            newTyre.setUpdatedBy(record.getCreatedBy());
                                            jdbcTemplateQueries.updateTyre(newTyre);

                                            try {
                                                kafkaProducerService.sendTyreEventToKafka(newTyre, "Installed Tyre", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                    }
                                });
                    }

                    if (isNotNullOrEmpty(record.getBatteryDamages())) {
                        record.getBatteryDamages().parallelStream()
                                .forEach(item -> {
                                    if (jdbcTemplateQueries.getDamageTypeTypeById(item.getDamageTypeId())
                                            .equalsIgnoreCase("SWAP")) {

                                        var oldBatteryId = item.getOldBattery();
                                        var newBatteryId = item.getNewBattery();
                                        var oldBattery = jdbcTemplateQueries.getBatteryRecord(oldBatteryId);
                                        var newBattery = jdbcTemplateQueries.getBatteryRecord(newBatteryId);

//                                        var oldBatteryLocation = oldBattery.getBatteryLocation();
//                                        var newBatteryLocation = newBattery.getBatteryLocation();

                                        if (isNotNullOrEmpty(newBatteryId)) {
                                            oldBattery.setInUse(true);
                                            oldBattery.setVehicleId(record.getVehicleId());
//                                            oldBattery.setBatteryLocation(newBatteryLocation);
                                            oldBattery.setUpdatedBy(record.getCreatedBy());
                                            oldBattery.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateBattery(oldBattery);

                                            try {
                                                kafkaProducerService.sendBatteryEventToKafka(oldBattery, "Swapped Battery", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }

                                            newBattery.setInUse(true);
                                            newBattery.setVehicleId(record.getVehicleId());
//                                            newBattery.setTyreLocation(oldBatteryLocation);
                                            newBattery.setUpdatedBy(record.getCreatedBy());
                                            newBattery.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateBattery(newBattery);

                                            try {
                                                kafkaProducerService.sendBatteryEventToKafka(newBattery, "Swapped Battery", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }

                                        }
                                    } else {
                                        var oldBatteryId = item.getOldBattery();
                                        var newBatteryId = item.getNewBattery();
                                        var oldBattery = jdbcTemplateQueries.getBatteryRecord(oldBatteryId);
                                        var newBattery = jdbcTemplateQueries.getBatteryRecord(newBatteryId);


                                        if (isNotNullOrEmpty(newBatteryId)) {
                                            oldBattery.setInUse(false);
                                            oldBattery.setVehicleId(null);
                                            oldBattery.setDecommissioned(true);
                                            oldBattery.setUpdatedBy(record.getCreatedBy());
                                            oldBattery.setUpdatedAt(ZonedDateTime.now());
                                            jdbcTemplateQueries.updateBattery(oldBattery);

                                            try {
                                                kafkaProducerService.sendBatteryEventToKafka(oldBattery, "Decommissioned Battery", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }


                                            newBattery.setInUse(true);
                                            newBattery.setVehicleId(record.getVehicleId());
                                            newBattery.setUpdatedBy(record.getCreatedBy());
                                            jdbcTemplateQueries.updateBattery(newBattery);

                                            try {
                                                kafkaProducerService.sendBatteryEventToKafka(newBattery, "Installed Battery", createdBy);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                    }
                                });
                    }
                    sendJobCardNoticeAndPublish(record,existingJobCard,"job-card-closed");
//                    var jobCardEvent = jobCardTableRepository.findById(record.getId())
//                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + record.getId()+ "does not exist" ));
//
//                    kafkaProducerService.sendJobCardEventToKafka(jobCardEvent,"Closed JobCard",getAuthenticatedUserFirstName());
                } else {
                    sendJobCardNoticeAndPublish(record,existingJobCard,"job-card-updated");

//                    var jobCardEvent = jobCardTableRepository.findById(record.getId())
//                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + record.getId()+ "does not exist" ));
//
//                    kafkaProducerService.sendJobCardEventToKafka(jobCardEvent,"Updated JobCard",getAuthenticatedUserFirstName())
                }
            } else {
                response = getResponseDTO("No authorization to update jobcards",HttpStatus.FORBIDDEN);
            }


        } catch (ResponseStatusException e) {
            log.error("Exception Occurred!, statusCode -> {} and Message -> {}", e.getStatusCode(), e.getReason());
            response = getResponseDTO(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (ObjectNotValidException e) {
            var message = String.join("\n", e.getErrorMessages());
            log.info("Exception occurred! Reason -> {}", message);
            response = getResponseDTO(message, HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {
            log.error("Exception Occurred!, statusCode -> {} and Cause -> {} and Message -> {}", 500, e.getCause(), e.getMessage());
            response = getResponseDTO(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }

    /**
     * Publishes a new jobCard or updated jobCard and produce to kafka.
     *
     * @params jobCard,kafkaJobCard,template
     * @return void.
     */
    @Override
    public void sendJobCardNoticeAndPublish(JobCardModel jobCard, JobCardModel existingJobCard, String template) {
        log.info("Inside Send jobCard Notice And Publish -> {}", jobCard);
        /**
         * Building notification payloads for Email
         */
        try {

            var userEmail = getAuthenticatedUserEmail();
            var userName = getAuthenticatedUserFirstName();
            CompletableFuture.runAsync(() -> {
                log.info("Inside Async block to send notification");

                List<NotificationProperties.ToBody> toBodyList = new ArrayList<>();
                toBodyList.addAll(notificationProperties.getExtraTo());
                log.info("ToBodyList -> {}", toBodyList);

                /**
                 Adding the logged-in user information to email to List
                 */
                if (isNotNullOrEmpty(userEmail) && isNotNullOrEmpty(userName)){
                    toBodyList.add(new NotificationProperties.ToBody(userEmail, userName));
                }

                Map<String, Object> defaultPlaceholders  = new HashMap<>();
                defaultPlaceholders.put("serviceNumber", jobCard.getServiceNumber());
                defaultPlaceholders.put("createdBy", userName);
                defaultPlaceholders.put("createdAtDate", formatDate(ZonedDateTime.now()));

                toBodyList
                        .forEach(toBody -> {
                            try {
                                log.info("Sending mail with params email -> {} and userName -> {}",
                                        toBody.getEmail(), toBody.getName());
                                NotificationDTO notificationDTO = new NotificationDTO();
                                Map<String, Object> placeholders  = new HashMap<>();
                                placeholders.putAll(defaultPlaceholders);
                                placeholders.put("user", toBody.getName());
                                notificationDTO.setPlaceholders(placeholders);
                                notificationDTO.setTo(List.of(toBody.getEmail()));

                                log.info("Hitting the sendJobCardNotification method with the body -> {}",
                                        notificationDTO);
                                sendJobCardNotificationToUser(notificationDTO, null,template);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        });
            });

//            var record = jobCardRepository.findById(jobCard.getId()).orElseThrow(
//                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role record " + jobCard.getId() + " does not exist")
//            );

//            /**
//             * Building kafka payloads for history
//             */
//            var res = modelMapper.map(record, JobCardDTO.class);
//

//            if (template.equalsIgnoreCase("job-card-created")) {
////                var historyEvent = HistoryEventDTO
////                    .builder()
////                    .aggregate("JobCard")
////                    .aggregateId(res.getId())
////                    .eventType("Saved JobCard")
////                    .eventFrom("job_card_service")
////                    .description("JobCard number " + res.getServiceNumber() + " was created by " + getAuthenticatedUserFirstName())
////                    .payload(res)
////                    .savedAt(res.getCreatedAt())
////                    .createdAt(String.valueOf(ZonedDateTime.now()))
////                    .createdBy(getAuthenticatedUserFirstName())
////                    .build();
////                kafkaProducerService.sendJobCardHistoryEventsToKafka(historyEvent);
////                kafkaProducerService.sendJobCardEventToKafka();
//                var jobCardEvent = jobCardTableRepository.findById(jobCard.getId())
//                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + jobCard.getId()+ "does not exist" ));
//
//                var res = modelMapper.map(existingJobCard,JobCardTable.class);
//                kafkaProducerService.sendJobCardEventToKafka(res,jobCardName,"Saved JobCard",getAuthenticatedUserFirstName());
//            } else if (template.equalsIgnoreCase("job-card-updated")) {
////                var kafkaJobCard = modelMapper.map(existingJobCard, JobCardDTO.class);
////
////                var jobCardName = jdbcTemplateQueries.getJobCardStatusNameById(jobCard.getJobCardStatusId());
////                log.info("jobCardName -> {}", jobCardName );
////                var historyEvent = HistoryEventDTO
////                        .builder()
////                        .aggregate("JobCard")
////                        .aggregateId(res.getId())
////                        .eventType("Updated JobCard")
////                        .eventFrom("job_card_service")
////                        .description("JobCard number " + res.getServiceNumber() + " was updated by " + getAuthenticatedUserFirstName() + " to " + jobCardName)
////                        .payload(kafkaJobCard)
////                        .savedAt(res.getCreatedAt())
////                        .createdAt(String.valueOf(ZonedDateTime.now()))
////                        .createdBy(getAuthenticatedUserFirstName())
////                        .build();
////                kafkaProducerService.sendJobCardHistoryEventsToKafka(historyEvent);
//                var res = modelMapper.map(existingJobCard,JobCardTable.class);
//            var jobCardEvent = jobCardTableRepository.findById(jobCard.getId())
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + jobCard.getId()+ "does not exist" ));
//
//            kafkaProducerService.sendJobCardEventToKafka(res,jobCardName,"Updated JobCard",getAuthenticatedUserFirstName());
//            }
//            else if (template.equalsIgnoreCase("job-card-closed")){
////                var kafkaJobCard = modelMapper.map(existingJobCard, JobCardDTO.class);
////                var historyEvent = HistoryEventDTO
////                        .builder()
////                        .aggregate("JobCard")
////                        .aggregateId(res.getId())
////                        .eventType("Closed JobCard")
////                        .eventFrom("job_card_service")
////                        .description("JobCard number " + res.getServiceNumber() + " was closed by " + getAuthenticatedUserFirstName())
////                        .payload(kafkaJobCard)
////                        .savedAt(res.getCreatedAt())
////                        .createdAt(String.valueOf(ZonedDateTime.now()))
////                        .createdBy(getAuthenticatedUserFirstName())
////                        .build();
////                kafkaProducerService.sendJobCardHistoryEventsToKafka(historyEvent);
//
//            var res = modelMapper.map(existingJobCard,JobCardTable.class);
//            var jobCardEvent = jobCardTableRepository.findById(jobCard.getId())
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Jobcard record " + jobCard.getId()+ "does not exist" ));
//
//            kafkaProducerService.sendJobCardEventToKafka(res,jobCardName,"Closed JobCard",getAuthenticatedUserFirstName());
//            }

//            var jobCardName = jdbcTemplateQueries.getJobCardStatusNameById(jobCard.getJobCardStatusId());
            var jobCardStatus = jdbcTemplateQueries.getIdName(jobCard.getJobCardStatusId(),"job_card_status");
            switch (template.toLowerCase()) {
                case "job-card-created" -> {
                    var jobCardEventCreated = modelMapper.map(existingJobCard, JobCardTable.class);
                    kafkaProducerService.sendJobCardEventToKafka(jobCardEventCreated, jobCardStatus, "Saved JobCard", getAuthenticatedUserFirstName());
                }
                case "job-card-updated" -> {
                    var jobCardEventUpdated = modelMapper.map(existingJobCard, JobCardTable.class);
                    kafkaProducerService.sendJobCardEventToKafka(jobCardEventUpdated, jobCardStatus, "Updated JobCard", getAuthenticatedUserFirstName());
                }
                case "job-card-closed" -> {
                    var jobCardEventClosed = modelMapper.map(existingJobCard, JobCardTable.class);
                    kafkaProducerService.sendJobCardEventToKafka(jobCardEventClosed, jobCardStatus, "Closed JobCard", getAuthenticatedUserFirstName());
                }
                default -> throw new RuntimeException("Jobcard case not recognized");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Maps a List of JobCardTable to a List of JobCardTableDTO objects.
     *
     * @param data The List of JobCardModel to be mapped.
     * @return The List of JobCardTableDTO objects.
     */
    private List<JobCardTableDTO> mapListForJobCardTable(List<JobCardModel> data) {
        if (data == null || data.size() == 0) {
            return  new ArrayList<>();
        }
        return data.stream()
                .map(item -> {
                    var res = modelMapper.map(item, JobCardTableDTO.class);
                    if (item.getId() != null) {
//                        var jobCardTypeName = (jdbcTemplateQueries.getIdName(item.getJobCardTypeId(),"job_card_type"));
                        var jobCardStatusName = (jdbcTemplateQueries.getIdName(item.getJobCardStatusId(),"job_card_status"));
                        var vehicleName = (jdbcTemplateQueries.getIdName(item.getVehicleId(),"vehicle"));
                        var vehicleTypeName = (jdbcTemplateQueries.getIdName(item.getVehicleTypeId(),"vehicle_type"));
                        var jobCardOwner = (jdbcTemplateQueries.getIdName(item.getCreatedBy(),"user_"));
                        var projectName = (jdbcTemplateQueries.getIdName(item.getProjectId(),"project"));
                        res.setId(item.getId());
                        res.setJobCardOwner(jobCardOwner);
//                        res.setJobCardTypeId(item.getJobCardTypeId());
//                        res.setJobCardType(jobCardTypeName);
                        res.setJobCardStatus(jobCardStatusName);
                        res.setJobCardStatusId(item.getJobCardStatusId());
                        res.setVehicle(vehicleName);
                        res.setVehicleId(item.getVehicleId());
                        res.setVehicleType(vehicleTypeName);
                        res.setVehicleTypeId(item.getVehicleTypeId());
                        res.setProject(projectName);
                        res.setProjectId(item.getProjectId());
                        res.setDateOpened(formatDate(item.getCreatedAt()));
                    }
                    return  res;
                }).collect(Collectors.toList());
    }

    /**
     * Maps a Page of JobCardModel to a Page of JobCardTableDTO objects.
     *
     * @param data The Page of JobCardModel to be mapped.
     * @return The Page of JobCardTableDTO objects.
     */
    private Page<JobCardTableDTO> mapPageForJobCardTable(Page<JobCardModel> data) {
        List<JobCardTableDTO> jobCardTableDTOList = data.getContent().stream()
                .map(item -> {
                    var res = modelMapper.map(item, JobCardTableDTO.class);
                    if (item.getId() != null) {
//                        var jobCardTypeName = (jdbcTemplateQueries.getIdName(item.getJobCardTypeId(),"job_card_type"));
                        var jobCardStatusName = (jdbcTemplateQueries.getIdName(item.getJobCardStatusId(),"job_card_status"));
                        var vehicleName = (jdbcTemplateQueries.getIdName(item.getVehicleId(),"vehicle"));
                        var vehicleTypeName = (jdbcTemplateQueries.getIdName(item.getVehicleTypeId(),"vehicle_type"));
                        var projectName = (jdbcTemplateQueries.getIdName(item.getProjectId(),"project"));
                        var jobCardOwner = (jdbcTemplateQueries.getIdName(item.getCreatedBy(),"user_"));
                        res.setId(item.getId());
                        res.setJobCardOwner(jobCardOwner);
//                        res.setJobCardTypeId(item.getJobCardTypeId());
//                        res.setJobCardType(jobCardTypeName);
                        res.setJobCardStatus(jobCardStatusName);
                        res.setJobCardStatusId(item.getJobCardStatusId());
                        res.setVehicle(vehicleName);
                        res.setVehicleId(item.getVehicleId());
                        res.setVehicleType(vehicleTypeName);
                        res.setVehicleTypeId(item.getVehicleTypeId());
                        res.setProject(projectName);
                        res.setProjectId(item.getProjectId());
                        res.setDateOpened(formatDate(item.getCreatedAt()));
                    }
                    return  res;
                }).collect(Collectors.toList());
        return new PageImpl<>(jobCardTableDTOList,data.getPageable(),data.getTotalElements());
    }

    /**
     * Maps a model of JobCardModel to a DTO of JobCardDTO object.
     *
     * @param jobCardModel The record of JobCardModel to be mapped.
     * @return The DTO of JobCardDTO object.
     */
    JobCardDTO mapRecordForDetailedJobCard(Optional<JobCardModel> jobCardModel) {
        var res = modelMapper.map(jobCardModel, JobCardDTO.class);
        Set<TyreDamageDTO> tyreDamageDTOSet = res.getTyreDamages();
        tyreDamageDTOSet.stream().forEach(
                tyreDamageDTO -> {
                    var oldTyreTypeId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getOldTyre(),"tyre_type");
                    var newTyreTypeId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getNewTyre(),"tyre_type");
                    var oldTyreBrandId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getOldTyre(),"tyre_brand");
                    var newTyreBrandId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getNewTyre(),"tyre_brand");
                    var oldTyreSizeId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getOldTyre(), "tyre_size");
                    var newTyreSizeId = jdbcTemplateQueries.getIdByTyreId(tyreDamageDTO.getNewTyre(), "tyre_size");
                    tyreDamageDTO.setDamage(jdbcTemplateQueries.getIdName(tyreDamageDTO.getDamageTypeId(),"damage"));
                    tyreDamageDTO.setOldSerialNumber(jdbcTemplateQueries.getIdName(tyreDamageDTO.getOldTyre(),"tyre"));
                    tyreDamageDTO.setOldBrand(jdbcTemplateQueries.getIdName(oldTyreBrandId,"tyre_brand"));
                    tyreDamageDTO.setOldTyreType(jdbcTemplateQueries.getIdName(oldTyreTypeId,"tyre_type"));
                    tyreDamageDTO.setOldTyreSize(jdbcTemplateQueries.getIdName(oldTyreSizeId, "tyre_size"));
                    tyreDamageDTO.setNewSerialNumber(jdbcTemplateQueries.getIdName(tyreDamageDTO.getNewTyre(),"tyre"));
                    tyreDamageDTO.setNewBrand(jdbcTemplateQueries.getIdName(newTyreBrandId,"tyre_brand"));
                    tyreDamageDTO.setNewTyreType(jdbcTemplateQueries.getIdName(newTyreTypeId,"tyre_type"));
                    tyreDamageDTO.setNewTyreSize(jdbcTemplateQueries.getIdName(newTyreSizeId, "tyre_size"));
                }
        );

        Set<BatteryDamageDTO> batteryDamageDTOSet = res.getBatteryDamages();
        batteryDamageDTOSet.stream().forEach(
                batteryDamageDTO -> {
                    var oldBatteryTypeId = jdbcTemplateQueries.getIdByBatteryId(batteryDamageDTO.getOldBattery(),"battery_type");
                    var newBatteryTypeId = jdbcTemplateQueries.getIdByBatteryId(batteryDamageDTO.getNewBattery(),"battery_type");
                    batteryDamageDTO.setDamage(jdbcTemplateQueries.getIdName(batteryDamageDTO.getDamageTypeId(),"damage"));
                    batteryDamageDTO.setOldSerialNumber(jdbcTemplateQueries.getIdName(batteryDamageDTO.getOldBattery(),"battery"));
                    batteryDamageDTO.setOldBatteryType(jdbcTemplateQueries.getIdName(oldBatteryTypeId,"battery_type"));
                    batteryDamageDTO.setNewSerialNumber(jdbcTemplateQueries.getIdName(batteryDamageDTO.getNewBattery(),"battery"));
                    batteryDamageDTO.setNewBatteryType(jdbcTemplateQueries.getIdName(newBatteryTypeId,"battery_type"));
                }
        );

//        var jobCardTypeName = (jdbcTemplateQueries.getIdName(res.getJobCardTypeId(),"job_card_type"));
//        var jobCardTypeCategoryName = (jdbcTemplateQueries.getIdName(res.getJobCardTypeId(),"job_card_type_category"));
        var jobCardStatusName = (jdbcTemplateQueries.getIdName(res.getJobCardStatusId(),"job_card_status"));
        var vehicleName = (jdbcTemplateQueries.getIdName(res.getVehicleId(),"vehicle"));
        var vehicleTypeName = (jdbcTemplateQueries.getIdName(res.getVehicleTypeId(),"vehicle_type"));
        var ticketOwner = (jdbcTemplateQueries.getIdName(res.getCreatedBy(),"user_"));
        var projectName = (jdbcTemplateQueries.getIdName(res.getProjectId(),"project"));

//        res.setJobCardType(jobCardTypeName);
//        res.setJobCardTypeCategory(jobCardTypeCategoryName);
        res.setJobCardStatus(jobCardStatusName);
        res.setVehicle(vehicleName);
        res.setVehicleType(vehicleTypeName);
        res.setProject(projectName);
        res.setJobCardOwner(ticketOwner);
        res.setDateOpened(formatDate(ZonedDateTime.parse(res.getCreatedAt())));
        res.setTyreDamages(tyreDamageDTOSet);
        res.setBatteryDamages(batteryDamageDTOSet);
        return res;
    }

    /**
     * This is a method that triggers the notification for a given user
     *
     * @param notificationDTO This is the notification object after adding the placeholders for the particular jobCard
     * @param files           This for the Map<String, byte[]> which contains all attachments
     * @param template        This is the corresponding name for the Template in the application.yml
     * @author Prince Amofah
     * @createdAt 14th December 2023
     * @modified
     * @modifiedBy
     * @modifiedAt
     */
    void sendJobCardNotificationToUser(NotificationDTO notificationDTO, Map<String, byte[]> files, String template) {
        log.info("Inside Send Form Notification To User in JobCard Service Implementation");
        try {
            if(!isNotNullOrEmpty(notificationDTO)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "....... Notification object is null");
            }

            if (files != null && files.size() > 0) {
                notificationDTO.setHasAttachment(true);
                notificationDTO.setFiles(files);
            }

            notificationService.sendEmail(notificationDTO,template);
            log.info("Success! statusCode -> {} and Message -> {}",201,notificationDTO);

        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    e.getReason());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred while sending notification");
        }
    }

    /**
     * Formats the date and time string into a standardized format.
     *
     * @param createdAt The string representing the date and time of creation.
     * @return The formatted date and time string.
     */
    String formatDate(ZonedDateTime createdAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return createdAt.format(formatter);
    }

}
