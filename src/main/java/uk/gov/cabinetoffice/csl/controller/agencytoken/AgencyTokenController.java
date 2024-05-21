package uk.gov.cabinetoffice.csl.controller.agencytoken;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.service.AgencyTokenCapacityService;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/agency")
public class AgencyTokenController {


    private AgencyTokenCapacityService agencyTokenCapacityService;

    @GetMapping("/{uid}")
    public ResponseEntity<AgencyToken> getSpacesUsedForAgencyToken(
            @PathVariable(value = "uid") String uid) {
        log.debug("Getting spaces used for agency token {}", uid);
        try {
            return ResponseEntity.ok(agencyTokenCapacityService.getSpacesUsedByAgencyToken(uid));
        } catch (Exception e) {
            log.error("Unexpected error calling getSpacesUsedForAgencyToken with uid: {}, {}", uid, e.toString());
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity deleteAgencyToken(@PathVariable(value = "uid") String uid) {
        log.debug("Deleting agency token {}", uid);
        try {
            agencyTokenCapacityService.deleteAgencyToken(uid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Unexpected error calling deleteAgencyToken with uid: {}, {}", uid, e.toString());
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }
    }
}
