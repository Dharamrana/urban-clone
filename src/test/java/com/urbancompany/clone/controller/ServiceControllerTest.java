package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceService serviceService;

    @Test
    void shouldReturnServices() throws Exception {
        List<Service> services = Arrays.asList(
                new Service(1L, "Carpenter", "Furniture repair", 499.0, "icon_url", null, true),
                new Service(2L, "Electrician", "Electrical work", 399.0, "icon_url", null, true)
        );
        when(serviceService.getAllActiveServices()).thenReturn(services);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/services"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Carpenter"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Electrician"));
    }
}
