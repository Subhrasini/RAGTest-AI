package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AuditPreferenceTest extends FodBaseTest {

    StaticScanDTO firstScan, secondScan;
    ApplicationDTO firstApp, secondApp;

    String expectedManualForOverviewPage = "Manual";
    String expectedAutomatedForOverviewPage = "Automated";
    String expectedManualForJobsPage = "Manual Audit";
    String ValidateAutomatedAudit = "Automated Audit";

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate scan with automated audit preference")
    @Test(groups = {"regression"})
    public void validateAutomatedAuditPreference() {
        firstApp = ApplicationDTO.createDefaultInstance();
        firstScan = StaticScanDTO.createDefaultInstance();
        firstScan.setAssessmentType("Static Premium");
        firstScan.setAuditPreference(FodCustomTypes.AuditPreference.Automated);
        firstScan.setFileToUpload("payloads/fod/static.java.zip");

        ApplicationActions.createApplication(firstApp, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(firstScan, firstApp, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        var jobsPage = waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded, firstApp);
        var scan = jobsPage.getLatestScanByAppDto(firstApp);
        assertThat(scan.getAuditPreference())
                .as("Audit preference on scan should be equal to: ")
                .isEqualTo(ValidateAutomatedAudit);

        var detailsPage = scan.openDetails();

        assertThat(detailsPage.getAuditPreference())
                .as("Audit preference on scan should be equal to: ")
                .isEqualTo(expectedAutomatedForOverviewPage);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate scan with manual audit preference")
    @Test(groups = {"regression"})
    public void validateManualAuditPreference() {
        secondApp = ApplicationDTO.createDefaultInstance();
        secondScan = StaticScanDTO.createDefaultInstance();
        secondScan.setAssessmentType("Static Premium");
        secondScan.setFileToUpload("payloads/fod/static.java.zip");
        secondScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        ApplicationActions.createApplication(secondApp, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(secondScan, secondApp, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var jobsPage = waitForJobStatus(FodCustomTypes.JobStatus.Success, secondApp);
        var scan = jobsPage.getLatestScanByAppDto(secondApp);
        assertThat(scan.getAuditPreference())
                .as("Audit preference on scan should be equal to: ")
                .isEqualTo(expectedManualForJobsPage);

        var detailsPage = scan.openDetails();

        assertThat(detailsPage.getAuditPreference())
                .as("Audit preference on scan should be equal to: ")
                .isEqualTo(expectedManualForOverviewPage);

        new AdminTopNavbar().openStatic().getLatestScanByAppDto(secondApp).getLastJob()
                .releaseJob(true).waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded);
    }

    public ScanJobsPage waitForJobStatus(FodCustomTypes.JobStatus status, ApplicationDTO applicationDTO) {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic().getLatestScanByAppDto(applicationDTO)
                .getLastJob().waitForJobStatus(40, status);

        return page(ScanJobsPage.class);
    }
}
