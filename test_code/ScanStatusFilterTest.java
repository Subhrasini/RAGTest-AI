package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.exceptions.FodElementNotFoundException;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.dashboard.DashboardPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
public class ScanStatusFilterTest extends FodBaseTest {
    String inProgress = "In Progress";
    String completed = "Completed";
    String waiting = "Waiting";
    String cancelled = "Canceled";
    String scheduled = "Scheduled";
    String[] adminDashboardTabs = new String[]{"Static", "Dynamic", "Mobile"};
    ApplicationDTO staticApp, dynamicApp, mobileApp;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;
    MobileScanDTO mobileScanDTO;
    FodCustomTypes.ScansDetailsPageStatus expectedPauseScanStatus = FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan Status Filters in Tenant Page")
    @Test(groups = {"regression"})
    public void verifyScanStatusFilterInTenant() {
        var applicationPage = LogInActions.tamUserLogin(defaultTenantDTO);
        var dynamicStatusOptions = applicationPage.filters.expandAllFilters()
                .setFilterByName("Dynamic Scan Status")
                .getAllOptions();
        for (var dynamicOptions : dynamicStatusOptions) {
            switch (dynamicOptions) {
                case "Scheduled":
                    log.info("Just skip Scheduled status!");
                    break;
                case "In Progress":
                    applicationPage.filters.expandAllFilters().setFilterByName("Dynamic Scan Status")
                            .clickFilterOptionByName(inProgress);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Dynamic Scan Status:").getValue())
                            .as("Dynamic scans which is in progress should be displayed")
                            .isEqualTo(inProgress);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Completed":
                    applicationPage.filters.expandAllFilters().setFilterByName("Dynamic Scan Status")
                            .clickFilterOptionByName(completed);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Dynamic Scan Status:").getValue())
                            .as("Dynamic scans which is completed should be displayed")
                            .isEqualTo(completed);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Canceled":
                    applicationPage.filters.expandAllFilters().setFilterByName("Dynamic Scan Status")
                            .clickFilterOptionByName(cancelled);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Dynamic Scan Status:").getValue())
                            .as("Dynamic scans which is cancelled should be displayed")
                            .isEqualTo(cancelled);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Waiting":
                    applicationPage.filters.expandAllFilters().setFilterByName("Dynamic Scan Status")
                            .clickFilterOptionByName(waiting);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Dynamic Scan Status:").getValue())
                            .as("Dynamic scans which is cancelled should be displayed")
                            .isEqualTo(waiting);
                    applicationPage.appliedFilters.clearAll();
                    break;
                default:
                    throw new FodElementNotFoundException(dynamicOptions + " status not found");
            }

            applicationPage = page(YourApplicationsPage.class);
        }

        var mobileStatusOptions = applicationPage.filters.expandAllFilters()
                .setFilterByName("Mobile Scan Status")
                .getAllOptions();
        for (var mobileOptions : mobileStatusOptions) {
            switch (mobileOptions) {
                case "Scheduled":
                    log.info("Just skip Scheduled status!");
                    break;
                case "In Progress":
                    applicationPage.filters.expandAllFilters().setFilterByName("Mobile Scan Status")
                            .clickFilterOptionByName(inProgress);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Mobile Scan Status:").getValue())
                            .as("Mobile scans which is in progress should be displayed")
                            .isEqualTo(inProgress);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Completed":
                    applicationPage.filters.expandAllFilters().setFilterByName("Mobile Scan Status")
                            .clickFilterOptionByName(completed);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Mobile Scan Status:").getValue())
                            .as("Mobile scans which is completed should be displayed")
                            .isEqualTo(completed);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Canceled":
                    applicationPage.filters.expandAllFilters().setFilterByName("Mobile Scan Status")
                            .clickFilterOptionByName(cancelled);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Mobile Scan Status:").getValue())
                            .as("Mobile scans which is cancelled should be displayed")
                            .isEqualTo(cancelled);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Waiting":
                    applicationPage.filters.expandAllFilters().setFilterByName("Mobile Scan Status")
                            .clickFilterOptionByName(waiting);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Mobile Scan Status:").getValue())
                            .as("Mobile scans which is cancelled should be displayed")
                            .isEqualTo(waiting);
                    applicationPage.appliedFilters.clearAll();
                    break;
                default:
                    throw new FodElementNotFoundException(applicationPage + "status not found");
            }
            applicationPage = page(YourApplicationsPage.class);
        }

        var staticStatusOptions = applicationPage.filters.expandAllFilters()
                .setFilterByName("Static Scan Status")
                .getAllOptions();
        for (var staticOptions : staticStatusOptions) {
            switch (staticOptions) {
                case "Queued":
                    log.info("Just skip Queued status!");
                    break;
                case "In Progress":
                    applicationPage.filters.expandAllFilters().setFilterByName("Static Scan Status")
                            .clickFilterOptionByName(inProgress);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Static Scan Status:").getValue())
                            .as("Static scans which is in progress should be displayed")
                            .isEqualTo(inProgress);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Completed":
                    applicationPage.filters.expandAllFilters().setFilterByName("Static Scan Status")
                            .clickFilterOptionByName(completed);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Static Scan Status:").getValue())
                            .as("Static scans which is completed should be displayed")
                            .isEqualTo(completed);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Canceled":
                    applicationPage.filters.expandAllFilters().setFilterByName("Static Scan Status")
                            .clickFilterOptionByName(cancelled);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Static Scan Status:").getValue())
                            .as("Static scans which is cancelled should be displayed")
                            .isEqualTo(cancelled);
                    applicationPage.appliedFilters.clearAll();
                    break;
                case "Waiting":
                    applicationPage.filters.expandAllFilters().setFilterByName("Static Scan Status")
                            .clickFilterOptionByName(waiting);
                    assertThat(applicationPage.appliedFilters.getFilterByName("Static Scan Status:").getValue())
                            .as("Static scans which is cancelled should be displayed")
                            .isEqualTo(waiting);
                    applicationPage.appliedFilters.clearAll();
                    break;
                default:
                    throw new FodElementNotFoundException(applicationPage + " status not found");
            }
            applicationPage = page(YourApplicationsPage.class);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan Status Filters in Admin Dashboard")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyScanStatusFilterInTenant"}, alwaysRun = true)
    public void verifyScanStatusInAdminDashboard() {
        var dashboardPage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        for (var dashboardTab : adminDashboardTabs) {
            DashboardPage dashboardTabs;
            switch (dashboardTab) {
                case "Static":
                    dashboardTabs = dashboardPage.openStaticTab();
                    var staticOptions = dashboardTabs.filters.expandAllFilters()
                            .setFilterByName("Scan Status")
                            .getAllOptions();
                    for (var staticStatus : staticOptions) {
                        switch (staticStatus) {
                            case "In Progress":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(inProgress);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Static scans which is in progress should be displayed")
                                        .isEqualTo(inProgress);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Scheduled":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(scheduled);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Static scans which is scheduled should be displayed")
                                        .isEqualTo(scheduled);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Waiting":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(waiting);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Static scans which is in waiting should be displayed")
                                        .isEqualTo(waiting);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            default:
                                throw new FodElementNotFoundException(dashboardPage + " status not found");
                        }
                    }
                    break;
                case "Dynamic":
                    dashboardTabs = dashboardPage.openDynamicTab();
                    var dynamicOptions = dashboardTabs.filters.expandAllFilters()
                            .setFilterByName("Scan Status").getAllOptions();
                    for (var dynamicStatus : dynamicOptions) {
                        switch (dynamicStatus) {
                            case "In Progress":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(inProgress);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Dynamic scans which is in progress should be displayed")
                                        .isEqualTo(inProgress);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Scheduled":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(scheduled);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Dynamic scans which is scheduled should be displayed")
                                        .isEqualTo(scheduled);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Waiting":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(waiting);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Dynamic scans which is waiting should be displayed")
                                        .isEqualTo(waiting);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            default:
                                throw new FodElementNotFoundException(dashboardPage + " status not found");
                        }
                    }
                    break;
                case "Mobile":
                    dashboardTabs = dashboardPage.openMobileTab();
                    var mobileOptions = dashboardTabs.filters.expandAllFilters()
                            .setFilterByName("Scan Status").getAllOptions();
                    for (var mobileStatus : mobileOptions) {
                        switch (mobileStatus) {
                            case "In Progress":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(inProgress);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Mobile scans which is in progress should be displayed")
                                        .isEqualTo(inProgress);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Scheduled":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(scheduled);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Mobile scans which is scheduled should be displayed")
                                        .isEqualTo(scheduled);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            case "Waiting":
                                dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                                        .clickFilterOptionByName(waiting);
                                assertThat(dashboardTabs.appliedFilters.getFilterByName("Scan Status:")
                                        .getValue())
                                        .as("Mobile scans which is waiting should be displayed")
                                        .isEqualTo(waiting);
                                dashboardTabs.appliedFilters.clearAll();
                                break;
                            default:
                                throw new FodElementNotFoundException(dashboardPage + " status not found");
                        }
                    }
                    break;
                default:
                    throw new FodElementNotFoundException(dashboardPage + " tab not found");
            }
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify different scan status filters of Static Scan")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyScanStatusInAdminDashboard"}, alwaysRun = true)
    public void verifyStaticScanStatusFilter() {
        staticApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(staticApp, defaultTenantDTO, true);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.InProgress);

        var dashboardPage = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                .clickFilterOptionByName(inProgress);
        dashboardPage.findWithSearchBox(staticApp.getApplicationName());
        WaitUtil.waitFor(WaitUtil.Operator.Equals, false, Table::isEmpty,
                Duration.ofMinutes(3), true);
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getApplication())
                .as("Application should be displayed in the Dashboard page  ")
                .isEqualTo(staticApp.getApplicationName());
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getScanStatus())
                .as("Scan Status of the Application should be In-Progress ")
                .isEqualTo(inProgress);
        dashboardPage.appliedFilters.clearAll();
        BrowserUtil.clearCookiesLogOff();

        pauseStaticScan();
        BrowserUtil.clearCookiesLogOff();

        var dashboardPageStatic = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .openStaticTab();
        dashboardPage.findWithSearchBox(staticApp.getApplicationName());
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getApplication())
                .as("Application details should be displayed on dashboard page ")
                .isEqualTo(staticApp.getApplicationName());
        Supplier<String> scan = () -> dashboardPageStatic.getAllDeliveryQueues().get(0).getScanStatus();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, waiting, scan, Duration.ofMinutes(2), true);
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getScanStatus())
                .as("After pausing the scan , Static scan status should be in Waiting ")
                .isEqualTo(waiting);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify different scan status filters of Dynamic Scan")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyStaticScanStatusFilter"}, alwaysRun = true)
    public void verifyDynamicScanStatusFilter() {
        dynamicApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);

        var dashboardPage = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .openDynamicTab();
        dashboardPage.filters.expandAllFilters().setFilterByName("Scan Status")
                .clickFilterOptionByName(inProgress);
        dashboardPage.findWithSearchBox(dynamicApp.getApplicationName());
        WaitUtil.waitFor(WaitUtil.Operator.Equals, false, Table::isEmpty,
                Duration.ofMinutes(3), true);
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getApplication())
                .as("Application details should be displayed in dashboard page ")
                .isEqualTo(dynamicApp.getApplicationName());
        dashboardPage.appliedFilters.clearAll();
        BrowserUtil.clearCookiesLogOff();

        pauseDynamicScan();
        BrowserUtil.clearCookiesLogOff();

        var dashboardPageDynamic = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .openDynamicTab();
        dashboardPage.findWithSearchBox(dynamicApp.getApplicationName());
        assertThat(dashboardPageDynamic.getAllDeliveryQueues().get(0).getApplication())
                .as("App name ")
                .isEqualTo(dynamicApp.getApplicationName());
        Supplier<String> scan = () -> dashboardPageDynamic.getAllDeliveryQueues().get(0).getScanStatus();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, "Waiting - Customer", scan, Duration.ofMinutes(2), true);
        assertThat(dashboardPageDynamic.getAllDeliveryQueues().get(0).getScanStatus())
                .as("After pausing the scan , Dynamic scan status should be in Waiting ")
                .isEqualTo("Waiting - Customer");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify different scan status filters of Mobile Scan")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyDynamicScanStatusFilter"}, alwaysRun = true)
    public void verifyMobileScanStatusFilter() {
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        var dashboardPage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME,
                        FodConfig.ADMIN_PASSWORD)
                .openMobileTab();
        dashboardPage.findWithSearchBox(mobileApp.getApplicationName());
        WaitUtil.waitFor(WaitUtil.Operator.Equals, false,
                Table::isEmpty, Duration.ofMinutes(3), true);
        Supplier<String> status = () -> dashboardPage.getAllDeliveryQueues().get(0).getScanStatus();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, inProgress, status, Duration.ofMinutes(2), true);
        assertThat(dashboardPage.getAllDeliveryQueues().get(0).getApplication())
                .as("Application details should be displayed in dashboard page ")
                .isEqualTo(mobileApp.getApplicationName());
        dashboardPage.appliedFilters.clearAll();
        BrowserUtil.clearCookiesLogOff();

        pauseMobileScan();
        BrowserUtil.clearCookiesLogOff();

        var dashboardPageMobile = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .openMobileTab();
        dashboardPage.findWithSearchBox(mobileApp.getApplicationName());
        assertThat(dashboardPageMobile.getAllDeliveryQueues().get(0).getApplication())
                .as("Application details should be displayed in dashboard page ")
                .isEqualTo(mobileApp.getApplicationName());
        Supplier<String> scan = () -> dashboardPageMobile.getAllDeliveryQueues().get(0).getScanStatus();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, "Waiting - Customer", scan, Duration.ofMinutes(2), true);
        assertThat(dashboardPageMobile.getAllDeliveryQueues().get(0).getScanStatus())
                .as("After pausing the scan , Mobile scan status should be in Waiting ")
                .isEqualTo("Waiting - Customer");
    }

    public void pauseStaticScan() {
        var staticScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByAppName(staticApp.getApplicationName())
                .openDetails().pressPauseButton().pressOkButton();
        refresh();
        assertThat(staticScanOverviewPage.getStatus())
                .as("Static Scan status should be changed to : " + expectedPauseScanStatus.getTypeValue())
                .isEqualTo(expectedPauseScanStatus.getTypeValue());
    }

    public void pauseDynamicScan() {
        var dynamicScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName())
                .pressPauseButton().pressOkButton();
        refresh();
        assertThat(dynamicScanOverviewPage.getStatus())
                .as("Dynamic Scan status should be changed to : " + expectedPauseScanStatus.getTypeValue())
                .isEqualTo(expectedPauseScanStatus.getTypeValue());
    }

    public void pauseMobileScan() {
        var mobileScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                .openDetailsFor(mobileApp.getApplicationName())
                .pressPauseButton().pressOkButton();
        refresh();
        assertThat(mobileScanOverviewPage.getStatus())
                .as("Mobile Scan status should be changed on : " + expectedPauseScanStatus.getTypeValue()
                        + " or " + FodCustomTypes.ScansDetailsPageStatus.WaitingScanImported.getTypeValue())
                .containsAnyOf(FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue(),
                        FodCustomTypes.ScansDetailsPageStatus.WaitingScanImported.getTypeValue());
    }
}