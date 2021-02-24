package no.nav.fo.veilarbdialog.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@TestConfiguration
public class ServiceConfig {

    @MockBean
    SystemUserTokenProvider systemUserTokenProvider;

    @MockBean
    AktorregisterClient aktorregisterClient;

    @MockBean
    UnleashClient unleashClient;

    @Bean
    SystemUserTokenProvider systemUserTokenProvider() {
        when(systemUserTokenProvider.getSystemUserToken())
                .thenReturn("test-token");
        return systemUserTokenProvider;
    }

    @Bean
    AktorOppslagClient aktorOppslagClient() {
        when(aktorregisterClient.hentAktorId(any(Fnr.class)))
                .thenReturn(null);
        when(aktorregisterClient.hentFnr(any(AktorId.class)))
                .thenReturn(null);
        return aktorregisterClient;
    }

    @Bean
    UnleashClient unleashClient() {
        when(unleashClient.checkHealth())
                .thenReturn(HealthCheckResult.healthy());
        when(unleashClient.isEnabled(anyString()))
                .thenReturn(false);
        when(unleashClient.isEnabled(anyString(), any()))
                .thenReturn(false);
        return unleashClient;
    }

}
