package com.fooddelivery.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.productservice.config.JwtAuthFilter;
import com.fooddelivery.productservice.config.SecurityConfig;
import com.fooddelivery.productservice.dto.ProductRequest;
import com.fooddelivery.productservice.dto.ProductResponse;
import com.fooddelivery.productservice.entity.Category;
import com.fooddelivery.productservice.exception.GlobalExceptionHandler;
import com.fooddelivery.productservice.exception.ProductNotFoundException;
import com.fooddelivery.productservice.service.ProductService;
import com.fooddelivery.productservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    private ProductResponse sampleResponse() {
        return new ProductResponse(1L, "Burger", "Classic beef burger", new BigDecimal("9.99"), 50, Category.MAIN_COURSE);
    }

    private ProductRequest sampleRequest() {
        return new ProductRequest("Burger", "Classic beef burger", new BigDecimal("9.99"), 50, Category.MAIN_COURSE);
    }

    // --- GET /products ---

    @Test
    void getAllProducts_shouldReturn200WithList() throws Exception {
        when(productService.getAllProducts(null)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Burger"))
                .andExpect(jsonPath("$[0].category").value("MAIN_COURSE"));
    }

    @Test
    void getAllProducts_shouldReturn200WithFilteredListWhenCategoryProvided() throws Exception {
        when(productService.getAllProducts(Category.MAIN_COURSE)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/products").param("category", "MAIN_COURSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("MAIN_COURSE"));
    }

    // --- GET /products/{id} ---

    @Test
    void getProductById_shouldReturn200WhenFound() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger"))
                .andExpect(jsonPath("$.price").value(9.99));
    }

    @Test
    void getProductById_shouldReturn404WhenNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException("Product not found: 99"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found: 99"));
    }

    // --- POST /products ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn201WhenAdmin() throws Exception {
        when(productService.createProduct(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Burger"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":9.99,\"stock\":10,\"category\":\"MAIN_COURSE\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /products/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn200WhenAdminAndFound() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn404WhenNotFound() throws Exception {
        when(productService.updateProduct(eq(99L), any()))
                .thenThrow(new ProductNotFoundException("Product not found: 99"));

        mockMvc.perform(put("/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /products/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturn204WhenAdmin() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProduct_shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isForbidden());
    }
}