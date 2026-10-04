package com.sepisac.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sepisac.backend.dto.CompanyCreateDTO;
import com.sepisac.backend.dto.CompanyResponseDTO;
import com.sepisac.backend.dto.CompanyUpdateDTO;
import com.sepisac.backend.exception.DuplicateResourceException;
import com.sepisac.backend.exception.GlobalExceptionHandler;
import com.sepisac.backend.exception.ResourceNotFoundException;
import com.sepisac.backend.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyController Web Layer Unit Tests")
class CompanyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CompanyController companyController;

    private ObjectMapper objectMapper;
    private UUID testCompanyId;
    private CompanyResponseDTO testCompanyResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(companyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        testCompanyId = UUID.randomUUID();
        testCompanyResponse = new CompanyResponseDTO(
                testCompanyId,
                "SEPI S.A.C.",
                "20123456789",
                "ACTIVE",
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/companies")
    class CreateCompanyEndpointTests {

        @Test
        @DisplayName("Should return 201 Created when request payload is valid")
        void shouldReturn201WhenValid() throws Exception {
            CompanyCreateDTO request = new CompanyCreateDTO("SEPI S.A.C.", "20123456789");

            when(companyService.createCompany(any(CompanyCreateDTO.class), any()))
                    .thenReturn(testCompanyResponse);

            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testCompanyId.toString())))
                    .andExpect(jsonPath("$.businessName", is("SEPI S.A.C.")))
                    .andExpect(jsonPath("$.ruc", is("20123456789")))
                    .andExpect(jsonPath("$.subscriptionStatus", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when businessName is blank")
        void shouldReturn400WhenBusinessNameIsBlank() throws Exception {
            CompanyCreateDTO invalidRequest = new CompanyCreateDTO("", "20123456789");

            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("businessName")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when RUC does not have exactly 11 digits")
        void shouldReturn400WhenRucIsInvalid() throws Exception {
            CompanyCreateDTO invalidRequest = new CompanyCreateDTO("SEPI S.A.C.", "12345");

            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("ruc")));
        }

        @Test
        @DisplayName("Should return 409 Conflict when RUC is already registered")
        void shouldReturn409WhenRucDuplicate() throws Exception {
            CompanyCreateDTO request = new CompanyCreateDTO("SEPI S.A.C.", "20123456789");

            when(companyService.createCompany(any(CompanyCreateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("El RUC 20123456789 ya se encuentra registrado"));

            mockMvc.perform(post("/api/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")))
                    .andExpect(jsonPath("$.message", containsString("20123456789")));
        }
    }

    @Nested
    @DisplayName("GET /api/companies & QUERY /api/companies/search")
    class GetAllCompaniesEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with paged companies")
        void shouldReturn200WithCompaniesList() throws Exception {
            com.sepisac.backend.dto.PageResponseDTO<CompanyResponseDTO> pageResponse =
                    new com.sepisac.backend.dto.PageResponseDTO<>(
                            List.of(testCompanyResponse), 0, 10, 1, 1, true, true
                    );

            when(companyService.getCompaniesPaged(eq(0), eq(10), any(), any(), any())).thenReturn(pageResponse);

            mockMvc.perform(get("/api/companies?page=0&size=10&search=SEPI"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(testCompanyId.toString())))
                    .andExpect(jsonPath("$.content[0].businessName", is("SEPI S.A.C.")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Should return 200 OK for HTTP QUERY /api/companies/search")
        void shouldReturn200ForQueryEndpoint() throws Exception {
            com.sepisac.backend.dto.PageResponseDTO<CompanyResponseDTO> pageResponse =
                    new com.sepisac.backend.dto.PageResponseDTO<>(
                            List.of(testCompanyResponse), 0, 10, 1, 1, true, true
                    );

            when(companyService.queryCompanies(any(com.sepisac.backend.dto.CompanyFilterDTO.class), any()))
                    .thenReturn(pageResponse);

            com.sepisac.backend.dto.CompanyFilterDTO filter =
                    new com.sepisac.backend.dto.CompanyFilterDTO("SEPI", "ACTIVE", 0, 10, "createdAt,desc");

            mockMvc.perform(post("/api/companies/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/companies/{id}")
    class GetCompanyByIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when company is found")
        void shouldReturn200WhenCompanyFound() throws Exception {
            when(companyService.getCompanyById(eq(testCompanyId), any())).thenReturn(testCompanyResponse);

            mockMvc.perform(get("/api/companies/{id}", testCompanyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testCompanyId.toString())))
                    .andExpect(jsonPath("$.businessName", is("SEPI S.A.C.")));
        }

        @Test
        @DisplayName("Should return 404 Not Found when company does not exist")
        void shouldReturn404WhenCompanyNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(companyService.getCompanyById(eq(nonExistentId), any()))
                    .thenThrow(new ResourceNotFoundException("Empresa no encontrada"));

            mockMvc.perform(get("/api/companies/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("Empresa no encontrada")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user has no access to company")
        void shouldReturn403WhenAccessDenied() throws Exception {
            UUID otherCompanyId = UUID.randomUUID();
            when(companyService.getCompanyById(eq(otherCompanyId), any()))
                    .thenThrow(new AccessDeniedException("Acceso denegado"));

            mockMvc.perform(get("/api/companies/{id}", otherCompanyId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    @Nested
    @DisplayName("PUT /api/companies/{id}")
    class UpdateCompanyEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when update payload is valid")
        void shouldReturn200WhenValid() throws Exception {
            CompanyUpdateDTO request = new CompanyUpdateDTO("SEPI INGENIERIA S.A.C.", "20123456789", "ACTIVE");
            CompanyResponseDTO updatedResponse = new CompanyResponseDTO(
                    testCompanyId, "SEPI INGENIERIA S.A.C.", "20123456789", "ACTIVE", OffsetDateTime.now()
            );

            when(companyService.updateCompany(eq(testCompanyId), any(CompanyUpdateDTO.class), any()))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/companies/{id}", testCompanyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testCompanyId.toString())))
                    .andExpect(jsonPath("$.businessName", is("SEPI INGENIERIA S.A.C.")))
                    .andExpect(jsonPath("$.ruc", is("20123456789")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when RUC is invalid")
        void shouldReturn400WhenRucInvalid() throws Exception {
            CompanyUpdateDTO request = new CompanyUpdateDTO("SEPI", "123", "ACTIVE");

            mockMvc.perform(put("/api/companies/{id}", testCompanyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Should return 409 Conflict when RUC is duplicated")
        void shouldReturn409WhenRucDuplicated() throws Exception {
            CompanyUpdateDTO request = new CompanyUpdateDTO("SEPI", "20999999999", "ACTIVE");

            when(companyService.updateCompany(eq(testCompanyId), any(CompanyUpdateDTO.class), any()))
                    .thenThrow(new DuplicateResourceException("Ya existe una empresa registrada con el RUC 20999999999"));

            mockMvc.perform(put("/api/companies/{id}", testCompanyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("20999999999")));
        }
    }
}
