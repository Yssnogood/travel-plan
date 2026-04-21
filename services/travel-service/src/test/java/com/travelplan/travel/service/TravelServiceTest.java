package com.travelplan.travel.service;

import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import com.travelplan.travel.dto.CreateTravelRequest;
import com.travelplan.travel.dto.TravelDto;
import com.travelplan.travel.dto.UpdateTravelRequest;
import com.travelplan.travel.entity.Travel;
import com.travelplan.travel.repository.ActivityRepository;
import com.travelplan.travel.repository.DestinationRepository;
import com.travelplan.travel.repository.TravelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private TravelService travelService;

    private Travel travel;
    private final Long userId = 1L;
    private final Long travelId = 100L;

    @BeforeEach
    void setUp() {
        travel = Travel.builder()
                .id(travelId)
                .title("Trip to Paris")
                .description("A wonderful trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 10))
                .status(Travel.TravelStatus.DRAFT)
                .totalBudget(new BigDecimal("2000.00"))
                .currency("EUR")
                .createdBy(userId)
                .destinations(new ArrayList<>())
                .accommodations(new ArrayList<>())
                .transportations(new ArrayList<>())
                .build();
    }

    @Test
    void getAllTravels_returnsPageOfTravelDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Travel> page = new PageImpl<>(List.of(travel), pageable, 1);

        when(travelRepository.findAllWithFiltersAndSearch(eq(userId), eq(Travel.TravelStatus.DRAFT), eq("Paris"), eq(pageable)))
                .thenReturn(page);

        Page<TravelDto> result = travelService.getAllTravels(userId, Travel.TravelStatus.DRAFT, "Paris", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Trip to Paris");
        verify(travelRepository).findAllWithFiltersAndSearch(userId, Travel.TravelStatus.DRAFT, "Paris", pageable);
    }

    @Test
    void getTravelById_existingId_returnsTravelDto() {
        when(travelRepository.findByIdWithDestinations(travelId)).thenReturn(Optional.of(travel));

        TravelDto result = travelService.getTravelById(travelId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(travelId);
        assertThat(result.getTitle()).isEqualTo("Trip to Paris");
        assertThat(result.getStatus()).isEqualTo(Travel.TravelStatus.DRAFT);
        verify(travelRepository).findByIdWithDestinations(travelId);
    }

    @Test
    void getTravelById_nonExistingId_throwsResourceNotFoundException() {
        when(travelRepository.findByIdWithDestinations(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelService.getTravelById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTravel_validRequest_returnsTravelDto() {
        CreateTravelRequest request = CreateTravelRequest.builder()
                .title("Trip to Rome")
                .description("Italian adventure")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 15))
                .totalBudget(new BigDecimal("3000.00"))
                .currency("EUR")
                .build();

        Travel savedTravel = Travel.builder()
                .id(200L)
                .title("Trip to Rome")
                .description("Italian adventure")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 15))
                .totalBudget(new BigDecimal("3000.00"))
                .currency("EUR")
                .createdBy(userId)
                .status(Travel.TravelStatus.DRAFT)
                .destinations(new ArrayList<>())
                .accommodations(new ArrayList<>())
                .transportations(new ArrayList<>())
                .build();

        when(travelRepository.save(any(Travel.class))).thenReturn(savedTravel);

        TravelDto result = travelService.createTravel(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Trip to Rome");
        assertThat(result.getCreatedBy()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(Travel.TravelStatus.DRAFT);
        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void createTravel_endDateBeforeStartDate_throwsBusinessException() {
        CreateTravelRequest request = CreateTravelRequest.builder()
                .title("Bad Trip")
                .startDate(LocalDate.of(2026, 7, 15))
                .endDate(LocalDate.of(2026, 7, 1))
                .build();

        assertThatThrownBy(() -> travelService.createTravel(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void updateTravel_asOwner_returnsTravelDto() {
        UpdateTravelRequest request = UpdateTravelRequest.builder()
                .title("Updated Trip to Paris")
                .totalBudget(new BigDecimal("2500.00"))
                .build();

        Travel updatedTravel = Travel.builder()
                .id(travelId)
                .title("Updated Trip to Paris")
                .description("A wonderful trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 10))
                .status(Travel.TravelStatus.DRAFT)
                .totalBudget(new BigDecimal("2500.00"))
                .currency("EUR")
                .createdBy(userId)
                .destinations(new ArrayList<>())
                .accommodations(new ArrayList<>())
                .transportations(new ArrayList<>())
                .build();

        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));
        when(travelRepository.save(any(Travel.class))).thenReturn(updatedTravel);

        TravelDto result = travelService.updateTravel(travelId, request, userId, false);

        assertThat(result.getTitle()).isEqualTo("Updated Trip to Paris");
        assertThat(result.getTotalBudget()).isEqualByComparingTo(new BigDecimal("2500.00"));
        verify(travelRepository).findById(travelId);
        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void updateTravel_asAdmin_allowsUpdateOnOtherUserTravel() {
        Long adminUserId = 99L;
        UpdateTravelRequest request = UpdateTravelRequest.builder()
                .title("Admin Updated")
                .build();

        Travel updatedTravel = Travel.builder()
                .id(travelId)
                .title("Admin Updated")
                .description("A wonderful trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 10))
                .status(Travel.TravelStatus.DRAFT)
                .totalBudget(new BigDecimal("2000.00"))
                .currency("EUR")
                .createdBy(userId)
                .destinations(new ArrayList<>())
                .accommodations(new ArrayList<>())
                .transportations(new ArrayList<>())
                .build();

        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));
        when(travelRepository.save(any(Travel.class))).thenReturn(updatedTravel);

        TravelDto result = travelService.updateTravel(travelId, request, adminUserId, true);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Admin Updated");
    }

    @Test
    void updateTravel_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        UpdateTravelRequest request = UpdateTravelRequest.builder()
                .title("Hacked")
                .build();

        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));

        assertThatThrownBy(() -> travelService.updateTravel(travelId, request, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void deleteTravel_asOwner_deletesSuccessfully() {
        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));

        travelService.deleteTravel(travelId, userId, false);

        verify(travelRepository).delete(travel);
    }

    @Test
    void deleteTravel_asAdmin_deletesOtherUserTravel() {
        Long adminUserId = 99L;
        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));

        travelService.deleteTravel(travelId, adminUserId, true);

        verify(travelRepository).delete(travel);
    }

    @Test
    void deleteTravel_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));

        assertThatThrownBy(() -> travelService.deleteTravel(travelId, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void deleteTravel_nonExistingId_throwsResourceNotFoundException() {
        when(travelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelService.deleteTravel(999L, userId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTravelStatus_asOwner_returnsUpdatedTravelDto() {
        Travel updatedTravel = Travel.builder()
                .id(travelId)
                .title("Trip to Paris")
                .description("A wonderful trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 10))
                .status(Travel.TravelStatus.PLANNED)
                .totalBudget(new BigDecimal("2000.00"))
                .currency("EUR")
                .createdBy(userId)
                .destinations(new ArrayList<>())
                .accommodations(new ArrayList<>())
                .transportations(new ArrayList<>())
                .build();

        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));
        when(travelRepository.save(any(Travel.class))).thenReturn(updatedTravel);

        TravelDto result = travelService.updateTravelStatus(travelId, Travel.TravelStatus.PLANNED, userId, false);

        assertThat(result.getStatus()).isEqualTo(Travel.TravelStatus.PLANNED);
        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void updateTravelStatus_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        when(travelRepository.findById(travelId)).thenReturn(Optional.of(travel));

        assertThatThrownBy(() -> travelService.updateTravelStatus(travelId, Travel.TravelStatus.CANCELLED, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }
}