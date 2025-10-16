package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;

import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("766012")
@Slf4j
public class ScansRedirectToReleasePageTest extends FodBaseTest {

    static ApplicationDTO applicationDTO, mobileApplicationDTO, dynamicApplicationDTO;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Description("Creating tenant and applications to execute tests")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        applicationDTO = ApplicationDTO.createDefaultInstance();
        mobileApplicationDTO = ApplicationDTO.createDefaultMobileInstance();
        dynamicApplicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        ApplicationActions.createApplication(mobileApplicationDTO, tenantDTO, false);
        ApplicationActions.createApplication(dynamicApplicationDTO, tenantDTO, false);
    }

    @MaxRetryCount(1)
    @Description("Checks that for Open Source, Static scan and Dynamic scan imports, resultant page is Release Scans")
    @Test(dependsOnMethods = {"prepareTestData"}, dataProvider = "importScanType")
    public void webAppImportCheckReleasePageTest(FodCustomTypes.ScanType scanType, String payload) {
        var releaseDetailsScansPage = getReleaseDetailsPage(applicationDTO.getApplicationName(),
                applicationDTO.getReleaseName())
                .openScans();
        if (scanType == FodCustomTypes.ScanType.OpenSource) {
            releaseDetailsScansPage.pressImportOpenSourceScan()
                    .uploadFile(payload)
                    .pressImportButton();
        }
        if (scanType == FodCustomTypes.ScanType.Dynamic) {
            importScans(releaseDetailsScansPage, scanType, payload);
        }
        if (scanType == FodCustomTypes.ScanType.Static) {
            importScans(releaseDetailsScansPage, scanType, payload);
        }
        assertThat(url())
                .as("The " + url() + " should contain 'Release/{dd}/Scans")
                .containsPattern("^.*Releases/\\d{1,5}/Scans");
    }

    @MaxRetryCount(1)
    @Description("Checks that for Web App and Mobile App static scan, the resultant page is Release Scans page")
    @Test(dependsOnMethods = {"webAppImportCheckReleasePageTest"}, dataProvider = "staticApplicationType")
    public void multiAppStaticCheckReleasePageTest(FodCustomTypes.ApplicationType applicationType) {
        var application = applicationType == FodCustomTypes.ApplicationType.Mobile
                ? mobileApplicationDTO
                : applicationDTO;
        var releaseDetailsPage = getReleaseDetailsPage(application.getApplicationName(),
                application.getReleaseName());
        executeStaticScan(releaseDetailsPage);
        waitAndValidateUrl("StaticScanSetup");
    }

    @MaxRetryCount(1)
    @Description("Checks that for dynamic scan, the resultant page is Release Scans page")
    @Test(dependsOnMethods = {"prepareTestData"})
    public void webAppDynamicCheckReleasePageTest() {
        var releaseDetailsPage = getReleaseDetailsPage(dynamicApplicationDTO.getApplicationName(),
                dynamicApplicationDTO.getReleaseName());
        releaseDetailsPage.pressStartDynamicScan()
                .setAssessmentType("AUTO-DYNAMIC")
                .setDynamicSiteUrl("http://zero.webappsecurity.com")
                .setEntitlement("Subscription")
                .setTimeZone(FodConfig.SCAN_TIMEZONE)
                .setEnvironmentFacing(FodCustomTypes.EnvironmentFacing.INTERNAL)
                .pressStartScanBtn()
                .pressNextButtonUntilStartAvailable()
                .startScanButton.click();
        waitAndValidateUrl("DynamicScanSetup");
        DynamicScanActions.completeDynamicScanAdmin(dynamicApplicationDTO, true);
    }

    @MaxRetryCount(1)
    @Description("Checks for that for Mobile Scan, resultant page is Release Scans")
    @Test(dependsOnMethods = {"prepareTestData"})
    public void mobileAppScanMobileCheckReleasePageTest() {
        var releaseDetailsPage = getReleaseDetailsPage(mobileApplicationDTO.getApplicationName(),
                mobileApplicationDTO.getReleaseName());
        releaseDetailsPage.pressStartMobileScan()
                .openMobileScanSetup()
                .setAssessmentType("Mobile Express")
                .setEntitlement("Subscription")
                .setFrameworkType("iOS")
                .setTimeZone(FodConfig.SCAN_TIMEZONE)
                .setAuditPreference("Automated")
                .pressStartScanBtn()
                .uploadFile("payloads/fod/iGoat.ipa")
                .pressNextButton()
                .pressNextButton()
                .startScanButton.click();
        waitAndValidateUrl("MobileScanSetup");
    }

    @DataProvider(name = "importScanType", parallel = true)
    public static Object[][] scanType() {
        return new Object[][]{
                {FodCustomTypes.ScanType.Dynamic, "payloads/fod/dynscandata.fpr"},
                {FodCustomTypes.ScanType.Static, "payloads/fod/10JavaDefects_ORIGINAL.fpr"},
                {FodCustomTypes.ScanType.OpenSource, "payloads/fod/yarn_cyclonedx.json"}
        };
    }

    @DataProvider(name = "staticApplicationType", parallel = true)
    public static Object[][] applicationType() {
        return new Object[][]{
                {FodCustomTypes.ApplicationType.Mobile},
                {FodCustomTypes.ApplicationType.WebThickClient}
        };
    }

    public void importScans(ReleaseScansPage releaseScansPage, FodCustomTypes.ScanType scanType, String payload) {
        releaseScansPage.pressImportByScanType(scanType)
                .uploadFile(payload)
                .pressImportButton()
                .waitUntilProgressBarDisappear();
    }

    public ReleaseDetailsPage getReleaseDetailsPage(String applicationName, String releaseName) {
        return LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openDetailsFor(applicationName)
                .getReleaseByName(releaseName)
                .openReleaseDetails();
    }

    public void executeStaticScan(ReleaseDetailsPage releaseDetailsPage) {
        var scanSetupPage = releaseDetailsPage.pressStartStaticScan();
        scanSetupPage.getAdvancedSettingsPanel().expand();
        scanSetupPage.getLegacySettingsPanel().expand();
        scanSetupPage.getDevOpsAndIDEIntegrationPanel().expand();

        scanSetupPage.chooseAssessmentType("AUTO-STATIC")
                .chooseEntitlement("Subscription")
                .getAdvancedSettingsPanel()
                .expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("10");

        scanSetupPage.chooseAuditPreference(FodCustomTypes.AuditPreference.Automated)
                .pressStartScanBtn()
                .uploadFile("payloads/fod/static.java.zip")
                .pressNextBtn()
                .startScanButton.click();
    }

    public void waitAndValidateUrl(String currentScanPage) {
        WaitUtil.waitForTrue(() -> !url().contains(currentScanPage), Duration.ofSeconds(20), false);
        assertThat(url())
                .as("The " + url() + " should contain 'Release/{dd}/Scans")
                .containsPattern("^.*Releases/\\d{1,5}/Scans");
    }
}