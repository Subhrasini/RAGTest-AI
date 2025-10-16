package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.dynamic_scan_setup.DynamicScanSetupPage;
import com.fortify.fod.ui.pages.tenant.applications.release.popups.StartDynamicScanPopup;
import com.fortify.fod.ui.pages.tenant.applications.your_releases.YourReleasesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class SiteAccessibilityTest extends FodBaseTest {

    public ApplicationDTO applicationDTO, appDto;
    public String siteAccessibilityStep = "Site Accessibility";
    public String validUrl = "https://github.com";
    public String invalidUrl = "htttp://zero.webappsecurity.com";
    public String notResolvedHostUrl = "http://zero.thisisaninvalidhost.com/";
    public String page404 = "https://zero.webappsecurity.com/random";
    public String assessmentType = "AUTO-DYNAMIC";
    public String entitlements = "Single Scan";
    public String timeZone = "(UTC) Coordinated Universal Time";
    public String validationMessage = "The Dynamic Site URL is not valid.";

    String dastHostErrorUrl = "http://zero.webappsecurity";
    String dastInvalidHttpResponseUrl = "http://172.16.10.234:8080/WebGoat/login.mvc";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to view site accessibility section")
    @Test(groups = {"regression", "smoke"})
    public void siteAccessibilityTest() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        BrowserUtil.clearCookiesLogOff();
        SoftAssertions softAssertions = new SoftAssertions();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases();
        page(YourReleasesPage.class).openDetailsForRelease(applicationDTO)
                .openDynamicScanSetup();

        var dynamicPage = page(DynamicScanSetupPage.class);
        var createScanPopup = page(StartDynamicScanPopup.class);

        AllureReportUtil.info("Site Accessibility :All Pass");
        dynamicPage
                .setDynamicSiteUrl(validUrl)
                .setAssessmentType(assessmentType)
                .setEntitlement(entitlements)
                .setTimeZone(timeZone)
                .setEnvironmentFacing(FodCustomTypes.EnvironmentFacing.INTERNAL)
                .pressStartScanBtn()
                .pressNextButton();

        assertThat(createScanPopup.getWizardSteps().get(1))
                .as("Validate that Site Accessibility step available")
                .isEqualTo(siteAccessibilityStep);
        assertThat(createScanPopup.getCurrentWizardStep())
                .as("Validate that current step is: " + siteAccessibilityStep)
                .isEqualTo(siteAccessibilityStep);

        softAssertions.assertThat(createScanPopup.isUrlFormatValid()).as("Url format valid").isTrue();
        softAssertions.assertThat(createScanPopup.isHostValid()).as("Host valid").isTrue();
        softAssertions.assertThat(createScanPopup.isHttpResponseValid()).as("Http response valid").isTrue();
        softAssertions.assertThat(createScanPopup.isCodeResponseValid()).as("Status code valid").isTrue();
        softAssertions.assertThat(createScanPopup.isDomainValid()).as("Domain valid").isTrue();
        softAssertions.assertThat(createScanPopup.isApplicationFunctionalityValid())
                .as("App functionality valid").isTrue();

        createScanPopup.pressClose();

        AllureReportUtil.info("Site Accessibility :invalid url format");
        dynamicPage
                .setDynamicSiteUrl(invalidUrl)
                .pressStartScanBtn();

        softAssertions.assertThat(dynamicPage.getValidationMessages().get(0))
                .as("Validation message should be equal:  " + validationMessage)
                .isEqualTo(validationMessage);

        AllureReportUtil.info("Site Accessibility:Host does not resolve");
        dynamicPage
                .setDynamicSiteUrl(notResolvedHostUrl)
                .pressStartScanBtn()
                .pressNextButton();

        softAssertions.assertThat(createScanPopup.isUrlFormatValid()).as("Url format valid").isTrue();
        softAssertions.assertThat(createScanPopup.isHostValid()).as("Host not valid").isFalse();

        AllureReportUtil.info("Site Accessibility: Invalid status code");
        createScanPopup.pressClose()
                .setDynamicSiteUrl(page404)
                .pressStartScanBtn()
                .pressNextButton();

        softAssertions.assertThat(createScanPopup.isUrlFormatValid()).as("Url format valid").isTrue();
        softAssertions.assertThat(createScanPopup.isHostValid()).as("Host valid").isTrue();
        softAssertions.assertThat(createScanPopup.isHttpResponseValid()).as("Http response valid").isTrue();
        softAssertions.assertThat(createScanPopup.isCodeResponseValid())
                .as("Status code not valid").isFalse();

        softAssertions.assertAll();
    }

    @MaxRetryCount(2)
    @Owner("svpillai@opentext.com")
    @FodBacklogItem("1395004")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify site accessibility check on starting a dast automated scan")
    @Test(groups = {"regression"})
    public void dastSiteAccessibilityTest() {
        appDto = ApplicationDTO.createDefaultInstance();
        var dynamicScanDto= DynamicScanDTO.createDefaultDastAutomatedInstance();
        var dynamicScanSetupPage = ApplicationActions.createApplication(appDto, defaultTenantDTO,
                true).openDynamicScanSetup();
        var startDynamicScanPopup = page(StartDynamicScanPopup.class);
        dynamicScanSetupPage
                .setAssessmentType(dynamicScanDto.getAssessmentType())
                .setDynamicSiteUrl(dastHostErrorUrl)
                .setEntitlement(dynamicScanDto.getEntitlement())
                .setTimeZone(dynamicScanDto.getTimezone())
                .setEnvironmentFacing(dynamicScanDto.getEnvironmentalFacing())
                .pressStartScanBtn()
                .pressNextButton();

        assertThat(startDynamicScanPopup.getWizardSteps().get(1))
                .as("Site Accessibility section should be available in Start Dynamic Scan wizard")
                .isEqualTo(siteAccessibilityStep);
        assertThat(startDynamicScanPopup.getHostText())
                .as("Valid host error should be shown")
                .contains("Host does not resolve");
        startDynamicScanPopup.pressClose();

        dynamicScanSetupPage.setDynamicSiteUrl(page404).pressStartScanBtn().pressNextButton();
        assertThat(startDynamicScanPopup.getCodeResponse())
                .as("200 Ok code should not be returned")
                .contains("Does not return 200 OK response");
        startDynamicScanPopup.pressClose();

        dynamicScanSetupPage.setDynamicSiteUrl(dastInvalidHttpResponseUrl).pressStartScanBtn().pressNextButton();
        assertThat(startDynamicScanPopup.getHttpResponse())
                .as("Invalid http response should be returned")
                .contains("Does not return valid HTTP response");
        startDynamicScanPopup.pressClose();
    }
}
