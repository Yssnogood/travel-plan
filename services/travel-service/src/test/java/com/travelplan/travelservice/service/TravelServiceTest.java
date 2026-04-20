package com.travelplan.travelservice.service;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.travelservice.dto.CreateTravelRequest;
import com.travelplan.travelservice.dto.TravelDto;
import com.travelplan.travelservice.dto.UpdateTravelRequest;
import com.travelplan.travelservice.entity.Travel;
import com.travelplan.travelservice.entity.TravelStatus;
import com.travelplan.travelservice.repository.TravelRepository;
import com.travelplan.travelservice.repository.DestinationRepository;
import com.travelplan.shared.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private TravelService travelService;

    private Travel testTravel;
    private CreateTravelRequest createRequest;
    private UpdateTravelRequest updateRequest;

    @BeforeEach
    void setUp() {
        testTravel = new Travel();
        testTravel.setId(1L);
        testTravel.setUserId(1L);
        testTravel.setTitle("Trip to Paris");
        testTravel.setDescription("A wonderful trip to Paris");
        testTravel.setStartDate(LocalDate.now().plusDays(30));
        testTravel.setEndDate(LocalDate.now().plusDays(37));
        testTravel.setBudget(new BigDecimal("2500.00"));
        testTravel.setCurrency("EUR");
        testTravel.setStatus(TravelStatus.PLANNED);
        testTravel.setDestinations(new ArrayList<>());
        testTravel.setActivities(new ArrayList<>());
        testTravel.setCreatedAt(LocalDateTime.now());
        testTravel.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateTravelRequest();
        createRequest.setUserId(1L);
        createRequest.setTitle("New Travel");
        createRequest.setStartDate(LocalDate.now().plusDays(60));
        createRequest.setEndDate(LocalDate.now().plusDays(67));
        createRequest.setBudget(new BigDecimal("3000.00"));

        updateRequest = new UpdateTravelRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setBudget(new BigDecimal("3500.00"));
    }

    @Test
    void getAllTravels_ShouldReturnPagedTravels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Travel> travelPage = new PageImpl<>(List.of(testTravel), pageable, 1);
        
        when(travelRepository.findAll(pageable)).thenReturn(travelPage);

        ApiResponse<List<TravelDto>> response = travelService.getAllTravels(0, 10, null, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTitle()).isEqualTo("Trip to Paris");
        verify(travelRepository).findAll(pageable);
    }

    @Test
    void getTravelById_WhenTravelExists_ShouldReturnTravel() {
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));

        ApiResponse<TravelDto> response = travelService.getTravelById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getTitle()).isEqualTo("Trip to Paris");
    }

    @Test
    void getTravelById_WhenTravelNotFound_ShouldThrowException() {
        when(travelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelService.getTravelById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Travel not found");
    }

    @Test
    void createTravel_ShouldCreateAndReturnTravel() {
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> {
            Travel travel = invocation.getArgument(0);
            travel.setId(2L);
            return travel;
        });

        ApiResponse<TravelDto> response = travelService.createTravel(createRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTitle()).isEqualTo("New Travel");
        assertThat(response.getData().getStatus()).isEqualTo(TravelStatus.DRAFT);
        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void updateTravel_ShouldUpdateAndReturnTravel() {
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        when(travelRepository.save(any(Travel.class))).thenReturn(testTravel);

        ApiResponse<TravelDto> response = travelService.updateTravel(1L, updateRequest);

        assertThat(response.isSuccess()).isTrue();
        verify(travelRepository).save(any(Travel.class));
    }

    @Test
    void updateTravelStatus_ShouldUpdateStatus() {
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        when(travelRepository.save(any(Travel.class))).thenReturn(testTravel);

        ApiResponse<TravelDto> response = travelService.updateTravelStatus(1L, TravelStatus.IN_PROGRESS);

        assertThat(response.isSuccess()).isTrue();
        verify(travelRepository).save(argThat(travel -> travel.getStatus() == TravelStatus.IN_PROGRESS));
    }

    @Test
    void deleteTravel_ShouldCallRepositoryDelete() {
        when(travelRepository.findById(1L)).thenReturn(Optional.of(testTravel));
        doNothing().when(travelRepository).delete(any(Travel.class));

        ApiResponse<Void> response = travelService.deleteTravel(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(travelRepository).delete(testTravel);
    }

    @Test
    void getTravelsByUserId_ShouldReturnUserTravels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Travel> travelPage = new PageImpl<>(List.of(testTravel), pageable, 1);
        
        when(travelRepository.findByUserId(1L, pageable)).thenReturn(travelPage);

        ApiResponse<List<TravelDto>> response = travelService.getAllTravels(0, 10, 1L, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(travelRepository).findByUserId(1L, pageable);
    }

    @Test
    void createTravel_WithInvalidDates_ShouldThrowException() {
        createRequest.setEndDate(LocalDate.now().plusDays(30)); // End before start
        createRequest.setStartDate(LocalDate.now().plusDays(60));

        assertThatThrownBy(() -> travelService.createTravel(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
    }
}
