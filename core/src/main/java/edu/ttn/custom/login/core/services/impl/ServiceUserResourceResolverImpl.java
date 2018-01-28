package edu.ttn.custom.login.core.services.impl;

import edu.ttn.custom.login.core.services.ServiceUserResourceResolver;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = ServiceUserResourceResolver.class)
public class ServiceUserResourceResolverImpl implements ServiceUserResourceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUserResourceResolverImpl.class);

    private static final String USER_ADMIN = "custom-login-module-user";

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public ResourceResolver getResourceResolver() {
        Map<String, Object> param = new HashMap();
        param.put(ResourceResolverFactory.SUBSERVICE, USER_ADMIN);
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
        } catch (LoginException le) {
            LOGGER.error("LoginException occurred : " + le);
        }
        LOGGER.trace("Returning Resource Resolver : {}", resourceResolver);
        return (resourceResolver);
    }
}
