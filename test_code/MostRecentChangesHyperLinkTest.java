package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationCell;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

@Owner("dgochev@opentext.com")
@Slf4j
public class MostRecentChangesHyperLinkTest extends FodBaseTest {
    public String dynamicFileName = "payloads/fod/dynamic.zero.fpr";

    @FodBacklogItem("1465002")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after updating existing release SDLC to production via edit attribute button")
    @Test(groups = {"regression"})
    void updateReleaseSdlcToProductionViaEditAttributesTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var app = ApplicationActions.createApplication(appDto, defaultTenantDTO, true)
                .tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());

        var appDetailsPage = app.openDetails();
        appDetailsPage.getReleaseByName(appDto.getReleaseName()).select();

        appDetailsPage.clickEditAttributes().setSdlc(FodCustomTypes.Sdlc.Production).pressSave();
        new ModalDialog("Alert").pressClose();

        Supplier<Boolean> sup = () -> appDetailsPage.getReleaseByName(appDto.getReleaseName())
                .getSdlcStatus().equalsIgnoreCase("Production");
        WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);

        var sdlc = appDetailsPage.getReleaseByName(appDto.getReleaseName()).getSdlcStatus();
        var soft = new SoftAssertions();

        soft.assertThat(sdlc)
                .as("Release SDLC status is not changed to production")
                .isEqualTo("Production");

        app = appDetailsPage.tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());

        soft.assertThat(getLinkTextWithAwait(app, "Release Deployed to Production"))
                .as("Wrong hyper link text")
                .isEqualTo("Release Deployed to Production");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after creating new release")
    @Test(groups = {"regression"})
    void createReleaseTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(appDto, defaultTenantDTO, true);

        var releaseDto = ReleaseDTO.createDefaultInstance();
        releaseDto.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        var app = ReleaseActions.createReleases(appDto, releaseDto)
                .tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());

        var soft = new SoftAssertions();

        soft.assertThat(getLinkTextWithAwait(app, "Release Created"))
                .as("Wrong hyper link text")
                .isEqualTo("Release Created");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Release details opens in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Release Details page")
                .isEqualTo("Release Details");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after updating existing release SDLC to production via release settings page")
    @Test(groups = {"regression"})
    void updateReleaseSdlcToProductionViaSettingsTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var app = ApplicationActions.createApplication(appDto, defaultTenantDTO, true)
                .tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());
        var appDetailsPage =
                app.openDetails()
                        .getReleaseByName(appDto.getReleaseName())
                        .openReleaseDetails()
                        .openReleaseSettings()
                        .setSDLCStatus(FodCustomTypes.Sdlc.Production)
                        .pressSave()
                        .openAppDetailsPage();

        Supplier<Boolean> sup = () -> appDetailsPage.getReleaseByName(appDto.getReleaseName())
                .getSdlcStatus().equalsIgnoreCase("Production");
        WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);

        var sdlc = appDetailsPage.getReleaseByName(appDto.getReleaseName()).getSdlcStatus();
        var soft = new SoftAssertions();

        soft.assertThat(sdlc)
                .as("Release SDLC status is not changed to production")
                .isEqualTo("Production");

        app = appDetailsPage.tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());
        soft.assertThat(getLinkTextWithAwait(app, "Release Deployed to Production"))
                .as("Wrong hyper link text")
                .isEqualTo("Release Deployed to Production");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after passing release security policy")
    @Test(groups = {"regression"})
    void releasePassingSecurityPolicyTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var dynamicScanDto = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(appDto, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDto, appDto, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(appDto, true);
        BrowserUtil.clearCookiesLogOff();

        var app = LogInActions.tamUserLogin(defaultTenantDTO).getAppByName(appDto.getApplicationName());
        var soft = new SoftAssertions();

        soft.assertThat(getLinkTextWithAwait(app, "Release Passing Security Policy"))
                .as("Wrong hyper link text")
                .isEqualTo("Release Passing Security Policy");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application Overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @FodBacklogItem("1465002")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after updating application business criticality via edit attribute button")
    @Test(groups = {"regression"})
    void updateAppBusinessCriticalityViaEditAttributeTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var appPage = ApplicationActions.createApplication(appDto, defaultTenantDTO, true)
                .tenantTopNavbar.openApplications();
        var app = appPage.getAppByName(appDto.getApplicationName());
        var actualCriticality = app.getBusinessCriticality();
        var criticality = getRandomCriticality(actualCriticality);
        appPage.getAppByName(appDto.getApplicationName()).select();
        appPage.pressEditAttributes().setBusinessCriticality(criticality).pressSave();

        Supplier<Boolean> sup = () -> app.getBusinessCriticality().equalsIgnoreCase(criticality.getTypeValue());
        WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);
        actualCriticality = app.getBusinessCriticality();
        var soft = new SoftAssertions();

        soft.assertThat(actualCriticality)
                .as("Application business criticality is not changed to '%s'".formatted(criticality))
                .isEqualTo(criticality.getTypeValue().toUpperCase());

        soft.assertThat(getLinkTextWithAwait(app, "Business Criticality Updated"))
                .as("Wrong hyper link text")
                .isEqualTo("Business Criticality Updated");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after failing release security policy")
    @Test(groups = {"regression"})
    void releaseFailingSecurityPolicyTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        appDto.setApplicationName("AutoMobileApp");
        var app = LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME,
                        FodConfig.TAM_PASSWORD,FodConfig.OVERRIDE_DEFAULT_TENANT_NAME)
                .tenantTopNavbar.openApplications()
                .getAppByName(appDto.getApplicationName());

        var soft = new SoftAssertions();

        soft.assertThat(getLinkTextWithAwait(app, "Release Failing Security Policy"))
                .as("Wrong hyper link text")
                .isEqualTo("Release Failing Security Policy");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application Overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after updating application business criticality via settings page")
    @Test(groups = {"regression"})
    void updateAppBusinessCriticalityViaSettingsTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var appPage = ApplicationActions.createApplication(appDto, defaultTenantDTO, true)
                .tenantTopNavbar.openApplications();
        var actualCriticality = appPage.getAppByName(appDto.getApplicationName()).getBusinessCriticality();
        var criticality = getRandomCriticality(actualCriticality);
        appPage.getAppByName(appDto.getApplicationName()).openDetails().openSettings().openAppSummaryTab()
                .setBusinessCriticality(criticality).pressSave();
        var app = appPage.tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());

        Supplier<Boolean> sup = () -> app.getBusinessCriticality()
                .equalsIgnoreCase(criticality.getTypeValue());
        WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);
        actualCriticality = app.getBusinessCriticality();
        var soft = new SoftAssertions();

        soft.assertThat(actualCriticality)
                .as("Application business criticality is not changed to '%s'".formatted(criticality))
                .isEqualTo(criticality.getTypeValue().toUpperCase());

        soft.assertThat(getLinkTextWithAwait(app, "Business Criticality Updated"))
                .as("Wrong hyper link text")
                .isEqualTo("Business Criticality Updated");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application overview page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page")
                .isEqualTo("Releases");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after running static scan")
    @Test(groups = {"regression"})
    void staticScanVulnerabilitiesTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var staticScanDto = StaticScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(appDto, defaultTenantDTO, true);
        staticScanDto.setFileToUpload("payloads/fod/WebGoat5.0.zip");
        var app = StaticScanActions.createStaticScan(staticScanDto, appDto,
                        FodCustomTypes.SetupScanPageStatus.Completed)
                .tenantTopNavbar.openApplications().getAppByName(appDto.getApplicationName());

        var soft = new SoftAssertions();

        soft.assertThat(getLinkTextWithAwait(app, "New Static Vulnerabilities Detected"))
                .as("Wrong hyper link text")
                .isEqualTo("New Static Vulnerabilities Detected");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application Issues page is opened in the same tab")
                .isEqualTo(2);

        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page")
                .isEqualTo("Application Issues");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after running dynamic scan")
    @Test(groups = {"regression"})
    void dynamicScanVulnerabilitiesTest() {
        var appDto = ApplicationDTO.createDefaultInstance();
        var dynamicScanDto = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(appDto, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDto, appDto, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(appDto.getReleaseName(), dynamicFileName, true,
                true, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(appDto, false);
        BrowserUtil.clearCookiesLogOff();

        var app = LogInActions.tamUserLogin(defaultTenantDTO).getAppByName(appDto.getApplicationName());
        var soft = new SoftAssertions();

        soft.assertThat(getLinkTextWithAwait(app, "New Dynamic Vulnerabilities Detected"))
                .as("New Dynamic Vulnerabilities Detected hyper link appears on Your Application page")
                .isEqualTo("New Dynamic Vulnerabilities Detected");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application Issues page is opened in the new tab")
                .isEqualTo(2);
        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page and it should be issues page")
                .isEqualTo("Application Issues");

        soft.assertAll();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hyper link text after running mobile scan")
    @Test(groups = {"regression"})
    void mobileScanVulnerabilitiesTest() {
        var appDto = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanDto = MobileScanDTO.createDefaultScanInstance();
        mobileScanDto.setFrameworkType("iOS");
        mobileScanDto.setFileToUpload("payloads/fod/Mobile_critical_vul.ipa");
        mobileScanDto.setAuditPreference(FodCustomTypes.AuditPreference.MobileAutomated);
        ApplicationActions.createApplication(appDto, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDto, appDto, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        var app = LogInActions.tamUserLogin(defaultTenantDTO).getAppByName(appDto.getApplicationName());
        var soft = new SoftAssertions();
        soft.assertThat(getLinkTextWithAwait(app, "New Mobile Vulnerabilities Detected"))
                .as("New Mobile Vulnerabilities Detected appears on Your Application page")
                .isEqualTo("New Mobile Vulnerabilities Detected");

        app.clickMostRecentChangeHyperLink();
        soft.assertThat(getBrowserTabCount())
                .as("Application Issues page is opened in the new tab")
                .isEqualTo(2);
        soft.assertThat(getHeaderTextAndCloseTab())
                .as("New tab is not a Application overview page and it should be issues page")
                .isEqualTo("Application Issues");

        soft.assertAll();
    }

    private int getBrowserTabCount() {
        return Selenide.webdriver().driver().getWebDriver().getWindowHandles().size();
    }

    private WebDriver switchToTab(int tabIndex) {
        return Selenide.switchTo().window(tabIndex);
    }

    private String getHeaderTextAndCloseTab() {
        var tab = switchToTab(1);
        String header = tab.findElement(By.cssSelector(".contextbar.lower .section")).getText().trim();
        if (getBrowserTabCount() > 1)
            tab.close();
        switchToTab(0);
        return header;
    }

    private FodCustomTypes.BusinessCriticality getRandomCriticality(String actual) {
        return Arrays.stream(FodCustomTypes.BusinessCriticality.values())
                .filter(e -> !e.getTypeValue().equals("None"))
                .filter(e -> !e.getTypeValue().equalsIgnoreCase(actual))
                .findFirst().get();
    }

    private String getLinkTextWithAwait(YourApplicationCell app, String expectedText) {
        Supplier<Boolean> sup = () -> app.getMostRecentChangeText().equals(expectedText);
        WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);
        return app.getMostRecentChangeText();
    }
}