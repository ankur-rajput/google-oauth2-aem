package edu.ttn.custom.login.core.services.impl;

import edu.ttn.custom.login.core.services.Test;
import edu.ttn.custom.login.core.services.configurations.TestConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Test.class, immediate = true)
@Designate(ocd = TestConfig.class)
public class TestImpl implements Test {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestImpl.class);

    @Activate
    public void activate(final TestConfig config) {
        LOGGER.debug("activated:  TestConfigImpl");
    }

    @Deactivate
    public void deactivate(final TestConfig config) {
        LOGGER.debug("deactivated:  TestConfigImpl");
    }


}
