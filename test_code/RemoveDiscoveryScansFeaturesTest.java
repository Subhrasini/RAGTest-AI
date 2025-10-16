package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("482004")
@Slf4j
public class RemoveDiscoveryScansFeaturesTest extends FodBaseTest {
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Disable use of digital discovery findings option is removed from tenant settings")
    @Test(groups = {"regression"})
    public void verifyDiscoveryScanOptionDeletedForTenant() {
        var tenantPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName());
        var labelNames = tenantPage.openTabOptions().getLabelNamesFromOptionsTab();
        assertThat(labelNames.contains("Disable use of digital discovery findings"))
                .as("'Disable use of digital discovery findings' option should be removed")
                .isFalse();

        AllureReportUtil.info("Digital Discovery Tab is removed from Additional service page");
        var tabExist = tenantPage.openAdditionalServices().getTabByName("Digital Discovery").exists();
        assertThat(tabExist)
                .as("Digital Discovery tab should be removed")
                .isFalse();
        var addConfigurationPopup = new AdminTopNavbar().openDynamic().openScanConfiguration().addNewConfiguration();
        var scanTypeList = addConfigurationPopup.scanTypeDropDown.getOptionsTextValues();
        assertThat(scanTypeList)
                .as("Scan Types are removed for Digital Discovery and Risk Profile Discovery")
                .containsExactlyInAnyOrder("Application Monitoring", "Web (Detailed)")
                .doesNotContain("Digital Discovery", "Risk Profile (Discovery)");
        addConfigurationPopup.pressCancelButton();
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that all Site Settings for Digital Discovery are removed")
    @Test(groups = {"regression"})
    public void verifyDiscoveryScanSettingsDeleted() {
        var settingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        assertThat(settingsPage.isSettingExists("AllowDigitalDiscovery"))
                .as("Setting 'AllowDigitalDiscovery' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryConfidenceMap"))
                .as("Setting 'DigitalDiscoveryConfidenceMap' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryConfigurationMaxDomains"))
                .as("Setting 'DigitalDiscoveryConfigurationMaxDomains' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryMaxConcurrentScans"))
                .as("Setting 'DigitalDiscoveryMaxConcurrentScans' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryRiskProfileMinimumConfidence"))
                .as("Setting 'DigitalDiscoveryRiskProfileMinimumConfidence' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryRiskProfileScanMaxDurationMinutes"))
                .as("Setting 'DigitalDiscoveryRiskProfileScanMaxDurationMinutes' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryScanFrequencyDays"))
                .as("Setting 'DigitalDiscoveryScanFrequencyDays' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryScanMaxDurationMinutes"))
                .as("Setting 'DigitalDiscoveryScanMaxDurationMinutes' should be removed")
                .isFalse();
        assertThat(settingsPage.isSettingExists("DigitalDiscoveryScanStartDelayMinutes"))
                .as("Setting 'DigitalDiscoveryScanStartDelayMinutes' should be removed")
                .isFalse();
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that the Discovered and Ignored tabs are removed from Application")
    @Test(groups = {"regression"})
    public void verifyRemovalOfDiscoveryTabsInTenant() {
        AllureReportUtil.info("Discovered and Ignored tabs are removed from Application");
        var tenantLogin = LogInActions.tamUserLogin(defaultTenantDTO);
        assertThat(tenantLogin.getTabByName("Discovered").exists())
                .as("Discovered Tab should not present")
                .isFalse();
        assertThat(tenantLogin.getTabByName("Ignored").exists())
                .as("Discovered Tab should not present")
                .isFalse();

        AllureReportUtil.info("API explorer Discovery scan endpoints should be removed");
        var swaggerPage = tenantLogin.tenantTopNavbar.openApiSwagger();
        assertThat(swaggerPage.resourceExist("Discovery Scans"))
                .as("Api endpoints related to Discovery Scans are removed")
                .isFalse();
        BrowserUtil.clearCookiesLogOff();
    }
}
