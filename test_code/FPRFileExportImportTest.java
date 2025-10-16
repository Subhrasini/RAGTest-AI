package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
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

import java.io.File;
import java.io.FileNotFoundException;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class FPRFileExportImportTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public StaticScanDTO staticScanDTO;
    public ReleaseDTO secondRelease;
    public File expectedFile;
    String issueTitle = null;
    String filePath = null;
    public String fileName = "payloads/fod/Sonatype vulns with yellow banner_CUT.zip";
    String fprFileName = "payloads/fod/sph_scandata.fpr";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Start and complete a static scan")
    @Test(groups = {"hf", "regression"})
    public void prepareTestData() {

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
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

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Audit some vulnerabilities (Developer Status = In Remediation, Auditor Status = Remediation Required)")
    @Test(dependsOnMethods = {"prepareTestData"}, groups = {"hf", "regression"})
    public void setDeveloperAndAuditorStatusTest() {
        var yourReleasesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases();
        var releaseDetailsPage = yourReleasesPage
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());
        assertThat(releaseDetailsPage.getStaticScanStatus())
                .as("Status should be completed").isEqualToIgnoringCase("Completed");

        var issuesPage = releaseDetailsPage.openIssues();
        issuesPage.groupBy("Developer Status");
        var issueCell = issuesPage.getAllIssues().get(0).openDetails();
        issueTitle = issueCell.getTitle();
        var issue = issuesPage.getAllIssues().get(0);
        assertThat(issue.getDeveloperStatus())
                .as("Developer status shouldn't have In Remediation option selected")
                .isNotEqualToIgnoringCase("In Remediation");
        assertThat(issue.getAuditorStatus())
                .as("Auditor status shouldn't have Remediation Required option selected")
                .isNotEqualToIgnoringCase(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue());

        issue.setFalsePositiveChallenge("In Remediation");
        issue.setAuditorStatus(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue());
        issuesPage.groupBy("Scan Type");
        assertThat(issueTitle)
                .as("Validate title of the vulnerability")
                .contains(issuesPage.getAllIssues().get(0).getTitle());
        assertThat(issue.getDeveloperStatus())
                .as("In Remediation option is selected for Developer status")
                .isEqualToIgnoringCase("In Remediation");
        assertThat(issue.getAuditorStatus())
                .as("Remediation Required option is selected for Auditor status")
                .isEqualToIgnoringCase(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Download the scan result in tenant scans page")
    @Test(dependsOnMethods = {"setDeveloperAndAuditorStatusTest"}, groups = {"hf", "regression"})
    public void exportFPRFileTest() throws FileNotFoundException {
        LogInActions.tamUserLogin(defaultTenantDTO).openYourScans();
        expectedFile = page(YourScansPage.class)
                .getScanByType(applicationDTO, FodCustomTypes.ScanType.Static)
                .downloadResults();
        filePath = expectedFile.getPath();
        assertThat(expectedFile.getName()).as("Verify file is downloaded").contains("_scandata.fpr");
        assertThat(expectedFile.length()).as("File size should be greater than 0").isGreaterThan(0);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Import the downloaded FPR")
    @Test(dependsOnMethods = {"exportFPRFileTest"}, groups = {"hf", "regression"})
    public void ImportFPRFileTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        secondRelease = ReleaseDTO.createDefaultInstance();
        ReleaseActions.createRelease(applicationDTO, secondRelease);
        TenantScanActions.importScanTenant(applicationDTO, secondRelease, filePath, FodCustomTypes.ScanType.Static);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("547001")
    @Description("Developer Status and Auditor Status is getting reset when FPR is imported")
    @Test(dependsOnMethods = {"ImportFPRFileTest"}, groups = {"hf", "regression"})
    public void validateAuditedStatusTest() {
        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), secondRelease.getReleaseName())
                .openIssues();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type");
        assertThat(issueTitle)
                .as("Validate title of the vulnerability")
                .contains(issuesPage.getAllIssues().get(0).getTitle());
        assertThat(issuesPage.getAllIssues().get(0).getDeveloperStatus())
                .as("In Remediation option is selected for Developer status")
                .isEqualToIgnoringCase("In Remediation");
        assertThat(issuesPage.getAllIssues().get(0).getAuditorStatus())
                .as("Remediation Required option is selected for Auditor status")
                .isEqualToIgnoringCase(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue());
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("785028")
    @Description("FPR fails to import due to special characters in the FPR")
    @Test(groups = {"hf", "regression"})
    public void specialCharacterValidationInFPR() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.importScanAdmin(applicationDTO, fprFileName, true);

        assertThat(LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .getStaticScanStatus())
                .as("Validate Scan status should be completed")
                .isEqualTo("Completed");
    }
}