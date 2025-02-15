package no.nav.fo.veilarbdialog.config;

import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EnhanceSecureLogsFilter implements Filter {

    private final IAuthService authService;

    public static final String SECURELOGS_ER_INTERN_BRUKER = "SecureLogsFilter.erInternBruker";
    public static final String SECURELOGS_INNLOGGET_BRUKER_IDENT = "SecureLogsFilter.innloggetBrukerIdent";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();

        MDC.put(SECURELOGS_ER_INTERN_BRUKER, erInternBruker);
        MDC.put(SECURELOGS_INNLOGGET_BRUKER_IDENT, innloggetBrukerIdent);

        filterChain.doFilter(servletRequest, servletResponse);
    }

}
