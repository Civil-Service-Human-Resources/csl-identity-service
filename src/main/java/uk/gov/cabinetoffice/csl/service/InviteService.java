package uk.gov.cabinetoffice.csl.service;

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
        return isInviteCodeExpired(invite);
    }

    public boolean isInviteCodeExpired(Invite invite) {
        long diffInMs = new Date().getTime() - invite.getInvitedAt().getTime();

        if (diffInMs > validityInSeconds * 1000) {
            return true;
        }
        return false;
    }

    public void updateInviteByCode(String code, InviteStatus newStatus) {
        Invite invite = inviteRepository.findByCode(code);
        invite.setStatus(newStatus);
        inviteRepository.save(invite);
    }

    public void createNewInviteForEmailAndRoles(String email, Set<Role> roleSet, Identity inviter)
            throws NotificationClientException {
        Invite invite = inviteFactory.create(email, roleSet, inviter);

        notifyService.notify(invite.getForEmail(), invite.getCode(), govNotifyInviteTemplateId, signupUrlFormat);

        inviteRepository.save(invite);
    }

    public void sendSelfSignupInvite(String email, boolean isAuthorisedInvite) throws NotificationClientException {
        Invite invite = inviteFactory.createSelfSignUpInvite(email);
        invite.setAuthorisedInvite(isAuthorisedInvite);

        notifyService.notify(invite.getForEmail(), invite.getCode(), govNotifyInviteTemplateId, signupUrlFormat);

        inviteRepository.save(invite);
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
