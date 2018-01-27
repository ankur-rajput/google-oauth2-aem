package edu.ttn.custom.login.core.module;

import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.oak.spi.security.authentication.Authentication;
import org.apache.jackrabbit.oak.spi.security.authentication.ImpersonationCredentials;
import org.apache.jackrabbit.oak.spi.security.authentication.PreAuthenticatedLogin;
import org.apache.jackrabbit.oak.spi.security.user.UserAuthenticationFactory;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Set;

public class MyCustomLoginModule extends AbstractLoginModule {

    private final Logger logger = LoggerFactory.getLogger(MyCustomLoginModule.class);
    private Credentials credentials;
    private String userId;

    @Override
    protected Set<Class> getSupportedCredentials() {
        return null;
    }

    @Override
    public boolean login() throws LoginException {
        this.credentials = this.getCredentials();
        PreAuthenticatedLogin preAuthenticatedLogin = this.getSharedPreAuthLogin();
        boolean success;
        Authentication authentication;
        if (preAuthenticatedLogin != null) {
            this.userId = preAuthenticatedLogin.getUserId();
            authentication = this.getUserAuthentication(userId);
            success = authentication != null && authentication.authenticate(PreAuthenticatedLogin.PRE_AUTHENTICATED);
        } else{
            this.userId = this.getUserId();
            authentication = this.getUserAuthentication(userId);
        }
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        return false;
    }

    private Authentication getUserAuthentication(String userId) {
        SecurityProvider securityProvider = this.getSecurityProvider();
        Root root = this.getRoot();
        if (securityProvider != null && root != null) {
            UserConfiguration uc = (UserConfiguration) securityProvider.getConfiguration(UserConfiguration.class);
            UserAuthenticationFactory factory = (UserAuthenticationFactory) uc.getParameters()
                    .getConfigValue("userAuthenticationFactory", null, UserAuthenticationFactory.class);
            if (factory != null) {
                return factory.getAuthentication(uc, root, userId);
            }

            logger.error("No user authentication factory configured in user configuration.");
        }

        return null;
    }

    private String getUserId() {
        String uid = null;
        if (this.credentials != null) {
            if (this.credentials instanceof SimpleCredentials) {
                uid = ((SimpleCredentials)this.credentials).getUserID();
            } else if (this.credentials instanceof GuestCredentials) {
                uid = this.getAnonymousId();
            } else if (this.credentials instanceof ImpersonationCredentials) {
                Credentials bc = ((ImpersonationCredentials)this.credentials).getBaseCredentials();
                if (bc instanceof SimpleCredentials) {
                    uid = ((SimpleCredentials)bc).getUserID();
                }
            } else {
                try {
                    NameCallback callback = new NameCallback("User-ID: ");
                    this.callbackHandler.handle(new Callback[]{callback});
                    uid = callback.getName();
                } catch (UnsupportedCallbackException var3) {
                    logger.warn("Credentials- or NameCallback must be supported");
                } catch (IOException var4) {
                    logger.error("Name-Callback failed: " + var4.getMessage());
                }
            }
        }

        if (uid == null) {
            uid = this.getSharedLoginName();
        }

        return uid;
    }

    private String getAnonymousId() {
        SecurityProvider sp = this.getSecurityProvider();
        if (sp == null) {
            return null;
        } else {
            ConfigurationParameters params = sp.getConfiguration(UserConfiguration.class).getParameters();
            return UserUtil.getAnonymousId(params);
        }
    }
}
