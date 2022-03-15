package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface VeilarboppfolgingClient {

    Optional<ManuellStatusV2DTO> hentManuellStatus(Fnr fnr);

    boolean erUnderOppfolging(Fnr fnr);

}
