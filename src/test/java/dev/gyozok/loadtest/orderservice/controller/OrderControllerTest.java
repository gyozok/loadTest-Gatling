package dev.gyozok.loadtest.orderservice.controller;

import dev.gyozok.loadtest.orderservice.dto.OrderRequest;
import dev.gyozok.loadtest.orderservice.dto.OrderResponse;
import dev.gyozok.loadtest.orderservice.model.OrderStatus;
import dev.gyozok.loadtest.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_returns202WithLocationHeader() throws Exception {
        // GIVEN
        OrderRequest request = new OrderRequest("widget", 2);
        OrderResponse response = new OrderResponse(1L, "widget", 2, OrderStatus.PENDING, Instant.now());
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(response);

        // WHEN & THEN
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/orders/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.item").value("widget"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_withBlankItem_returns400WithFieldError() throws Exception {
        // GIVEN
        OrderRequest invalidRequest = new OrderRequest("", 2);

        // WHEN & THEN
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("item"));
    }

    @Test
    void createOrder_withQuantityBelowMinimum_returns400() throws Exception {
        // GIVEN
        OrderRequest invalidRequest = new OrderRequest("widget", 0);

        // WHEN & THEN
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
    }

    @Test
    void getOrder_returns200WhenFound() throws Exception {
        // GIVEN
        OrderResponse response = new OrderResponse(5L, "widget", 3, OrderStatus.CONFIRMED, Instant.now());
        when(orderService.getOrder(5L)).thenReturn(Optional.of(response));

        // WHEN & THEN
        mockMvc.perform(get("/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getOrder_returns404WhenNotFound() throws Exception {
        // GIVEN
        when(orderService.getOrder(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_withNonNumericId_returns400() throws Exception {
        // GIVEN

        // WHEN & THEN
        mockMvc.perform(get("/orders/not-a-number"))
                .andExpect(status().isBadRequest());

        verify(orderService, org.mockito.Mockito.never()).getOrder(any());
    }
}
