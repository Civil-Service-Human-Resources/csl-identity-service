package uk.gov.cabinetoffice.csl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class CslIdentityServiceTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void testApplicationContextActive() {
        assertTrue(context.isActive());
    }
}
