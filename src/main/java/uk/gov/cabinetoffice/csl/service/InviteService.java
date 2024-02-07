package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Invite;
import uk.gov.cabinetoffice.csl.domain.InviteStatus;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.domain.factory.InviteFactory;
import uk.gov.cabinetoffice.csl.repository.InviteRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class InviteService {

    private final String govNotifyInviteTemplateId;
    private final int validityInSeconds;
    private final String signupUrlFormat;
    private final NotifyService notifyService;
    private final InviteRepository inviteRepository;
    private final InviteFactory inviteFactory;

    public InviteService(
            @Value("${govNotify.template.invite}") String govNotifyInviteTemplateId,
            @Value("${invite.validityInSeconds}") int validityInSeconds,
            @Value("${invite.url}") String signupUrlFormat,
            @Qualifier("notifyServiceImpl") NotifyService notifyService,
            @Qualifier("inviteRepository") InviteRepository inviteRepository,
            InviteFactory inviteFactory) {
        this.govNotifyInviteTemplateId = govNotifyInviteTemplateId;
        this.validityInSeconds = validityInSeconds;
        this.signupUrlFormat = signupUrlFormat;
        this.notifyService = notifyService;
        this.inviteRepository = inviteRepository;
        this.inviteFactory = inviteFactory;
    }

    @ReadOnlyProperty
    public Invite findByCode(String code) {
        return inviteRepository.findByCode(code);
    }

    @ReadOnlyProperty
    public Invite findByForEmail(String email) {
        return inviteRepository.findByForEmail(email);
    }

    @ReadOnlyProperty
    public Optional<Invite> findByForEmailAndStatus(String email, InviteStatus status) {
        return inviteRepository.findByForEmailAndStatus(email, status);
    }

    public boolean isCodeExpired(String code) {
        Invite invite = inviteRepository.findByCode(code);
        if(invite == null) {
            log.info("Invite not found for code: {}", code);
            return true;
        }
        return isInviteCodeExpired(invite);
    }

    public boolean isInviteCodeExpired(Invite invite) {
        long diffInMs = new Date().getTime() - invite.getInvitedAt().getTime();
        return diffInMs > validityInSeconds * 1000L;
    }

    public void updateInviteByCode(String code, InviteStatus newStatus) {
        Invite invite = inviteRepository.findByCode(code);
        invite.setStatus(newStatus);
        if(InviteStatus.ACCEPTED.equals(newStatus)) {
            invite.setAcceptedAt(new Date());
        }
        inviteRepository.save(invite);
    }

    public void createNewInviteForEmailAndRoles(String email, Set<Role> roleSet, Identity inviter)
            throws NotificationClientException {
        Invite invite = inviteFactory.create(email, roleSet, inviter);
        inviteRepository.save(invite);
        notifyService.notify(invite.getForEmail(), invite.getCode(), govNotifyInviteTemplateId, signupUrlFormat);
    }

    public void sendSelfSignupInvite(String email, boolean isAuthorisedInvite) throws NotificationClientException {
        Invite invite = inviteFactory.createSelfSignUpInvite(email);
        invite.setAuthorisedInvite(isAuthorisedInvite);
        inviteRepository.save(invite);
        notifyService.notify(invite.getForEmail(), invite.getCode(), govNotifyInviteTemplateId, signupUrlFormat);
    }

    public boolean isInviteValid(String code) {
        return inviteRepository.existsByCode(code) && !isCodeExpired(code);
    }

    public boolean isCodeExists(String code) {
        return inviteRepository.existsByCode(code);
    }

    public boolean isEmailInvited(String email) {
        return inviteRepository.existsByForEmailAndInviterIdIsNotNull(email);
    }
}
