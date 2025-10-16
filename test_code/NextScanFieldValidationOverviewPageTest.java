package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.dynamic.DynamicGridCell;
import com.fortify.fod.ui.pages.admin.mobile.queue_page.MobileScanCell;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticScanCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.stream.Collectors;

import static com.fortify.fod.common.custom_types.FodCustomTypes.TechnologyStack.DotNet;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("602013")
@Slf4j
public class NextScanFieldValidationOverviewPageTest extends FodBaseTest {
    ApplicationDTO mobileApp, dynamicApp, staticApp;
    DynamicScanDTO dynamicScanDTO;
    StaticScanDTO staticScanDTO;
    MobileScanDTO mobileScanDTO;
    ReleaseDTO secondReleaseMobApp, secondReleaseDynamicApp, copyReleaseStaticApp, copyReleaseStaticApp1,
            copyReleaseStaticApp2;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant and applications for the test")
    @Test(groups = {"regression"}, priority = 1)
    public void testDataPreparation() {
        AllureReportUtil.info("Test Data Preparation");
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(DotNet);
        staticScanDTO.setLanguageLevel("4.0");
        staticScanDTO.setFileToUpload("payloads/fod/webgoat-sc.zip");
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Previous and Next scan values for mobile scan release series")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyNextScanFieldForMobileScans() {
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        secondReleaseMobApp = ReleaseDTO.createDefaultInstance();
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, true);
        ReleaseActions.createRelease(mobileApp, secondReleaseMobApp);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions.completeMobileScan(mobileApp, true, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Repeat relaunch, scan and publishing same release to 2 more times");
        MobileScanActions.createMobileScan(defaultTenantDTO, mobileScanDTO, mobileApp, null, true,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.completeMobileScan(mobileApp, true, true);
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions.createMobileScan(defaultTenantDTO, mobileScanDTO, mobileApp, null, true,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.completeMobileScan(mobileApp, true, true);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.createMobileScan(defaultTenantDTO, mobileScanDTO, mobileApp, secondReleaseMobApp, true,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Open each mobile scan, and verify Previous Scan and Next Scan values");
        var adminLogin = LogInActions.adminLogIn().adminTopNavbar.openMobile();
        var mobileScans = adminLogin.findScanByAppDto(mobileApp).getAllScans().stream()
                .map(MobileScanCell::getScanId).collect(Collectors.toList());
        String firstMobileScanId = mobileScans.get(0).toString();
        String secondMobileScanId = mobileScans.get(1).toString();
        String thirdMobileScanId = mobileScans.get(2).toString();
        String fourthMobileScanId = mobileScans.get(3).toString();
        var overviewPage = adminLogin.openDetailsForScanId(firstMobileScanId);
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan value should be N/A")
                .isEqualTo("N/A");
        assertThat(overviewPage.getNextScan())
                .as("Next scan should have scan id")
                .isEqualTo(secondMobileScanId);
        overviewPage.adminTopNavbar.openMobile().appliedFilters.clearAll();
        overviewPage = adminLogin.openDetailsForScanId(secondMobileScanId);
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should reference a scan id")
                .isEqualTo(firstMobileScanId);
        assertThat(overviewPage.getNextScan())
                .as("Next scan should reference a scan id")
                .isEqualTo(thirdMobileScanId);
        overviewPage.adminTopNavbar.openMobile().appliedFilters.clearAll();
        overviewPage = adminLogin.openDetailsForScanId(thirdMobileScanId);
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should reference a scan id")
                .isEqualTo(secondMobileScanId);
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openMobile().appliedFilters.clearAll();
        overviewPage = adminLogin.openDetailsForScanId(fourthMobileScanId);
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan value should be N/A")
                .isEqualTo("N/A");
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Previous and Next scan values for dynamic scan release series")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyNextScanFieldForDynamicScans() {
        dynamicApp = ApplicationDTO.createDefaultInstance();
        secondReleaseDynamicApp = ReleaseDTO.createDefaultInstance();
        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        ReleaseActions.createRelease(dynamicApp, secondReleaseDynamicApp);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Repeat relaunch, scan and publishing same release to 2 more times");
        LogInActions.tamUserLogin(defaultTenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, secondReleaseDynamicApp.getReleaseName(),
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Open each dynamic scans and verify Previous Scan and Next Scan values");
        var adminLogin = LogInActions.adminLogIn().adminTopNavbar.openDynamic();
        adminLogin.appliedFilters.clearAll();
        var dynamicScans = adminLogin.findWithSearchBox(dynamicApp.getApplicationName()).getCells()
                .stream()
                .map(DynamicGridCell::getId)
                .collect(Collectors.toList());
        String firstDynamicScanId = dynamicScans.get(0).toString();
        String secondDynamicScanId = dynamicScans.get(1).toString();
        String thirdDynamicScanId = dynamicScans.get(2).toString();
        var overviewPage = adminLogin.findScanByScanId(dynamicScans.get(0)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan value should be N/A")
                .isEqualTo("N/A");
        assertThat(overviewPage.getNextScan())
                .as("Next scan should have scan id")
                .isEqualTo(secondDynamicScanId);
        overviewPage.adminTopNavbar.openDynamic();
        overviewPage = adminLogin.findScanByScanId(dynamicScans.get(1)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should reference a scan id")
                .isEqualTo(firstDynamicScanId);
        assertThat(overviewPage.getNextScan())
                .as("Next scan should reference a scan id")
                .isEqualTo(thirdDynamicScanId);
        overviewPage.adminTopNavbar.openDynamic();
        overviewPage = adminLogin.findScanByScanId(dynamicScans.get(2)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should reference a scan id")
                .isEqualTo(secondDynamicScanId);
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openDynamic();
        overviewPage = adminLogin.findScanByScanId(dynamicScans.get(3)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan value should be N/A")
                .isEqualTo("N/A");
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Previous and Next scan values for static scan release series")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyNextScanFieldForStaticScans() {
        staticApp = ApplicationDTO.createDefaultInstance();
        copyReleaseStaticApp = ReleaseDTO.createDefaultInstance();
        copyReleaseStaticApp1 = ReleaseDTO.createDefaultInstance();
        copyReleaseStaticApp2 = ReleaseDTO.createDefaultInstance();

        ApplicationActions.createApplication(staticApp, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(staticApp, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create copy state new releases from the completed original release");
        createCopyScanRelease(defaultTenantDTO, staticApp.getApplicationName(), staticApp.getReleaseName(),
                copyReleaseStaticApp.getReleaseName());
        createCopyScanRelease(defaultTenantDTO, staticApp.getApplicationName(), staticApp.getReleaseName(),
                copyReleaseStaticApp1.getReleaseName());
        createCopyScanRelease(defaultTenantDTO, staticApp.getApplicationName(), staticApp.getReleaseName(),
                copyReleaseStaticApp2.getReleaseName());

        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, copyReleaseStaticApp,
                FodCustomTypes.SetupScanPageStatus.Queued);
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, copyReleaseStaticApp1,
                FodCustomTypes.SetupScanPageStatus.Queued);
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, copyReleaseStaticApp2,
                FodCustomTypes.SetupScanPageStatus.Queued);

        AllureReportUtil.info("Open each static scan and verify Previous Scan and Next Scan values");
        var adminLogin = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        var staticScans = adminLogin.findScanByAppName(staticApp.getApplicationName(), true)
                .getAllScans()
                .stream()
                .map(StaticScanCell::getId)
                .collect(Collectors.toList());
        var overviewPage = adminLogin.findScanByScanId(staticScans.get(0)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan value should be N/A")
                .isEqualTo("N/A");
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openStatic();
        overviewPage = adminLogin.findScanByScanId(staticScans.get(1)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should contain scan id ")
                .isNotEqualTo("N/A").isNotEmpty();
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openStatic();
        overviewPage = adminLogin.findScanByScanId(staticScans.get(1)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should contain scan id")
                .isNotEqualTo("N/A").isNotEmpty();
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openStatic();
        overviewPage = adminLogin.findScanByScanId(staticScans.get(2)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should contain scan id")
                .isNotEqualTo("N/A").isNotEmpty();
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
        overviewPage.adminTopNavbar.openStatic();
        overviewPage = adminLogin.findScanByScanId(staticScans.get(3)).openDetails();
        assertThat(overviewPage.getPreviousScan())
                .as("Previous scan should contain scan id")
                .isNotEqualTo("N/A").isNotEmpty();
        assertThat(overviewPage.getNextScan())
                .as("Next scan value should be N/A")
                .isEqualTo("N/A");
    }

    @Description("By some reasons static scans can be halted and should be cancelled")
    @AfterClass
    public void checkHaltedScans() {
        setupDriver("checkHaltedScans");
        var scansPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        scansPage.appliedFilters.clearAll();
        var scans =
                scansPage.findWithSearchBox(staticApp.getApplicationName()).getAllScans()
                        .stream().map(StaticScanCell::getId).collect(Collectors.toList());

        scans.forEach(scan -> {
            try {
                var currentScan = scansPage.findScanByScanId(scan);
                if (!currentScan.getStatus().equals("Completed")) {
                    var staticPage = currentScan
                            .openDetails()
                            .pressCancelBtn()
                            .setReason("Other")
                            .pressOkBtn()
                            .adminTopNavbar.openStatic();
                    staticPage.appliedFilters.clearAll();
                }
            } catch (Error | Exception e) {
                log.info("There was an issue while cancelling scan: " + scan);
                log.info(e.getMessage());
                scansPage.adminTopNavbar
                        .openStatic()
                        .appliedFilters
                        .clearAll();
            }
        });
        attachTestArtifacts();
    }

    public void createCopyScanRelease(TenantDTO tenantDTO, String appName, String releaseName, String newRelease) {
        LogInActions.tamUserLogin(tenantDTO)
                .openYourApplications()
                .openDetailsFor(appName)
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(newRelease)
                .setSdlc("Development")
                .clickNext()
                .selectReleaseToCopyFrom(releaseName)
                .clickSave();
        BrowserUtil.clearCookiesLogOff();
    }
}
