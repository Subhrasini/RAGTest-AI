package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantActions;
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
import java.util.function.Supplier;

import static com.fortify.fod.common.custom_types.FodCustomTypes.TechnologyStack.Go;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("564009")
@Slf4j
public class GoPortalTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO app;
    String fileUpload = "payloads/fod/golang-example-app-master22.zip";
    String expectedNotification = "The AUTO-STATIC scan has been cancelled for Release.Additional user action may be " +
            "needed.Payload contains no scannable file extensions. Contact support for further clarification..";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should select technology stack as GO and verify scan should be cancelled without SC packaging ")
    @Test(groups = {"regression"})
    public void validateStaticScanWithoutScPackage() {
        AllureReportUtil.info("Create Tenant");
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Warning message should be displayed while selecting Go as Tech Stack");
        LogInActions.tamUserLogin(tenantDTO);
        app = ApplicationActions.createApplication(tenantDTO, false);
        var staticScanSetupPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(app)
                .openStaticScanSetup();
        staticScanSetupPage.chooseAssessmentType("AUTO-STATIC")
                .chooseEntitlement("Subscription")
                .chooseAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.Go);
        staticScanSetupPage.pressSaveBtn();
        Selenide.refresh();
        assertThat(staticScanSetupPage.getScanCentralWarningMessage())
                .as("Verify ScanCentral is recommended warning message should be displayed while selecting GO as TechStack")
                .containsIgnoringCase("ScanCentral is recommended for selected Technology Stack and Language Level for comprehensive scan results");

        AllureReportUtil.info("Start Scan without SC Packaging, scan should be cancelled");
        var staticScan = StaticScanDTO.createDefaultInstance();
        staticScan.setTechnologyStack(Go);

        staticScan.setFileToUpload(fileUpload);
        StaticScanActions.createStaticScan(staticScan, app);
        staticScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.SetupScanPageStatus.Canceled);
        BrowserUtil.clearCookiesLogOff();

        validateFailScan(app);
        BrowserUtil.clearCookiesLogOff();

        var myNotificationsPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openNotifications();
        expectedNotification = expectedNotification.replace("Release", app.getReleaseName());
        Supplier<String> note = () -> myNotificationsPage.getAllNotifications().get(0).getMessage();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, expectedNotification, note, Duration.ofMinutes(3), true);
        assertThat(note.get())
                .as("Verify notification message,scan should be cancelled")
                .isEqualTo(expectedNotification);
    }

    public void validateFailScan(ApplicationDTO app) {
        var staticScanJobsPage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();
        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(app.getApplicationName());
        assertThat(staticScanJobsPage.getAllScans()
                .get(0)
                .getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.Failed)
                .getStatus())
                .as("Verify Scan status should be Failed")
                .isEqualTo(FodCustomTypes.JobStatus.Failed.getTypeValue());
    }
}