package com.travelplan.travel.controller;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
import com.travelplan.travel.dto.CreateTravelRequest;
import com.travelplan.travel.dto.TravelDto;
import com.travelplan.travel.dto.UpdateTravelRequest;
import com.travelplan.travel.entity.Travel;
import com.travelplan.travel.service.TravelService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelControllerTest {

    @Mock
    private TravelService travelService;

    @InjectMocks
    private TravelController travelController;

    private UserContext adminUser;
    private UserContext regularUser;
    private TravelDto travelDto;

    @BeforeEach
    void setUp() {
        adminUser = UserContext.builder().userId(1L).role("ADMIN").build();
        regularUser = UserContext.builder().userId(2L).role("USER").build();

        travelDto = TravelDto.builder()
                .id(100L)
                .title("Paris")
                .createdBy(2L)
                .status(Travel.TravelStatus.DRAFT)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .totalBudget(new BigDecimal("1000"))
                .build();
    }

    @Test
    void getAllTravels_adminUsesProvidedUserIdFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TravelDto> page = new PageImpl<>(List.of(travelDto), pageable, 1);
        when(travelService.getAllTravels(99L, Travel.TravelStatus.DRAFT, "paris", pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<TravelDto>>> response = travelController.getAllTravels(
                99L, Travel.TravelStatus.DRAFT, "paris", adminUser, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getPageInfo()).isNotNull();
        verify(travelService).getAllTravels(99L, Travel.TravelStatus.DRAFT, "paris", pageable);
    }

    @Test
    void getAllTravels_nonAdminForcesOwnUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TravelDto> page = new PageImpl<>(List.of(travelDto), pageable, 1);
        when(travelService.getAllTravels(2L, null, null, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<TravelDto>>> response = travelController.getAllTravels(
                99L, null, null, regularUser, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(travelService).getAllTravels(2L, null, null, pageable);
    }

    @Test
    void getTravelById_whenOwner_returnsOk() {
        when(travelService.getTravelById(100L)).thenReturn(travelDto);

        ResponseEntity<ApiResponse<TravelDto>> response = travelController.getTravelById(100L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    void getTravelById_whenNotOwnerAndNotAdmin_returnsForbidden() {
        UserContext anotherUser = UserContext.builder().userId(50L).role("USER").build();
        when(travelService.getTravelById(100L)).thenReturn(travelDto);

        ResponseEntity<ApiResponse<TravelDto>> response = travelController.getTravelById(100L, anotherUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void getMyTravels_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TravelDto> page = new PageImpl<>(List.of(travelDto), pageable, 1);
        when(travelService.getUserTravels(2L, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<TravelDto>>> response = travelController.getMyTravels(regularUser, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        verify(travelService).getUserTravels(2L, pageable);
    }

    @Test
    void createTravel_returnsCreated() {
        CreateTravelRequest request = CreateTravelRequest.builder()
                .title("New Trip")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 10))
                .build();
        when(travelService.createTravel(request, 2L)).thenReturn(travelDto);

        ResponseEntity<ApiResponse<TravelDto>> response = travelController.createTravel(request, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("created");
    }

    @Test
    void updateTravel_returnsOk() {
        UpdateTravelRequest request = UpdateTravelRequest.builder().title("Updated").build();
        when(travelService.updateTravel(100L, request, 2L, false)).thenReturn(travelDto);

        ResponseEntity<ApiResponse<TravelDto>> response = travelController.updateTravel(100L, request, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("updated");
    }

    @Test
    void deleteTravel_returnsOk() {
        ResponseEntity<ApiResponse<Void>> response = travelController.deleteTravel(100L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("deleted");
        verify(travelService).deleteTravel(100L, 2L, false);
    }

    @Test
    void updateTravelStatus_returnsOk() {
        when(travelService.updateTravelStatus(100L, Travel.TravelStatus.PLANNED, 2L, false)).thenReturn(travelDto);

        ResponseEntity<ApiResponse<TravelDto>> response =
                travelController.updateTravelStatus(100L, Travel.TravelStatus.PLANNED, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("status");
    }

    @Test
    void getTravelStats_returnsAggregatedStats() {
        when(travelService.countTravelsByStatus(Travel.TravelStatus.DRAFT)).thenReturn(1L);
        when(travelService.countTravelsByStatus(Travel.TravelStatus.PLANNED)).thenReturn(2L);
        when(travelService.countTravelsByStatus(Travel.TravelStatus.BOOKED)).thenReturn(3L);
        when(travelService.countTravelsByStatus(Travel.TravelStatus.IN_PROGRESS)).thenReturn(4L);
        when(travelService.countTravelsByStatus(Travel.TravelStatus.COMPLETED)).thenReturn(5L);
        when(travelService.countTravelsByStatus(Travel.TravelStatus.CANCELLED)).thenReturn(6L);

        ResponseEntity<ApiResponse<TravelController.TravelStats>> response = travelController.getTravelStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCompleted()).isEqualTo(5L);
    }
}
