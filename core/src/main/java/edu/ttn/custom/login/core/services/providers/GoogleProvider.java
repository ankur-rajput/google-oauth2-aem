package edu.ttn.custom.login.core.services.providers;

import com.adobe.cq.social.connect.oauth.ProviderUtils;
import com.adobe.granite.auth.oauth.Provider;
import com.adobe.granite.auth.oauth.ProviderType;
import com.adobe.granite.security.user.UserPropertiesService;
import edu.ttn.custom.login.core.services.ServiceUserResourceResolver;
import edu.ttn.custom.login.core.services.configurations.GoogleProviderConfiguration;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.scribe.builder.api.Api;
import edu.ttn.custom.login.core.services.api.GoogleOauth2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Component(service = Provider.class, name = "Google OAuth Provider", immediate = true,
        property = {Constants.SERVICE_DESCRIPTION + "=Provider Implementation for Google"})
@Designate(ocd = GoogleProviderConfiguration.class)
public class GoogleProvider implements Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleProvider.class);

    private GoogleProviderConfiguration config;

    @Reference
    private UserPropertiesService userPropertiesService;

    @Reference
    private ServiceUserResourceResolver serviceUserResourceResolver;

    private static final String USER_ID = "user_id";
    private static final String TOKEN = "token";
    private static final String PROFILE_RESOURCE_TYPE = "cq/security/components/profile";
    private final Api api = new GoogleOauth2Api();
    private Session session;
    private ResourceResolver resourceResolver;

    private Map<String, String> fieldMap;

    public static final String GOOGLE_DETAILS_URL = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json";

    @Activate
    public void activate(GoogleProviderConfiguration config) throws Exception {

        LOGGER.debug("activate : GoogleProvider service");

        this.config = config;

        String[] profileMappings = config.providerConfigFieldMappings();

        LOGGER.debug("profileMappings: " + profileMappings);

        fieldMap = new HashMap<>();
        for (int i = 0; i < profileMappings.length; i++) {
            final String mapping = profileMappings[i];
            final String parts[] = mapping.split("=", 2);
            if (parts.length != 2) {
                LOGGER.warn("Invalid profile mapping \"{}\"", mapping);
            } else {
                // If the source (provider node) has a hierarchy specified, simply store it
                if (parts[1].contains("/")) {
                    fieldMap.put(parts[0], parts[1]);
                }
                // Map the profile attribute to the base path of the profile
                else {
                    fieldMap.put(parts[0], getPropertyPath(parts[1]));
                }
            }
        }

        resourceResolver = serviceUserResourceResolver.getResourceResolver();
        session = resourceResolver.adaptTo(Session.class);
    }

    @Deactivate
    protected void deactivate(final GoogleProviderConfiguration config) throws Exception {
        LOGGER.debug("deactivating provider id {}", config.providerConfigId());
        if (session != null && session.isLive()) {
            try {
                session.logout();
            } catch (final Exception e) {
                // ignore
            }
            session = null;
        }
        if (resourceResolver != null) {
            resourceResolver.close();
        }
    }


    /**
     * Currently only oauth 1a and oauth 2 are supported.
     *
     * @return type
     *
     * @see ProviderType
     */
    @Override
    public ProviderType getType() {
        return ProviderType.OAUTH2;
    }

    /**
     * Specifies an instance of scribe {@link Api} to use for this provider.
     *
     * @return an instance of LinkedInApi
     */
    @Override
    public Api getApi() {
        return api;
    }

    /**
     * OAuth provider's user details URL
     *
     * @return url
     */
    @Override
    public String getDetailsURL() {
        return GOOGLE_DETAILS_URL;
    }

    /**
     * OAuth provider's user extended details URLs, depending on the specific scope
     *
     * @return url
     */
    @Override
    public String[] getExtendedDetailsURLs(String s) {
        return new String[0];
    }

    /**
     * OAuth provider's user extended details URLs, depending on the specific scope and previously fetched data (e.g.
     * {@link #getDetailsURL()}, {@link #getExtendedDetailsURLs(String)}).
     *
     * @param scope  allows to specify a list of property names for each scope
     * @param userId the userId
     * @param props  contains the data previously fetched.
     *
     * @return the list of urls to fetch extended data from.
     */
    @Override
    public String[] getExtendedDetailsURLs(String scope, String userId, Map<String, Object> props) {
        return new String[0];
    }

    /**
     * Unique ID for this provider, used to match a ProviderConfig with this Provider
     *
     * @return ID of this provider
     */
    @Override
    public String getId() {
        return config.providerConfigId();
    }

    /**
     * Readable name for this Provider
     *
     * @return name of this Provider
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Map the provider's userid to CRX user id; Note that usernames must be unique so the returned username should
     * always include some prefix specific to this provider (e.g. in case facebook, twitter, etc have a user with the
     * same username)
     *
     * @param userId provider's userId
     * @param props  map of all provider's properties for this userId
     *
     * @return CQ user id
     */
    @Override
    public String mapUserId(String userId, Map<String, Object> props) {
        final String userName = (String) props.get(getPropertyPath("id"));
        if (userName != null && userName.length() > 0) {
            return "google-" + userName;
        } else {
            return "google-" + userId;
        }
    }

    /**
     * Return the node path where the user should be created
     *
     * @param userId
     * @param clientId in use when creating this user
     * @param props    map of all provider's properties for this user
     *
     * @return relative path to store this user within /home/users (e.g. "communities/1234" might be appropriate for a
     * user with id=12345678)
     */
    @Override
    public String getUserFolderPath(String userId, String clientId, Map<String, Object> props) {
        return config.providerConfigUserFolder() + "/" + userId.substring(0, 4);
    }

    /**
     * Map the provider's user properties name to CQ user properties. This method will at least be called to map
     * properties fetched from {@link #getDetailsURL()}. If {@link #getExtendedDetailsURLs(String)} is not null, this
     * method will be called for the map of properties fetched from each url.
     *
     * @param srcUrl
     * @param clientId      in use to retrieve this set of properties
     * @param existing      CQ properties that have already been mapped
     * @param newProperties addition provider properties that need to be mapped
     *
     * @return the result of mapping the new properties, and combining with the existing
     */
    @Override
    public Map<String, Object> mapProperties(String srcUrl, String clientId, Map<String, Object> existing,
            Map<String, String> newProperties) {

        if (srcUrl.equals(getDetailsURL())) {
            final Map<String, Object> mapped = new HashMap<>();
            mapped.putAll(existing);

            for (final Map.Entry<String, String> prop : newProperties.entrySet()) {
                final String key = prop.getKey();

                final String mappedKey = getPropertyPath(key);
                final Object mappedValue = prop.getValue();
                if (mappedValue != null) {
                    mapped.put(mappedKey, mappedValue);
                }
            }
            return mapped;
        }

        return existing;
    }

    /**
     * Return the property path where the access token will be stored (if ProviderConfig     has access token storage
     * enabled)
     *
     * @param clientId
     *
     * @return the property path where access token may be stored for a user e.g. profile/someapp-clientid/accesstoken
     */
    @Override
    public String getAccessTokenPropertyPath(String clientId) {
        return "oauth/token-" + clientId;
    }

    /**
     * Return the property path where the oauth user id will be stored
     *
     * @param clientId
     *
     * @return
     */
    @Override
    public String getOAuthIdPropertyPath(String clientId) {
        return "oauth/oauthid-" + clientId;
    }

    /**
     * Use the request to get the User who has (or will have) oauth profile data attached
     *
     * @param request
     *
     * @return the User or null, if no User is associated with the request
     */
    @Override
    public User getCurrentUser(SlingHttpServletRequest request) {
        try {
            final Authorizable authorizable = request.getResourceResolver().adaptTo(Authorizable.class);
            if (authorizable != null && !authorizable.isGroup() && !authorizable.getID().equals("anonymous")) {
                return (User) authorizable;
            }
        } catch (final RepositoryException e) {
            LOGGER.error("provider: disabled; failed identify user", e);
        }
        return null;
    }

    /**
     * Called after a user is created by Granite
     *
     * @param user
     */
    @Override
    public void onUserCreate(User user) {
        LOGGER.debug("onUserCreate:" + user);
        try {
            session.refresh(true);
            final Node userNode = session.getNode(userPropertiesService.getAuthorizablePath(user.getID()));
            final Node profNode = userNode.getNode("profile");

            // Set the profile resource type to be a CQ profile
            profNode.setProperty(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    PROFILE_RESOURCE_TYPE);

            processProfileMappings(userNode, profNode);

            session.save();
        } catch (final RepositoryException e) {
            LOGGER.error("onUserCreate: failed to copy profile properties to cq profile", e);
        }
    }

    /**
     * Called after a user is updated (i.e. profile data is mapped and applied to user that already exists);
     *
     * @param user
     */
    @Override
    public void onUserUpdate(User user) {
        if (config.providerConfigRefreshUserData()) {
            try {
                session.refresh(true);
                final Node userNode = session.getNode(userPropertiesService.getAuthorizablePath(user.getID()));
                final Node profNode = userNode.getNode("profile");

                processProfileMappings(userNode, profNode);

                session.save();
            } catch (final RepositoryException e) {
                LOGGER.error("onUserUpdate: failed to update profile properties in cq profile", e);
            }
        }

    }

    /**
     * Create an OAuthRequest to request protected data from the OAuth provider system.
     *
     * @param url
     *
     * @return the OAuthRequest
     */
    @Override
    public OAuthRequest getProtectedDataRequest(String url) {
        return new OAuthRequest(Verb.GET, url);
    }

    /**
     * Parse the OAuth Response for protected profile data during profile import
     *
     * @param response
     *
     * @return Map of profile properties
     */
    @Override
    public Map<String, String> parseProfileDataResponse(Response response) throws IOException {
        return ProviderUtils.parseProfileDataResponse(response);
    }

    /**
     * What is the user data property that contains this OAuth provider's user id? (e.g. "id")
     *
     * @return
     */
    @Override
    public String getUserIdProperty() {
        return "id";
    }

    /**
     * OAuth provider validate token URL
     *
     * @param clientId
     * @param token
     *
     * @return url or null if validate token is not supported
     */
    @Override
    public String getValidateTokenUrl(String clientId, String token) {
        return null;
    }

    /**
     * Check the validity of a token
     *
     * @param responseBody
     * @param clientId
     *
     * @return true if the response body contains the validity of the token, the token has been issued for the
     * provided clientId and the token type matches with the one provided
     */
    @Override
    public boolean isValidToken(String responseBody, String clientId, String tokenType) {
        return false;
    }

    /**
     * Parse the response body and return the userId contained in the response
     *
     * @param responseBody
     *
     * @return the userId contained in the response or null if is not contained
     */
    @Override
    public String getUserIdFromValidateTokenResponseBody(String responseBody) {
        String userId = null;
        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject(responseBody);
        } catch (final JSONException e) {
            LOGGER.error("getUserIdFromValidateTokenResponseBody: error while parsing response body", e);
            return userId;
        }
        final JSONObject token = jsonBody.optJSONObject(TOKEN);
        if (token != null) {
            userId = token.optString(USER_ID);
            if ("".equals(userId)) {
                userId = null;
            }
        }
        return userId;
    }

    /**
     * Parse the response body and return the error description contained in the response
     *
     * @param responseBody
     *
     * @return the error description contained in the response or null if is not contained
     */
    @Override
    public String getErrorDescriptionFromValidateTokenResponseBody(String responseBody) {
        return null;
    }

    protected String getPropertyPath(final String property) {
        return "profile/google/" + property;
    }

    private String getUserNodeAttributeString(final Node userNode, final String attributeName,
            final String defaultVal) {
        String retVal = defaultVal;
        try {
            final javax.jcr.Property prop = userNode.getProperty(attributeName);
            if (prop != null) {
                retVal = prop.getString() == null ? defaultVal : prop.getString();
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Couldn't get {} attribute value from user profile.", attributeName);
        }

        return retVal;
    }

    /**
     * Copy configured values from user's node (updated by Granite) to the profile node
     *
     * @param userNode
     * @param profNode
     *
     * @throws RepositoryException
     */
    protected void processProfileMappings(final Node userNode, final Node profNode) throws RepositoryException {
        for (final Entry<String, String> entry : fieldMap.entrySet()) {
            final String val = getUserNodeAttributeString(userNode, entry.getValue(), null);
            if (val != null) {
                profNode.setProperty(entry.getKey(), val);
            }
        }
    }
}
