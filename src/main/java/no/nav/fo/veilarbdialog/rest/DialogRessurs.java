package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.toIntExact;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Transactional
@RestController
@RequestMapping(
        value = "/api/dialog",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class DialogRessurs {

    private final DialogDataService dialogDataService;
    private final RestMapper restMapper;
    private final HttpServletRequest httpServletRequest;
    private final KontorsperreFilter kontorsperreFilter;
    private final IAuthService auth;

    private void sjekkErInternbruker() {
        if (!auth.erInternBruker())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bare internbrukere tillatt");
    }

    @GetMapping
    public List<DialogDTO> hentDialoger() {
        return dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(kontorsperreFilter::tilgangTilEnhet)
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @GetMapping("sistOppdatert")
    public SistOppdatertDTO sistOppdatert() {
        var oppdatert = dialogDataService.hentSistOppdatertForBruker(getContextUserIdent(), auth.getLoggedInnUser());
        return new SistOppdatertDTO(oppdatert == null ? null : oppdatert.getTime());
    }

    @GetMapping("antallUleste")
    public AntallUlesteDTO antallUleste() {

        long antall = dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(auth.erEksternBruker() ? DialogData::erUlestForBruker : DialogData::erUlestAvVeileder)
                .filter(it -> !it.isHistorisk())
                .count();
        return new AntallUlesteDTO(toIntExact(antall));

    }

    @GetMapping("{dialogId}")
    public DialogDTO hentDialog(@PathVariable String dialogId) {
        return Optional.ofNullable(dialogId)
                .map(Long::parseLong)
                .map(dialogDataService::hentDialogMedTilgangskontroll)
                .map(restMapper::somDialogDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public DialogDTO nyHenvendelse(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        Person bruker = getContextUserIdent();
        var dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, bruker);
        if (nyHenvendelseDTO.getVenterPaaSvarFraNav() != null) {
            dialogData = dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.getId(), !nyHenvendelseDTO.getVenterPaaSvarFraNav() );
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        if (nyHenvendelseDTO.getVenterPaaSvarFraBruker() != null) {
            var dialogStatus = DialogStatus.builder()
                    .dialogId(dialogData.getId())
                    .venterPaSvar(nyHenvendelseDTO.getVenterPaaSvarFraBruker())
                    .build();

            dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        return kontorsperreFilter.tilgangTilEnhet(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;

    }

    @PutMapping("{dialogId}/les")
    @Transactional
    public DialogDTO markerSomLest(@PathVariable String dialogId) {
        var dialogData = dialogDataService.markerDialogSomLest(Long.parseLong(dialogId));
        return kontorsperreFilter.tilgangTilEnhet(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    @PutMapping("{dialogId}/venter_pa_svar/{venter}")
    public DialogDTO oppdaterVenterPaSvar(@PathVariable String dialogId, @PathVariable boolean venter) {

        var dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        var dialog = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        dialogDataService.sendPaaKafka(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PutMapping("{dialogId}/ferdigbehandlet/{ferdigbehandlet}")
    public DialogDTO oppdaterFerdigbehandlet(@PathVariable String dialogId, @PathVariable boolean ferdigbehandlet) {
        sjekkErInternbruker();

        var dialog = dialogDataService.oppdaterFerdigbehandletTidspunkt(Long.parseLong(dialogId), ferdigbehandlet);
        dialogDataService.sendPaaKafka(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PostMapping("forhandsorientering")
    public DialogDTO forhandsorienteringPaAktivitet(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        sjekkErInternbruker();
        var aktorId = dialogDataService.hentAktoerIdForPerson(getContextUserIdent());
        auth.sjekkTilgangTilPerson(aktorId);

        var dialog = dialogDataService.hentDialogMedTilgangskontroll(nyHenvendelseDTO.getDialogId(),
               AktivitetId.of(nyHenvendelseDTO.getAktivitetId()));
        if (dialog == null) dialog = dialogDataService.opprettDialog(nyHenvendelseDTO, aktorId.get());

        long dialogId = dialog.getId();
        dialogDataService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        dialogDataService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
    }


    private Person getContextUserIdent() {
        if (auth.erEksternBruker()) {
            var user = auth.getLoggedInnUser();
            return Person.fnr(user.get());
        }
        Optional<Person> fnr = Optional
                .ofNullable(httpServletRequest.getParameter("fnr"))
                .map(Person::fnr);
        Optional<Person> aktorId = Optional
                .ofNullable(httpServletRequest.getParameter("aktorId"))
                .map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }
}
