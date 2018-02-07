package no.nav.fo.veilarbdialog.feed.avsluttetkvp;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import no.nav.fo.veilarbdialog.db.dao.KvpFeedConsumerDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

@Component
public class KvpFeedConsumer {

    private final AppService appService;

    private final KvpFeedConsumerDAO kvpFeedConsumerDAO;

    @Inject
    public KvpFeedConsumer(AppService appService,
            KvpFeedConsumerDAO feedConsumerDAO) {
        this.appService = appService;
        this.kvpFeedConsumerDAO = feedConsumerDAO;
    }

    String sisteEndring() {
        return String.valueOf(kvpFeedConsumerDAO.hentSisteId());
    }

    void lesKvpFeed(String lastEntryId, List<KvpDTO> elements) {
        long lastSuccessfulId = -1;
        for (KvpDTO element : elements) {
            LoggerFactory.getLogger(KvpFeedConsumer.class).debug("Prosesserer " + element);
//            
//            appService.settDialogerTilHistoriske(element.getAktorId(), element.getAvsluttetDato());
//            lastSuccessfulId = element.getSerial();
        }

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil 
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent
        if(lastSuccessfulId > -1) {
            kvpFeedConsumerDAO.oppdaterSisteFeedId(lastSuccessfulId);
        }
    }
}
