package com.travelplan.travel.service;

import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import com.travelplan.travel.dto.*;
import com.travelplan.travel.entity.*;
import com.travelplan.travel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelService {

    private final TravelRepository travelRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public Page<TravelDto> getAllTravels(Long userId, Travel.TravelStatus status, String search, Pageable pageable) {
        return travelRepository.findAllWithFilters(userId, status, search, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public TravelDto getTravelById(Long id) {
        Travel travel = travelRepository.findByIdWithDestinations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel", id));
        return mapToDto(travel);
    }

    @Transactional(readOnly = true)
    public Page<TravelDto> getUserTravels(Long userId, Pageable pageable) {
        return travelRepository.findByCreatedBy(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public TravelDto createTravel(CreateTravelRequest request, Long userId) {
        validateDates(request.getStartDate(), request.getEndDate());

        Travel travel = Travel.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalBudget(request.getTotalBudget())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .createdBy(userId)
                .status(Travel.TravelStatus.DRAFT)
                .build();

        if (request.getDestinations() != null) {
            for (int i = 0; i < request.getDestinations().size(); i++) {
                CreateTravelRequest.CreateDestinationRequest destRequest = request.getDestinations().get(i);
                TravelDestination travelDest = createTravelDestination(destRequest, travel, i);
                travel.getDestinations().add(travelDest);
            }
        }

        if (request.getAccommodations() != null) {
            for (CreateTravelRequest.CreateAccommodationRequest accRequest : request.getAccommodations()) {
                TravelAccommodation accommodation = createTravelAccommodation(accRequest, travel);
                travel.getAccommodations().add(accommodation);
            }
        }

        if (request.getTransportations() != null) {
            for (CreateTravelRequest.CreateTransportationRequest transRequest : request.getTransportations()) {
                Transportation transportation = createTransportation(transRequest, travel);
                travel.getTransportations().add(transportation);
            }
        }

        Travel savedTravel = travelRepository.save(travel);
        log.info("Created travel with id: {} for user: {}", savedTravel.getId(), userId);
        return mapToDto(savedTravel);
    }

    @Transactional
    public TravelDto updateTravel(Long id, UpdateTravelRequest request, Long userId, boolean isAdmin) {
        Travel travel = travelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel", id));

        if (!isAdmin && !travel.getCreatedBy().equals(userId)) {
            throw new BusinessException("You don't have permission to update this travel");
        }

        if (request.getTitle() != null) {
            travel.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            travel.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            travel.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            travel.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            travel.setStatus(request.getStatus());
        }
        if (request.getTotalBudget() != null) {
            travel.setTotalBudget(request.getTotalBudget());
        }
        if (request.getCurrency() != null) {
            travel.setCurrency(request.getCurrency());
        }

        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateDates(travel.getStartDate(), travel.getEndDate());
        }

        Travel updatedTravel = travelRepository.save(travel);
        log.info("Updated travel with id: {}", id);
        return mapToDto(updatedTravel);
    }

    @Transactional
    public void deleteTravel(Long id, Long userId, boolean isAdmin) {
        Travel travel = travelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel", id));

        if (!isAdmin && !travel.getCreatedBy().equals(userId)) {
            throw new BusinessException("You don't have permission to delete this travel");
        }

        travelRepository.delete(travel);
        log.info("Deleted travel with id: {}", id);
    }

    @Transactional
    public TravelDto updateTravelStatus(Long id, Travel.TravelStatus status, Long userId, boolean isAdmin) {
        Travel travel = travelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel", id));

        if (!isAdmin && !travel.getCreatedBy().equals(userId)) {
            throw new BusinessException("You don't have permission to update this travel");
        }

        travel.setStatus(status);
        Travel updatedTravel = travelRepository.save(travel);
        log.info("Updated travel status to {} for id: {}", status, id);
        return mapToDto(updatedTravel);
    }

    public long countTravelsByStatus(Travel.TravelStatus status) {
        return travelRepository.countByStatus(status);
    }

    public long countUserTravels(Long userId) {
        return travelRepository.countByCreatedBy(userId);
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("End date cannot be before start date");
        }
    }

    private TravelDestination createTravelDestination(CreateTravelRequest.CreateDestinationRequest request,
                                                       Travel travel, int order) {
        Destination destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination", request.getDestinationId()));

        TravelDestination travelDest = TravelDestination.builder()
                .travel(travel)
                .destination(destination)
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .visitOrder(request.getVisitOrder() != null ? request.getVisitOrder() : order + 1)
                .notes(request.getNotes())
                .build();

        if (request.getActivities() != null) {
            for (CreateTravelRequest.CreateActivityRequest actRequest : request.getActivities()) {
                Activity activity = activityRepository.findById(actRequest.getActivityId())
                        .orElseThrow(() -> new ResourceNotFoundException("Activity", actRequest.getActivityId()));

                TravelActivity travelActivity = TravelActivity.builder()
                        .travelDestination(travelDest)
                        .activity(activity)
                        .plannedDate(actRequest.getPlannedDate())
                        .plannedTime(actRequest.getPlannedTime())
                        .notes(actRequest.getNotes())
                        .status(TravelActivity.BookingStatus.PLANNED)
                        .build();
                travelDest.getActivities().add(travelActivity);
            }
        }

        return travelDest;
    }

    private TravelAccommodation createTravelAccommodation(CreateTravelRequest.CreateAccommodationRequest request,
                                                           Travel travel) {
        BigDecimal totalPrice = null;
        if (request.getPricePerNight() != null && request.getCheckInDate() != null && request.getCheckOutDate() != null) {
            long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
            totalPrice = request.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        }

        return TravelAccommodation.builder()
                .travel(travel)
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .pricePerNight(request.getPricePerNight())
                .totalPrice(totalPrice)
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .notes(request.getNotes())
                .status(TravelAccommodation.BookingStatus.PENDING)
                .build();
    }

    private Transportation createTransportation(CreateTravelRequest.CreateTransportationRequest request,
                                                 Travel travel) {
        return Transportation.builder()
                .travel(travel)
                .type(Transportation.TransportType.valueOf(request.getType().toUpperCase()))
                .departureLocation(request.getDepartureLocation())
                .arrivalLocation(request.getArrivalLocation())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .carrier(request.getCarrier())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .notes(request.getNotes())
                .status(Transportation.BookingStatus.PENDING)
                .build();
    }

    private TravelDto mapToDto(Travel travel) {
        return TravelDto.builder()
                .id(travel.getId())
                .title(travel.getTitle())
                .description(travel.getDescription())
                .startDate(travel.getStartDate())
                .endDate(travel.getEndDate())
                .status(travel.getStatus())
                .totalBudget(travel.getTotalBudget())
                .currency(travel.getCurrency())
                .createdBy(travel.getCreatedBy())
                .durationDays(travel.getDurationDays())
                .destinations(mapDestinations(travel.getDestinations()))
                .accommodations(mapAccommodations(travel.getAccommodations()))
                .transportations(mapTransportations(travel.getTransportations()))
                .createdAt(travel.getCreatedAt())
                .updatedAt(travel.getUpdatedAt())
                .build();
    }

    private List<TravelDto.DestinationDto> mapDestinations(List<TravelDestination> destinations) {
        if (destinations == null) return new ArrayList<>();
        return destinations.stream()
                .map(td -> TravelDto.DestinationDto.builder()
                        .id(td.getId())
                        .destinationId(td.getDestination().getId())
                        .name(td.getDestination().getName())
                        .country(td.getDestination().getCountry())
                        .city(td.getDestination().getCity())
                        .arrivalDate(td.getArrivalDate())
                        .departureDate(td.getDepartureDate())
                        .visitOrder(td.getVisitOrder())
                        .notes(td.getNotes())
                        .activities(mapActivities(td.getActivities()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<TravelDto.ActivityDto> mapActivities(List<TravelActivity> activities) {
        if (activities == null) return new ArrayList<>();
        return activities.stream()
                .map(ta -> TravelDto.ActivityDto.builder()
                        .id(ta.getId())
                        .activityId(ta.getActivity().getId())
                        .name(ta.getActivity().getName())
                        .category(ta.getActivity().getCategory())
                        .plannedDate(ta.getPlannedDate())
                        .plannedTime(ta.getPlannedTime())
                        .actualCost(ta.getActualCost())
                        .status(ta.getStatus().name())
                        .notes(ta.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    private List<TravelDto.AccommodationDto> mapAccommodations(List<TravelAccommodation> accommodations) {
        if (accommodations == null) return new ArrayList<>();
        return accommodations.stream()
                .map(acc -> TravelDto.AccommodationDto.builder()
                        .id(acc.getId())
                        .name(acc.getName())
                        .type(acc.getType())
                        .address(acc.getAddress())
                        .checkInDate(acc.getCheckInDate())
                        .checkOutDate(acc.getCheckOutDate())
                        .pricePerNight(acc.getPricePerNight())
                        .totalPrice(acc.getTotalPrice())
                        .currency(acc.getCurrency())
                        .confirmationNumber(acc.getConfirmationNumber())
                        .status(acc.getStatus().name())
                        .notes(acc.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    private List<TravelDto.TransportationDto> mapTransportations(List<Transportation> transportations) {
        if (transportations == null) return new ArrayList<>();
        return transportations.stream()
                .map(trans -> TravelDto.TransportationDto.builder()
                        .id(trans.getId())
                        .type(trans.getType().name())
                        .departureLocation(trans.getDepartureLocation())
                        .arrivalLocation(trans.getArrivalLocation())
                        .departureTime(trans.getDepartureTime())
                        .arrivalTime(trans.getArrivalTime())
                        .carrier(trans.getCarrier())
                        .bookingReference(trans.getBookingReference())
                        .price(trans.getPrice())
                        .currency(trans.getCurrency())
                        .status(trans.getStatus().name())
                        .notes(trans.getNotes())
                        .build())
                .collect(Collectors.toList());
    }
}
