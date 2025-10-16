package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.dynamic.dynamic_scan_details.DynamicScanOverviewPage;
import com.fortify.fod.ui.pages.admin.mobile.mobile_scan_details.MobileScanOverviewPage;
import com.fortify.fod.ui.pages.admin.statics.scan.OverviewPage;
import com.fortify.fod.ui.pages.tenant.GlobalSearchPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static com.fortify.fod.ui.test.actions.StaticScanActions.cancelScanByReleaseNameAdmin;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class SearchTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    ReportDTO reportDTO;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant User should be able to use global search")
    @Test(groups = {"regression"})
    public void globalSearchTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        applicationDTO = ApplicationActions.createApplication();
        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setApplicationDTO(applicationDTO);

        DynamicScanActions.importDynamicScanTenant(applicationDTO, "payloads/fod/dynamic.zero.fpr");
        ReportActions.createReport(reportDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        var globalSearch = page(GlobalSearchPopup.class);
        var appName = globalSearch.openGlobalSearch().searchFor(applicationDTO.getApplicationName())
                .waitForResults()
                .openApplication(applicationDTO.getApplicationName())
                .getApplicationName();

        assertThat(appName).as("Should be opened application: " + applicationDTO.getApplicationName())
                .isEqualTo(applicationDTO.getApplicationName());

        var releaseName = globalSearch.openGlobalSearch().searchFor(applicationDTO.getReleaseName())
                .waitForResults()
                .openRelease(applicationDTO.getReleaseName())
                .getReleaseName();

        assertThat(releaseName).as("Should be opened releaseName: " + applicationDTO.getReleaseName())
                .isEqualTo(applicationDTO.getReleaseName());

        var report = globalSearch.openGlobalSearch().searchFor(reportDTO.getReportName())
                .waitForResults()
                .downloadReport(reportDTO.getReportName(), "pdf");

        assertThat(report)
                .as("File should be downloaded")
                .isNotNull().hasName(reportDTO.getReportName() + ".pdf");
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("732002")
    @Parameters({"Status"})
    @Description("validate Admin portal : Dashboard page : Ability to " +
            "search for a specific Scan by ScanID search")
    @Test(groups = {"regression"},
            dataProvider = "staticScanStatusData")
    public void searchingStaticScansByScanIdTest(String scanStatus) {
        var staticApp = ApplicationDTO.createDefaultInstance();
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        ApplicationActions.createApplication(staticApp, defaultTenantDTO, true);
        switch (scanStatus) {
            case "Queued":
                StaticScanActions.createStaticScan(staticScanDTO, staticApp,
                        FodCustomTypes.SetupScanPageStatus.Queued);
                break;
            case "In Progress":
                StaticScanActions.createStaticScan(staticScanDTO, staticApp,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            case "Canceled":
                StaticScanActions.createStaticScan(staticScanDTO, staticApp,
                        FodCustomTypes.SetupScanPageStatus.Queued);
                BrowserUtil.clearCookiesLogOff();
                cancelScanByReleaseNameAdmin(staticApp.getApplicationName(),
                        staticApp.getReleaseName());
                break;
            case "Completed":
                StaticScanActions.createStaticScan(staticScanDTO, staticApp,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                BrowserUtil.clearCookiesLogOff();
                StaticScanActions.completeStaticScan(staticApp, true);
                break;
            case "Waiting - Customer":
                StaticScanActions.createStaticScan(staticScanDTO, staticApp,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                BrowserUtil.clearCookiesLogOff();
                LogInActions.adminLogIn().adminTopNavbar.openStatic()
                        .findScanByAppName(staticApp.getApplicationName())
                        .openDetails().pressPauseButton().pressOkButton();
                break;
            default:
                throw new IllegalStateException("Unexpected status: " + scanStatus);
        }
        BrowserUtil.clearCookiesLogOff();
        var staticScanId = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .getLatestScanByAppDto(staticApp).getId();
        BrowserUtil.clearCookiesLogOff();
        var dashboardPage = LogInActions.adminLogIn();
        assertThat(dashboardPage.getSearchScansWithScanIdCheckBox().isDisplayed())
                .as("Search Scans with ScanId check-box under search text on Admin side>Dashboard")
                .isTrue();
        dashboardPage.setSearchScansWithScanIdCheckBox(true);
        dashboardPage.findWithSearchBox(staticScanId);
        assertThat(page(OverviewPage.class).getStatus())
                .as("Verify that check-box is enabled we can search scans with any status")
                .contains(scanStatus);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("732002")
    @Parameters({"Status"})
    @Description("validate Admin portal : Dashboard page : Ability to " +
            "search for a specific Scan by ScanID search")
    @Test(groups = {"regression"},
            dataProvider = "dynamicAndMobileScanStatusData")
    public void searchingDynamicScansByScanIdTest(String scanStatus) {
        var dynamicApp = ApplicationDTO.createDefaultInstance();
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        switch (scanStatus) {
            case "Scheduled":
                DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                break;
            case "In Progress":
                DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            case "Canceled":
                DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                DynamicScanActions.cancelDynamicScanAdmin(dynamicApp, true, false);
                break;
            case "Completed":
                DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                DynamicScanActions.completeDynamicScanAdmin(dynamicApp, true);
                break;
            case "Waiting - Customer":
                DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                        .openDetailsFor(dynamicApp.getApplicationName())
                        .pressPauseButton().pressOkButton();
                break;
            default:
                throw new IllegalStateException("Unexpected status: " + scanStatus);
        }
        BrowserUtil.clearCookiesLogOff();
        var dynamicScanId = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .getLatestScanByApplicationDto(dynamicApp).getId();
        BrowserUtil.clearCookiesLogOff();
        var dashboardPage = LogInActions.adminLogIn().openDynamicTab();
        assertThat(dashboardPage.getSearchScansWithScanIdCheckBox().isDisplayed())
                .as("Search Scans with ScanId check-box under search text on Admin side>Dashboard")
                .isTrue();
        dashboardPage.setSearchScansWithScanIdCheckBox(true);
        dashboardPage.findWithSearchBox(dynamicScanId);
        assertThat(page(DynamicScanOverviewPage.class).getScanStatus())
                .as("Verify that check-box is enabled we can search scans with any status")
                .contains(scanStatus);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("732002")
    @Parameters({"Status"})
    @Description("validate Admin portal : Dashboard page : Ability to " +
            "search for a specific Scan by ScanID search")
    @Test(groups = {"regression"},
            dataProvider = "dynamicAndMobileScanStatusData")
    public void searchingMobileScansByScanIdTest(String scanStatus) {
        var mobileApp = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, true);
        switch (scanStatus) {
            case "Scheduled":
                MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                break;
            case "In Progress":
                MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            case "Canceled":
                MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                LogInActions.adminLogIn().adminTopNavbar.openMobile()
                        .findWithSearchBox(mobileApp.getApplicationName())
                        .openDetailsFor(mobileApp.getApplicationName())
                        .pressCancelBtn()
                        .setReason("Other")
                        .setRefund(false)
                        .pressOkBtn();
                break;
            case "Completed":
                MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                MobileScanActions.completeMobileScan(mobileApp, true);
                break;
            case "Waiting - Customer":
                MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                        FodCustomTypes.SetupScanPageStatus.Scheduled);
                BrowserUtil.clearCookiesLogOff();
                LogInActions.adminLogIn().adminTopNavbar.openMobile()
                        .openDetailsFor(mobileApp.getApplicationName())
                        .pressPauseButton()
                        .pressOkButton();
                break;
            default:
                throw new IllegalStateException("Unexpected status: " + scanStatus);
        }
        BrowserUtil.clearCookiesLogOff();
        var mobileScanId = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                .getScanByApplicationDto(mobileApp).getScanId();
        BrowserUtil.clearCookiesLogOff();
        var dashboardPage = LogInActions.adminLogIn().openMobileTab();
        assertThat(dashboardPage.getSearchScansWithScanIdCheckBox().isDisplayed())
                .as("Search Scans with ScanId check-box under search text on Admin side>Dashboard")
                .isTrue();
        dashboardPage.setSearchScansWithScanIdCheckBox(true);
        dashboardPage.findWithSearchBox(mobileScanId);
        if (scanStatus.equals("Waiting - Customer")) {
            assertThat(page(MobileScanOverviewPage.class).getScanStatus())
                    .as("Verify that check-box is enabled we can search scans with any status")
                    .containsAnyOf(scanStatus,
                            FodCustomTypes.ScansDetailsPageStatus.WaitingScanImported.getTypeValue());
        } else {
            assertThat(page(MobileScanOverviewPage.class).getScanStatus())
                    .as("Verify that check-box is enabled we can search scans with any status")
                    .contains(scanStatus);
        }
    }

    @DataProvider(name = "dynamicAndMobileScanStatusData", parallel = true)
    public Object[][] dynamicAndMobileScanStatusData() {
        return new Object[][]{
                {FodCustomTypes.ScansDetailsPageStatus.Scheduled.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.InProgress.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.Canceled.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.Completed.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue()}
        };
    }

    @DataProvider(name = "staticScanStatusData", parallel = true)
    public Object[][] staticScanStatusData() {
        return new Object[][]{
                {FodCustomTypes.ScansDetailsPageStatus.Queued.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.InProgress.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.Canceled.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.Completed.getTypeValue()},
                {FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue()}
        };
    }
}
