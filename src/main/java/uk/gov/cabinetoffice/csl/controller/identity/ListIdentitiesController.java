package uk.gov.cabinetoffice.csl.controller.identity;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.BatchProcessResponse;
import uk.gov.cabinetoffice.csl.dto.IdentityAgencyDTO;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.UidList;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.service.IdentityService;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.*;

@Slf4j
@AllArgsConstructor
@RestController
public class ListIdentitiesController {

    private final IdentityService identityService;

    @PostMapping("/api/identities/remove-reporting-roles")
    @ResponseBody
    @ResponseStatus(OK)
    public BatchProcessResponse removeAdminAccessFromUsers(@RequestBody @Valid UidList uids) {
        return identityService.removeReportingRoles(uids.getUids());
    }

    @GetMapping("/api/identities")
    public ResponseEntity<List<IdentityDTO>> listIdentities() {
        return ok(
                identityService.getAllIdentities()
                .stream()
                .map(IdentityDTO::new)
                .collect(toList())
        );
    }

    @GetMapping("/api/identities/map")
    public ResponseEntity<Map<String, IdentityDTO>> listIdentitiesAsMap() {
        return ok(
                identityService.getAllNormalisedIdentities()
                .stream()
                .collect(toMap(IdentityDTO::getUid, o -> o))
        );
    }

    @GetMapping(value ="/api/identities/map-for-uids", params = "uids")
    public ResponseEntity<Map<String, IdentityDTO>> listIdentitiesAsMapForUids(@RequestParam List<String> uids) {
        return ok(
                identityService.getIdentitiesByUidsNormalised(uids)
                .stream()
                .collect(toMap(IdentityDTO::getUid, o -> o)));
    }

    @GetMapping(value = "/api/identities", params = "emailAddress")
    public ResponseEntity<IdentityDTO> findByEmailAddress(@RequestParam String emailAddress) {
        Identity identity = identityService.getActiveIdentityForEmail(emailAddress);
        if (identity != null) {
            return ok(new IdentityDTO(identity));
        }
        return notFound().build();
    }

    @GetMapping(value = "/api/identities", params = "uid")
    public ResponseEntity<IdentityDTO> findByUid(@RequestParam String uid) {
        try {
            Identity identity = identityService.getIdentityForUid(uid);
            return ok(new IdentityDTO(identity));
        } catch(IdentityNotFoundException e) {
            return notFound().build();
        } catch (Exception e) {
            return status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/api/identity/agency/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdentityAgencyDTO> findAgencyTokenUidByUid(@PathVariable String uid) {
        log.info("Getting agency token uid for identity with uid " + uid);
        try {
            Identity identity = identityService.getIdentityForUid(uid);
            return ok(new IdentityAgencyDTO(identity.getUid(), identity.getAgencyTokenUid()));
        } catch(IdentityNotFoundException e) {
            return notFound().build();
        } catch (Exception e) {
            return status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
