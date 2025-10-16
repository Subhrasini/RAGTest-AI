package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.LocalTime;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class ReleaseOverviewSonatypeIssueTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public ReleaseDTO releaseDTO;
    public StaticScanDTO staticScanDTO;
    public String fileName = "payloads/fod/Sonatype vulns with yellow banner_CUT.zip";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Start a static scan with an open source component enabled")
    @Test(groups = {"hf", "regression"})
    public void prepareTestData() {
        releaseDTO = ReleaseDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        ReleaseActions.createRelease(applicationDTO, releaseDTO);
        staticScanDTO.setLanguageLevel("11");
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, releaseDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();
        var scanJobsPage = page(ScanJobsPage.class);
        var staticJobCell = scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), true).getLastJob();
        staticJobCell.waitForJobStatus(FodCustomTypes.JobStatus.Success);
        StaticScanActions.completeStaticScan(applicationDTO, false);
        assertThat(scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), false).getStatus())
                .as("Validate scan should be completed")
                .isEqualTo(FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("527007")
    @Description("Fixed validated sonatype vulnerabilities still included in the count of issues in the release overview page")
    @Test(dependsOnMethods = {"prepareTestData"}, groups = {"hf", "regression"})
    public void validateNumberOfIssuesTest() {
        var yourReleasesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases();
        var releaseDetailsPage = yourReleasesPage
                .openDetailsForRelease(applicationDTO.getApplicationName(), releaseDTO.getReleaseName());
        assertThat(releaseDetailsPage.getStaticScanStatus())
                .as("Status should be completed").isEqualToIgnoringCase("Completed");

        var issuesPage = releaseDetailsPage.openIssues();
        int issuesCountBefore = issuesPage.getAllCount();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type");
        var issue = issuesPage.getAllIssues().get(0);

        issuesPage.selectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        issue.setAuditorStatus(FodCustomTypes.AuditorStatus.RiskAccepted.getTypeValue());
        issue.submitChanges();
        assertThat(issuesPage.getAllCount())
                .as("Total issues count should decrease after suppressing static issues").isLessThan(issuesCountBefore);
        IssuesCounters issuesCounters = new IssuesCounters(
                issuesPage.getCriticalCount(),
                issuesPage.getHighCount(),
                issuesPage.getMediumCount(),
                issuesPage.getLowCount(),
                issuesPage.getAllCount()
        );

        releaseDetailsPage.openOverview();
        var endTime = LocalTime.now().plusMinutes(10);
        do {
            if (releaseDetailsPage.getCriticalCount() == issuesCounters.getCritical()) {
                break;
            }
            sleep(20000);
            refresh();
        } while (LocalTime.now().isBefore(endTime));
        IssuesActions.validateOverviewIssuesTenant(issuesCounters);
    }
}
