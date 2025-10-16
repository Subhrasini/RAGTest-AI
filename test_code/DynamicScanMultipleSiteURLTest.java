package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
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

@Owner("sbehera3@opentext.com")
@Slf4j
public class DynamicScanMultipleSiteURLTest extends FodBaseTest {

    public DynamicScanDTO dynamicScanDTO;
    public ApplicationDTO applicationDTO;

    public String fileName = "payloads/fod/2107709.scan";
    public String firstURL = "http://zero.webappsecurity.com";
    public String secondURL = "https://espn.com";


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Start the first Dynamic Web Assessment scan")
    @Test(groups = {"hf", "regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setDynamicSiteUrl(firstURL);
        dynamicScanDTO.setAssessmentType("Dynamic Website Assessment");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(
                applicationDTO.getReleaseName(),
                fileName,
                true,
                true,
                true,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate that dynamic scan url field disabled after scan")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyDisabledSiteUrlField() {
        dynamicScanDTO.setDynamicSiteUrl(secondURL);
        dynamicScanDTO.setAssessmentType("Dynamic Website Assessment");
        var dynamicScanSetupPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openYourReleases().openDetailsForRelease(applicationDTO).openDynamicScanSetup();

        assertThat(dynamicScanSetupPage.getDynamicSiteUrlInput().isEnabled())
                .as("Dynamic site url field should be disabled after scan!")
                .isFalse();
    }


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("552007")
    @Description("Dynamic Scan URL of previous scans are getting updated by the latest in-progress scan")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"verifyDisabledSiteUrlField"},
            enabled = false)
    public void verifyDynamicSiteURL() {

        var dynamicQueuePage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openDynamic();
        dynamicQueuePage.appliedFilters.clearAll();
        dynamicQueuePage.findWithSearchBox(applicationDTO.getApplicationName());
        var listOfURL = dynamicQueuePage.getScanURL();
        assertThat(listOfURL.get(0))
                .as("Validate that scan URL for both scans is different from each other")
                .isNotEqualTo(listOfURL.get(1));
        var dynamicScanOverviewPage = dynamicQueuePage.openDetailsFor(applicationDTO.getApplicationName());

        assertThat(dynamicScanOverviewPage.getScanStatus())
                .as("Validate that scan status is completed")
                .isEqualTo("Completed");

        assertThat(dynamicScanOverviewPage.getDynamicSiteURL())
                .as("Validate that site url of the previous scan has not changed")
                .isEqualTo(firstURL);
    }
}
