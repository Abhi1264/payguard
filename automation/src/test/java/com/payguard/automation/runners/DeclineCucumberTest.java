package com.payguard.automation.runners;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@Tag("negative")
@Tag("e2e")
@Tag("integration")
@IncludeEngines("cucumber")
@SelectClasspathResource("features/provider_decline.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.payguard.automation.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm,pretty")
public class DeclineCucumberTest {
}
