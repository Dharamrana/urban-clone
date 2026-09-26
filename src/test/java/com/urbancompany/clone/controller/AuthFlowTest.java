package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceRepository;
import com.urbancompany.clone.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceRepository serviceRepository;

    private MockHttpSession signup(String name, String email) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"email\":\"" + email
                + "\",\"phone\":\"9111111111\",\"password\":\"secret123\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains(email));
        assertFalse(content.contains("secret123"), "password hash must never leak in responses");
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void signupHashesPasswordAndStartsSession() throws Exception {
        MockHttpSession session = signup("Asha", "asha@test.com");
        assertNotNull(session);

        User saved = userService.getUserByEmail("asha@test.com").orElseThrow();
        assertNotEquals("secret123", saved.getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateSignupRejected() throws Exception {
        signup("Bina", "bina@test.com");
        String body = "{\"name\":\"Bina Two\",\"email\":\"bina@test.com\",\"phone\":\"9222222222\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingPagesRequireLogin() throws Exception {
        mockMvc.perform(get("/request"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void bookingsAreBoundAndIsolatedPerUser() throws Exception {
        MockHttpSession sessionA = signup("User A", "a@test.com");
        MockHttpSession sessionB = signup("User B", "b@test.com");

        com.urbancompany.clone.model.Service svc = serviceRepository.save(
                new com.urbancompany.clone.model.Service(null, "Auth Cleaning", "desc", 499.0, null, null, true));

        String tomorrow = LocalDate.now().plusDays(1).toString();
        String booking = "{\"service\":{\"id\":" + svc.getId() + "},\"address\":\"A street\","
                + "\"scheduledDate\":\"" + tomorrow + "\",\"scheduledSlot\":\"10:00-12:00\","
                + "\"paymentMethod\":\"UPI\","
                + "\"user\":{\"name\":\"User A\",\"phone\":\"9111111111\",\"email\":\"a@test.com\"}}";

        MvcResult created = mockMvc.perform(post("/api/requests").session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(created.getResponse().getContentAsString().contains("A street"));

        // A sees it, B does not (API + page).
        String listA = mockMvc.perform(get("/api/requests").session(sessionA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String listB = mockMvc.perform(get("/api/requests").session(sessionB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(listA.contains("A street"));
        assertFalse(listB.contains("A street"));

        String pageB = mockMvc.perform(get("/requests").session(sessionB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(pageB.contains("A street"));

        // Anonymous booking API is locked (UC requires login).
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isUnauthorized());
    }
}
