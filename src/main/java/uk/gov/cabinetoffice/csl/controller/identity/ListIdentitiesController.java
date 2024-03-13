package uk.gov.cabinetoffice.csl.controller.identity;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.BatchProcessResponse;
import uk.gov.cabinetoffice.csl.dto.IdentityAgencyDTO;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.UidList;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.IdentityService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
public class ListIdentitiesController {

    private final IdentityRepository identityRepository;
    private final IdentityService identityService;

    @Autowired
    public ListIdentitiesController(IdentityRepository identityRepository, IdentityService identityService) {
        this.identityRepository = identityRepository;
        this.identityService = identityService;
    }

    @PostMapping("/api/identities/remove-reporting-roles")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public BatchProcessResponse removeAdminAccessFromUsers(@RequestBody @Valid UidList uids) {
        return identityService.removeReportingRoles(uids.getUids());
    }

    @GetMapping("/api/identities")
    public ResponseEntity<List<IdentityDTO>> listIdentities() {

        return ResponseEntity.ok(StreamSupport.stream(identityRepository.findAll().spliterator(), false)
                .map(IdentityDTO::new)
                .collect(toList()));
    }

    @GetMapping("/api/identities/map")
    public ResponseEntity<Map<String, IdentityDTO>> listIdentitiesAsMap() {
        return ResponseEntity.ok(identityRepository.findAllNormalised().stream().collect(Collectors.toMap(o -> o.getUid(), o -> o)));
    }

    @GetMapping(value ="/api/identities/map-for-uids", params = "uids")
    public ResponseEntity<Map<String, IdentityDTO>> listIdentitiesAsMapForUids(@RequestParam List<String> uids) {
        return ResponseEntity.ok(
                identityRepository
                        .findIdentitiesByUidsNormalised(uids)
                        .stream()
                        .collect(Collectors.toMap(IdentityDTO::getUid, o -> o)));
    }

    @GetMapping(value = "/api/identities", params = "emailAddress")
    public ResponseEntity<IdentityDTO> findByEmailAddress(@RequestParam String emailAddress) {

        Identity identity = identityRepository.findFirstByActiveTrueAndEmailEqualsIgnoreCase(emailAddress);
        if (identity != null) {
            return ResponseEntity.ok(new IdentityDTO(identity));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/api/identities", params = "uid")
    public ResponseEntity<IdentityDTO> findByUid(@RequestParam String uid) {
        Optional<Identity> identity = identityRepository.findFirstByUid(uid);
        return identity
                .map(i -> ResponseEntity.ok(new IdentityDTO(i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/identity/agency/{uid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdentityAgencyDTO> findAgencyTokenUidByUid(@PathVariable String uid) {
        log.info("Getting agency token uid for user with uid " + uid);
        try {
            Optional<Identity> identity = identityRepository.findFirstByUid(uid);
            return identity
                    .map(i -> buildResponse(i))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<IdentityAgencyDTO> buildResponse(Identity i) {
        IdentityAgencyDTO responseDTO = new IdentityAgencyDTO();
        responseDTO.setAgencyTokenUid(i.getAgencyTokenUid());
        responseDTO.setUid(i.getUid());
        return ResponseEntity.ok(responseDTO);
    }
}
