package uk.gov.cabinetoffice.csl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Role;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@SpringBootTest
@Transactional
@ActiveProfiles("no-redis")
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository repository;

    @Test
    public void shouldSaveRole() {
        long repositoryCount = repository.count();
        Role role = createRole();
        repository.save(role);

        assertThat(repository.count(), equalTo(repositoryCount + 1));
    }

    @Test
    public void shouldReturnFirstByName() {
        Role role = createRole();
        repository.save(role);
        Role actualRole = repository.findFirstByNameEquals("name");

        assertThat(actualRole.getId(), notNullValue());
        assertThat(actualRole.getName(), equalTo("name"));
        assertThat(actualRole.getDescription(), equalTo("description"));
    }

    private Role createRole() {
        return new Role( "name", "description");
    }
}
