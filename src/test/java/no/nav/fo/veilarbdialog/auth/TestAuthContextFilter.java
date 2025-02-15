package no.nav.fo.veilarbdialog.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.test.auth.AuthTestUtils.TEST_AUDIENCE;

@Service
public class TestAuthContextFilter implements Filter {
    public static final String identHeader = "test_ident";
    public static final String typeHeader = "test_ident_type";
    public static final String clientIdHeader = "test_client_id";

    public static final String AZURE_ISSUER="https://microsoft.com";
    public static final String TOKENDINGS_ISSUER="https://tokendings";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String test_ident = httpRequest.getHeader(identHeader);
        String test_ident_type = httpRequest.getHeader(typeHeader);
        String client_id = httpRequest.getHeader(clientIdHeader);

        AuthContext authContext = new AuthContext(
                UserRole.valueOf(test_ident_type),
                new PlainJWT((test_ident_type.equals("EKSTERN") ? brukerClaims(test_ident, client_id) : veilederClaims(test_ident, client_id)))
        );

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    private JWTClaimsSet veilederClaims(String test_ident, String client_id) {
        return new JWTClaimsSet.Builder()
                .subject(test_ident)
                .audience(TEST_AUDIENCE)
                .issuer(AZURE_ISSUER)
                .claim(AAD_NAV_IDENT_CLAIM, test_ident)
                .claim("azp_name", client_id)
                .build();
    }
    private JWTClaimsSet brukerClaims(String test_ident, String client_id) {
        return new JWTClaimsSet.Builder()
                .subject(test_ident)
                .claim("pid", test_ident)
                .claim("acr", "Level4")
                .claim("client_id", client_id)
                .audience(TEST_AUDIENCE)
                .issuer(TOKENDINGS_ISSUER)
                .build();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
