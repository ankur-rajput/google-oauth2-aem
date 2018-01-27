package edu.ttn.custom.login.core.services.handlers;

import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component(name = "My Custom Authentication Handler", immediate = true,
        property = {Constants.SERVICE_DESCRIPTION + "=Authenticate in a different way",
                AuthenticationHandler.PATH_PROPERTY + "=/"})
public class CustomAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private final Logger logger = LoggerFactory.getLogger(CustomAuthenticationHandler.class);

    private static final String REQUEST_METHOD = "POST";
    private static final String USER_NAME = "j_username";
    private static final String PASSWORD = "j_password";

    static final String AUTH_TYPE = "custom.authType";

    static final String REQUEST_URL_SUFFIX = "/j_my_custom_security_check";

    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        if (REQUEST_METHOD.equals(httpServletRequest.getMethod()) && httpServletRequest.getRequestURI()
                .endsWith(REQUEST_URL_SUFFIX) && httpServletRequest.getParameter(USER_NAME) != null) {

            if (!AuthUtil.isValidateRequest(httpServletRequest)) {
                AuthUtil.setLoginResourceAttribute(httpServletRequest, httpServletRequest.getContextPath());
            }

            SimpleCredentials simpleCredentials = new SimpleCredentials(httpServletRequest.getParameter(USER_NAME),
                    httpServletRequest.getParameter(PASSWORD).toCharArray());
            simpleCredentials.setAttribute("j_attr_host_name", httpServletRequest.getServerName());

            return createAuthenticationInfo(simpleCredentials);
        }

        return null;
    }

    private AuthenticationInfo createAuthenticationInfo(Credentials credentials) {
        AuthenticationInfo authenticationInfo = new AuthenticationInfo(AUTH_TYPE);
        authenticationInfo.put("my_credentials", credentials);
        return authenticationInfo;
    }

    @Override
    public boolean requestCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {

    }

    @Override
    public boolean authenticationSucceeded(HttpServletRequest request, HttpServletResponse response,
            AuthenticationInfo authInfo) {
        logger.info("authenticationSucceeded -> Authentication succeeded");
        return super.authenticationSucceeded(request, response, authInfo);
    }

    @Override
    public void authenticationFailed(HttpServletRequest request, HttpServletResponse response,
            AuthenticationInfo authInfo) {
        logger.error("authenticationFailed -> Authentication failed");
        super.authenticationFailed(request, response, authInfo);
    }
}
