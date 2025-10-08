package com.bookstack.bookstack.borrow.controller;

import com.bookstack.bookstack.auth.service.JwtService;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.service.BorrowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BorrowControllerTest {

    @Mock
    private BorrowService borrowService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private BorrowController borrowController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private BorrowResponse sampleBorrowResponse;
    private List<BorrowResponse> sampleBorrowResponses;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(borrowController).build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleBorrowResponse = new BorrowResponse();
        sampleBorrowResponse.setId(1L);
        sampleBorrowResponse.setUserId(1L);
        sampleBorrowResponse.setBookId(1L);
        sampleBorrowResponse.setUserName("john_doe");
        sampleBorrowResponse.setBookTitle("The Great Gatsby");
        sampleBorrowResponse.setBorrowDate(LocalDateTime.now());
        sampleBorrowResponse.setDueDate(LocalDateTime.now().plusDays(14));
        sampleBorrowResponse.setStatus(BorrowStatus.ACTIVE);
        sampleBorrowResponse.setNotes("Sample borrow");

        sampleBorrowResponses = Arrays.asList(sampleBorrowResponse);
    }

    @Test
    void borrowBook_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        when(borrowService.borrowBook(anyLong(), anyLong(), anyString())).thenReturn(sampleBorrowResponse);

        String requestBody = """
            {
                "userId": 1,
                "bookId": 1,
                "notes": "Sample borrow"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.bookId").value(1L))
                .andExpect(jsonPath("$.userName").value("john_doe"))
                .andExpect(jsonPath("$.bookTitle").value("The Great Gatsby"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnBook_WithValidRequest_ShouldReturnOk() throws Exception {
        // Given
        BorrowResponse returnedBorrow = new BorrowResponse();
        returnedBorrow.setId(1L);
        returnedBorrow.setUserId(1L);
        returnedBorrow.setBookId(1L);
        returnedBorrow.setStatus(BorrowStatus.RETURNED);
        returnedBorrow.setReturnDate(LocalDateTime.now());
        returnedBorrow.setNotes("Book returned in good condition");

        when(borrowService.returnBook(anyLong(), anyString())).thenReturn(returnedBorrow);

        String requestBody = """
            {
                "notes": "Book returned in good condition"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/borrows/1/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.notes").value("Book returned in good condition"));
    }

    @Test
    void getAllBorrows_ShouldReturnBorrowsList() throws Exception {
        // Given
        when(borrowService.getAllBorrows()).thenReturn(sampleBorrowResponses);

        // When & Then
        mockMvc.perform(get("/api/borrows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userName").value("john_doe"))
                .andExpect(jsonPath("$[0].bookTitle").value("The Great Gatsby"));
    }

    @Test
    void getBorrowById_WithValidId_ShouldReturnBorrow() throws Exception {
        // Given
        when(borrowService.getBorrowById(1L)).thenReturn(sampleBorrowResponse);

        // When & Then
        mockMvc.perform(get("/api/borrows/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.bookId").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}