package edu.ttn.custom.login.core.services.api;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;
import org.scribe.utils.OAuthEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleOauth2Api extends DefaultApi20 {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOauth2Api.class);

    private static final String AUTHORIZE_URL =
            "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s";
    private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://accounts.google.com/o/oauth2/token";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig oAuthConfig) {

        LOGGER.debug("inside getAuthorizationUrl");

        String url_returned;

        // Append scope if present
        if (oAuthConfig.hasScope()) {
            url_returned = String.format(SCOPED_AUTHORIZE_URL, oAuthConfig.getApiKey(),
                    OAuthEncoder.encode(oAuthConfig.getCallback()), OAuthEncoder.encode(oAuthConfig.getScope()));
        } else {
            url_returned = String.format(AUTHORIZE_URL, oAuthConfig.getApiKey(),
                    OAuthEncoder.encode(oAuthConfig.getCallback()));
        }

        LOGGER.debug("url_returned: ", url_returned);

        return url_returned;
    }

}
