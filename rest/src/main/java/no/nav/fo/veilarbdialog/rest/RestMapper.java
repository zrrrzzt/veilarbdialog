package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.domain.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

@Component
class RestMapper {

    public DialogDTO somDialogDTO(DialogData dialogData) {
        List<HenvendelseData> henvendelser = dialogData.henvendelser;
        Optional<HenvendelseData> forsteHenvendelse = henvendelser.stream()
                .sorted(comparing(HenvendelseData::getSendt))
                .findFirst();
        Optional<HenvendelseData> sisteHenvendelse = henvendelser.stream()
                .sorted(comparing(HenvendelseData::getSendt).reversed())
                .findFirst();

        // TODO her gjøres endel masering av data som klienten kanskje burde gjøre selv. Eller?
        return new DialogDTO()
                .setId(Long.toString(dialogData.getId()))
                .setAktivitetId(dialogData.getAktivitetId())
                .setOverskrift(dialogData.overskrift)
                .setHenvendelser(henvendelser.stream()
                        .map(this::somHenvendelseDTO)
                        .collect(toList())
                )
                .setLest(dialogData.lestAvVeileder)
                .setLestAvBrukerTidspunkt(dialogData.lestAvBrukerTidspunkt)
                .setErLestAvBruker(dialogData.lestAvBruker)
                .setFerdigBehandlet(dialogData.ferdigbehandlet)
                .setVenterPaSvar(dialogData.venterPaSvar)
                .setSisteDato(sisteHenvendelse
                        .map(HenvendelseData::getSendt)
                        .orElse(null)
                )
                .setOpprettetDato(forsteHenvendelse
                        .map(HenvendelseData::getSendt)
                        .orElse(null)
                )
                .setSisteTekst(sisteHenvendelse
                        .map(HenvendelseData::getTekst)
                        .orElse(null))
                ;
    }

    private HenvendelseDTO somHenvendelseDTO(HenvendelseData henvendelseData) {
        return new HenvendelseDTO()
                .setDialogId(Long.toString(henvendelseData.dialogId))
                .setAvsender(henvendelseData.avsenderType == BRUKER ? Avsender.BRUKER : Avsender.VEILEDER)
                .setSendt(henvendelseData.sendt)
                .setLest(henvendelseData.lestAvVeileder)
                .setTekst(henvendelseData.tekst);
    }

    public DialogAktorDTO somDTO(DialogAktor dialogAktor) {
        return new DialogAktorDTO()
                .setAktorId(dialogAktor.aktorId)
                .setTidspunktEldsteUbehandlede(dialogAktor.tidspunktEldsteUbehandlede)
                .setTidspunktEldsteVentende(dialogAktor.tidspunktEldsteVentende)
                .setSisteEndring(dialogAktor.sisteEndring)
                ;
    }
}

