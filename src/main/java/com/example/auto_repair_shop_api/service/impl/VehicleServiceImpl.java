package com.example.auto_repair_shop_api.service.impl;

import com.example.auto_repair_shop_api.dto.vehicle.VehicleCreateDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleResponseDTO;
import com.example.auto_repair_shop_api.dto.vehicle.VehicleUpdateDTO;
import com.example.auto_repair_shop_api.dto.common.PageResponse;
import com.example.auto_repair_shop_api.exception.DuplicateResourceException;
import com.example.auto_repair_shop_api.exception.OwnershipViolationException;
import com.example.auto_repair_shop_api.exception.ResourceInUseException;
import com.example.auto_repair_shop_api.exception.ResourceNotFoundException;
import com.example.auto_repair_shop_api.mapper.VehicleMapper;
import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.Vehicle;
import com.example.auto_repair_shop_api.model.enums.Role;
import com.example.auto_repair_shop_api.repository.ServiceRequestRepository;
import com.example.auto_repair_shop_api.repository.ServiceVisitRepository;
import com.example.auto_repair_shop_api.repository.VehicleRepository;
import com.example.auto_repair_shop_api.service.AppUserService;
import com.example.auto_repair_shop_api.service.VehicleService;
import com.example.auto_repair_shop_api.util.PageRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.licensePlateContains;
import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.manufacturerContains;
import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.modelContains;
import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.ownedBy;
import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.yearAtLeast;
import static com.example.auto_repair_shop_api.repository.specification.VehicleSpecifications.yearAtMost;

@Service
public class VehicleServiceImpl implements VehicleService {

    // Sort whitelist: API field name -> entity property.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "licensePlate", "licensePlate",
            "manufacturer", "manufacturer",
            "model", "model",
            "year", "year",
            "firstRegistrationDate", "firstRegistrationDate"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "licensePlate");

    private final AppUserService appUserService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceVisitRepository serviceVisitRepository;

    @Autowired
    public VehicleServiceImpl(AppUserService appUserService, VehicleRepository vehicleRepository, VehicleMapper vehicleMapper,
                              ServiceRequestRepository serviceRequestRepository,
                              ServiceVisitRepository serviceVisitRepository) {
        this.appUserService = appUserService;
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceVisitRepository = serviceVisitRepository;
    }

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(VehicleCreateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (vehicleRepository.existsByLicensePlate(dto.licensePlate())) {
            throw new DuplicateResourceException("Vehicle with this license plate already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setManufacturer(dto.manufacturer());
        vehicle.setModel(dto.model());
        vehicle.setYear(dto.year());
        vehicle.setFirstRegistrationDate(dto.firstRegistrationDate());
        vehicle.setOwner(appUser);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponseDto(savedVehicle);
    }

    @Override
    public PageResponse<VehicleResponseDTO> getVehiclesByCurrentUser(String currentUsername,
                                                                     String licensePlate, String manufacturer, String model,
                                                                     Integer yearFrom, Integer yearTo, Long ownerId,
                                                                     Integer page, Integer size, String sort) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new IllegalArgumentException("yearFrom must not be greater than yearTo");
        }

        PageRequest pageRequest = PageRequestFactory.create(page, size, sort, SORT_FIELDS, DEFAULT_SORT);

        List<Specification<Vehicle>> filters = new ArrayList<>();


        if (appUser.getRole() != Role.ADMIN) {
            filters.add(ownedBy(appUser.getId()));
        } else if (ownerId != null) {
            filters.add(ownedBy(ownerId));
        }

        if (licensePlate != null && !licensePlate.isBlank()) {
            filters.add(licensePlateContains(licensePlate));
        }
        if (manufacturer != null && !manufacturer.isBlank()) {
            filters.add(manufacturerContains(manufacturer));
        }
        if (model != null && !model.isBlank()) {
            filters.add(modelContains(model));
        }
        if (yearFrom != null) {
            filters.add(yearAtLeast(yearFrom));
        }
        if (yearTo != null) {
            filters.add(yearAtMost(yearTo));
        }

        Page<Vehicle> result = vehicleRepository.findAll(Specification.allOf(filters), pageRequest);
        return PageResponse.from(result, vehicleMapper::toResponseDto);
    }

    @Override
    public VehicleResponseDTO getVehicleById(Long id, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = getVehicleOrThrow(id);
        checkOwnerOrAdmin(vehicle, appUser, "You do not have permission to view this vehicle");

        return vehicleMapper.toResponseDto(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Long id, VehicleUpdateDTO dto, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = getVehicleOrThrow(id);
        checkOwnerOrAdmin(vehicle, appUser, "You do not have permission to update this vehicle");

        if (vehicleRepository.existsByLicensePlateAndIdNot(dto.licensePlate(), id)) {
            throw new DuplicateResourceException("Vehicle with this license plate already exists");
        }

        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setManufacturer(dto.manufacturer());
        vehicle.setModel(dto.model());
        vehicle.setYear(dto.year());
        vehicle.setFirstRegistrationDate(dto.firstRegistrationDate());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponseDto(updatedVehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id, String currentUsername) {

        AppUser appUser = appUserService.getByUsernameOrThrow(currentUsername);

        Vehicle vehicle = getVehicleOrThrow(id);
        checkOwnerOrAdmin(vehicle, appUser, "You do not have permission to delete this vehicle");

        if (serviceRequestRepository.existsByVehicleId(id) || serviceVisitRepository.existsByVehicleId(id)) {
            throw new ResourceInUseException("Vehicle cannot be deleted because it has service requests or visits");
        }

        vehicleRepository.delete(vehicle);
    }

    private Vehicle getVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    private void checkOwnerOrAdmin(Vehicle vehicle, AppUser appUser, String message) {
        boolean isOwner = vehicle.getOwner().getId().equals(appUser.getId());
        boolean isAdmin = appUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new OwnershipViolationException(message);
        }
    }
}
