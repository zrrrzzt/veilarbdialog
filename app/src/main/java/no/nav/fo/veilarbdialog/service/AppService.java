package no.nav.fo.veilarbdialog.service;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class AppService {

    private final AktoerConsumer aktoerConsumer;
    private final DialogDAO dialogDAO;
    private final PepClient pepClient;

    public AppService(AktoerConsumer aktoerConsumer, DialogDAO dialogDAO, PepClient pepClient) {
        this.aktoerConsumer = aktoerConsumer;
        this.dialogDAO = dialogDAO;
        this.pepClient = pepClient;
    }

    public List<DialogData> hentDialogerForBruker(String ident) {
        sjekkTilgangTilFnr(ident);
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        long dialogId = dialogDAO.opprettDialog(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.opprettHenvendelse(henvendelseData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData hentDialog(long dialogId) {
        DialogData dialogData = hentDialogUtenTilgangskontroll(dialogId);
        sjekkLeseTilgangTilDialog(dialogData);
        return dialogData;
    }

    private DialogData hentDialogUtenTilgangskontroll(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    public DialogData markerDialogSomLestAvVeileder(long dialogId) {
        sjekkLeseTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        sjekkLeseTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId).map(this::sjekkLeseTilgangTilDialog);
    }

    public String hentAktoerIdForIdent(String ident) {
        // NB: ingen tilgangskontroll på dette oppslaget
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt, int pageSize) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        return dialogDAO.hentAktorerMedEndringerFOM(tidspunkt, pageSize);
    }

    public void settDialogerTilHistoriske(AvsluttetOppfolgingFeedDTO element) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentGjeldendeDialogerForAktorId(element.getAktoerid())
                .forEach(dialog -> {
                    val dialogStatus = DialogStatus.builder()
                            .dialogId(dialog.getId())
                            .ferdigbehandlet(true)
                            .venterPaSvar(false)
                            .build();
                    if (dialog.erUbehandlet()) {
                        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
                    }
                    if (dialog.venterPaSvar()) {
                        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
                    }
                    dialogDAO.settDialogTilHistoriskOgOppdaterFeed(dialog);
                });
    }

    private String sjekkTilgangTilFnr(String ident) {
        return pepClient.sjekkLeseTilgangTilFnr(ident);
    }

    private void sjekkTilgangTilAktorId(String aktorId) {
        sjekkTilgangTilFnr(aktoerConsumer.hentIdentForAktorId(aktorId).orElseThrow(IngenTilgang::new));
    }

    private DialogData sjekkLeseTilgangTilDialog(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        return dialogData;
    }

    private void sjekkLeseTilgangTilDialog(long id) {
        hentDialog(id);
    }

    private void sjekkSkriveTilgangTilDialog(long id) {
        DialogData dialogData = hentDialog(id);
        if (dialogData.isHistorisk()) {
            throw new UlovligHandling();
        }
    }

}
