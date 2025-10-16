package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("msokolovskyi@opentext.com")
@Slf4j
public class SetupScanValidationTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    ReleaseDTO releaseDTO;
    public String dynamicPlusWebsite = "Dynamic+ Website Assessment";
    public String websiteUrl = "www.foo.com";
    public String errorMessage = "Do not include URLs that are under the Dynamic Site URL domain. %s/";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("562001")
    @Description("Validate that user can not run scan with wrong scan setup input")
    @Test(groups = {"regression"})
    public void setupScanValidationTest() {
        tenantDTO = defaultTenantDTO;
        applicationDTO = ApplicationActions.createApplication(tenantDTO, true);
        releaseDTO = ReleaseDTO.createDefaultInstance();
        ReleaseDetailsPage releaseDetailPage = ReleaseActions.createRelease(applicationDTO, releaseDTO);
        var setupPage = releaseDetailPage.pressStartDynamicScan();
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setDynamicSiteUrl("http://zero.webappsecurity.com, http://zero.webappsecurity.com");
        setupPage.setAssessmentType(dynamicScanDTO.getAssessmentType())
                .setEntitlement(dynamicScanDTO.getEntitlement())
                .setDynamicSiteUrl(dynamicScanDTO.getDynamicSiteUrl())
                .setTimeZone(dynamicScanDTO.getTimezone())
                .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing());
        setupPage.pressStartScanBtn();
        String setupStatus = setupPage.getSetupScanStatus();

        assertThat(setupStatus).as("Setup status should be Incomplete")
                .isEqualToIgnoringCase("Setup Status: Incomplete");
        assertThat(setupPage.getValidationMessages()).as("An appropriate error message should be displayed")
                .contains("The Dynamic Site URL is not valid.");
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("814007")
    @Description("Verify that Include URLs field in Dynamic+ website assessment needs to have validation " +
            "developed to prevent it from being the same as the Dynamic Site URL")
    @Test(groups = {"regression"})
    public void dynamicPlusFieldIncludeUrlsTest() {
        LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .findWithSearchBox(defaultTenantDTO.getTenantName())
                .openTenantByName(defaultTenantDTO.getTenantName())
                .openAssessmentTypes()
                .selectAllowSingleScan(dynamicPlusWebsite)
                .selectAllowSubscription(dynamicPlusWebsite);
        BrowserUtil.clearCookiesLogOff();
        var application = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(application, defaultTenantDTO, true);
        var dynamicScanSetupPage = new TenantTopNavbar()
                .openApplications().openYourReleases()
                .findWithSearchBox(application.getReleaseName())
                .openDetailsForRelease(application.getApplicationName(), application.getReleaseName())
                .pressStartDynamicScan();
        dynamicScanSetupPage.setAssessmentType(dynamicPlusWebsite)
                .setDynamicSiteUrl("https://" + websiteUrl)
                .getScopePanel().expand()
                .setIncludeUrls(websiteUrl + "/api", websiteUrl + "/auth/login", "www.github.com")
                .getParentPage()
                .setEnvironmentFacing(FodCustomTypes.EnvironmentFacing.INTERNAL)
                .setTimeZone(FodConfig.SCAN_TIMEZONE);
        dynamicScanSetupPage.pressSaveBtn();
        String setupStatus = dynamicScanSetupPage.getSetupScanStatus();
        assertThat(setupStatus).as("Setup status should be Incomplete")
                .isEqualToIgnoringCase("Setup Status: Incomplete");
        WaitUtil.waitForTrue(() -> !dynamicScanSetupPage.getValidationMessages().isEmpty(),
                Duration.ofSeconds(60), false);
        assertThat(dynamicScanSetupPage.getValidationMessages())
                .as("Error message is generated after save and should be equal to text")
                .contains(String.format(errorMessage + "api", websiteUrl))
                .contains(String.format(errorMessage + "auth/login", websiteUrl));
    }
}