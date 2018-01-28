package edu.ttn.custom.login.core.services.configurations;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "GoogleProvider Service Configuration", description = "Service Configuration")
public @interface GoogleProviderConfiguration {

    String DEFAULT_PATH = "/home/users/google-oauth";

    @AttributeDefinition(name = "OAuth Provider ID", description = "Google Provider",
            defaultValue = "google") String providerConfigId();

    @AttributeDefinition(name = "User Path", description = "CRX Path for created users", defaultValue = DEFAULT_PATH,
            options = {@Option(label = "google-oauth",
                    value = DEFAULT_PATH)}) String providerConfigUserFolder() default DEFAULT_PATH;

    @AttributeDefinition(name = "Field Mappings", description = "profile-field=provider-field",
            defaultValue = {"givenName=firstName", "familyName=lastName",
                    "jobTitle=headline"}, type = AttributeType.STRING, cardinality = 10) String[] providerConfigFieldMappings();

    @AttributeDefinition(name = "Update User", defaultValue = "true",
            type = AttributeType.BOOLEAN) boolean providerConfigRefreshUserData() default true;
}
