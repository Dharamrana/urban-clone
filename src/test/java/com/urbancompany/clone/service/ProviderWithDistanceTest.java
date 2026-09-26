package com.urbancompany.clone.service;

import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.Location;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProviderWithDistanceTest {

    @Test
    void testProviderWithDistanceCreation() {
        ServiceProvider provider = new ServiceProvider();
        provider.setId(1L);
        provider.setName("Test Provider");
        ProviderWithDistance pd = new ProviderWithDistance(provider, 5.5);
        assertEquals(5.5, pd.getDistance());
        assertNotNull(pd.getProvider());
        assertEquals("Test Provider", pd.getProvider().getName());
    }
}
