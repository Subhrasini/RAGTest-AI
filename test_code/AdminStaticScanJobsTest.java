package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.elements.AppliedFilters;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticScanCell;
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

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class AdminStaticScanJobsTest extends FodBaseTest {

    TenantDTO firstTenant, secondTenant;
    ApplicationDTO firstApp, secondApp, thirdApp;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin Static Scan Jobs Test")
    @Test(groups = {"regression"}, enabled = true)
    public void adminStaticScanJobsTest() {
        AllureReportUtil.info("<<< Creating first Tenant with enabled 'Allow scanning with no entitlements' >>>");
        firstTenant = TenantDTO.createDefaultInstance();
        firstTenant.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        firstTenant.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        firstTenant.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(firstTenant);

        AllureReportUtil.info("<<< Creating second Tenant with enabled 'Allow scanning with no entitlements' >>>");
        secondTenant = TenantDTO.createDefaultInstance();
        secondTenant.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        secondTenant.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        secondTenant.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(secondTenant, false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Creating first Application >>>");
        firstApp = ApplicationDTO.createDefaultInstance();
        firstApp.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        ApplicationActions.createApplication(firstApp, firstTenant, true);

        AllureReportUtil.info("<<< Creating first Static Scan >>>");
        var firstStaticScan = StaticScanDTO.createDefaultInstance();
        firstStaticScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        firstStaticScan.setFileToUpload("payloads/fod/JavaTestPayload.zip");
        firstStaticScan.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        firstStaticScan.setLanguageLevel("1.9");
        StaticScanActions.createStaticScan(firstStaticScan, firstApp, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Creating second Application >>>");
        secondApp = ApplicationDTO.createDefaultInstance();
        secondApp.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        ApplicationActions.createApplication(secondApp, secondTenant, true);

        AllureReportUtil.info("<<< Creating second Static Scan >>>");
        var secondStaticScan = StaticScanDTO.createDefaultInstance();
        secondStaticScan.setAssessmentType("Static Assessment");
        secondStaticScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        secondStaticScan.setFileToUpload("payloads/fod/PerfStuffExtractor.zip");
        secondStaticScan.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        secondStaticScan.setLanguageLevel("3.5");
        StaticScanActions.createStaticScan(secondStaticScan, secondApp, FodCustomTypes.SetupScanPageStatus.InProgress);

        AllureReportUtil.info("<<< Creating third Application >>>");
        thirdApp = ApplicationDTO.createDefaultInstance();
        thirdApp.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        ApplicationActions.createApplication(thirdApp, secondTenant, false);

        AllureReportUtil.info("<<< Creating third Static Scan >>>");
        var thirdStaticScan = StaticScanDTO.createDefaultInstance();
        thirdStaticScan.setAssessmentType("Static Assessment");
        thirdStaticScan.setAuditPreference(FodCustomTypes.AuditPreference.Automated);
        thirdStaticScan.setFileToUpload("payloads/fod/PerfStuffExtractor.zip");
        thirdStaticScan.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        thirdStaticScan.setLanguageLevel("3.5");
        StaticScanActions.createStaticScan(thirdStaticScan, thirdApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Completing second Static Scan >>>");
        StaticScanActions.completeStaticScan(secondApp, true);

        AllureReportUtil.info("<<< Cloning Static Scan Job >>>");
        var jobsPage = page(ScanJobsPage.class);
        jobsPage.findScanByAppName(firstApp.getApplicationName()).getLastJob()
                .clickClone().setRescanReason("Some reason").pressRescan();
        var scan = jobsPage.findScanByAppName(firstApp.getApplicationName());
        assertThat(scan.getLastJob().getStatus())
                .as("Status should be 'Pending'")
                .containsAnyOf(FodCustomTypes.JobStatus.Pending.getTypeValue(),
                        FodCustomTypes.JobStatus.Scanning.getTypeValue());

        AllureReportUtil.info("<<< Creating and rejecting Audit for Static Scan Job >>>");
        var primaryJob = scan.getFirstJob();
        primaryJob.clickAction(Actions.JobAction.ReleaseToAudit).waitForJobStatus(FodCustomTypes.JobStatus.AuditPending);
        primaryJob.clickAction(Actions.JobAction.ClaimAudit).waitForJobStatus(FodCustomTypes.JobStatus.Auditing);
        primaryJob.clickAction(Actions.JobAction.UnclaimAudit).waitForJobStatus(FodCustomTypes.JobStatus.AuditPending);
        primaryJob.clickAction(Actions.JobAction.ClaimAudit).waitForJobStatus(FodCustomTypes.JobStatus.Auditing);
        primaryJob.clickAction(Actions.JobAction.RejectAudit).waitForJobStatus(FodCustomTypes.JobStatus.AuditRejected);

        AllureReportUtil.info("<<< Verifying applied by default Job Filters >>>");
        new AdminTopNavbar().openStatic();
        AppliedFilters appliedFilters = new AppliedFilters();
        assertThat(appliedFilters.getFilterByName("Has Alerts").getValue())
                .as("'Has Alerts' filter should be equals 'True'").isEqualTo("True");
        assertThat(appliedFilters.getFilterByName("Canned Query").getValue())
                .as("'Canned Query equals' filter should be equals 'Open Scans'")
                .isEqualTo("Open Scans");

        AllureReportUtil.info("<<< Verifying availability and visibility of Job Filters >>>");
        appliedFilters.clearAll();
        Filters filter = new Filters();
        filter.expandSideFiltersDrawer();
        filter.setFilterByName("Completed On").expand();
        filter.setFilterByName("Technology").expand();
        filter.setFilterByName("Technology Version").expand();
        filter.setFilterByName("Analysis Status").expand();
        filter.setFilterByName("Tenant").expand();
        filter.setFilterByName("Assessment Type").expand();
        filter.setFilterByName("Audit Preference").expand();

        AllureReportUtil.info("<<< Applying Job Filters >>>");
        filter.setFilterByName("Tenant").clickFilterOptionByName(firstTenant.getTenantName());
        assertThat(appliedFilters.getFilterByName("Tenant").getValue())
                .as("'Tenant' filter should be equals '{firstTenant}'")
                .isEqualTo(firstTenant.getTenantName());
        var scans = new StaticScanCell().getAll();
        assertThat(scans).as("Only filtered tenant is shown.").hasSize(1);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(firstTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("All filtered scans are shown.").hasSize(3);
        assertThat(scans.get(0).getTenantName()).as("First filtered tenant is shown.")
                .isEqualTo(firstTenant.getTenantName());
        assertThat(scans.get(1).getTenantName()).as("Second filtered tenant is shown.")
                .isEqualTo(secondTenant.getTenantName());

        filter.setFilterByName("Audit Preference").expand().clickFilterOptionByName("Manual Audit");
        filter.setFilterByName("Analysis Status").expand().clickFilterOptionByName("Completed");
        assertThat(appliedFilters.getFilterByName("Analysis Status").getValue())
                .as("'Analysis Status' filter should be equals 'Completed'").isEqualTo("Completed");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 1").hasSize(1);
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Technology").expand()
                .clickFilterOptionByName(FodCustomTypes.TechnologyStack.DotNet.getTypeValue());
        assertThat(appliedFilters.getFilterByName("Technology").getValue())
                .as("'Technology' filter should be equals '.NET'")
                .isEqualTo(FodCustomTypes.TechnologyStack.DotNet.getTypeValue());
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 2").hasSize(2);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(secondTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Technology").expand()
                .clickFilterOptionByName(FodCustomTypes.TechnologyStack.JAVA.getTypeValue());
        assertThat(appliedFilters.getFilterByName("Technology").getValue())
                .as("'Technology' filter should be equals 'JAVA/J2EE'")
                .isEqualTo(FodCustomTypes.TechnologyStack.JAVA.getTypeValue());
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 1").hasSize(1);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(firstTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Technology Version").expand().clickFilterOptionByName(".NET - 3.5");
        assertThat(appliedFilters.getFilterByName("Technology Version").getValue())
                .as("'Technology Version' filter should be equals '.NET - 3.5'")
                .isEqualTo(".NET - 3.5");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 2").hasSize(2);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(secondTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Assessment Type").expand().clickFilterOptionByName("AUTO-STATIC");
        assertThat(appliedFilters.getFilterByName("Assessment Type").getValue())
                .as("'Assessment Type' filter should be equals 'AUTO-STATIC'")
                .isEqualTo("AUTO-STATIC");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 1").hasSize(1);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(firstTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Assessment Type").expand().clickFilterOptionByName("Static Assessment");
        assertThat(appliedFilters.getFilterByName("Assessment Type").getValue())
                .as("'Assessment Type' filter should be equals 'Static Assessment'")
                .isEqualTo("Static Assessment");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 2").hasSize(2);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(secondTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Audit Preference").expand().clickFilterOptionByName("Automated Audit");
        assertThat(appliedFilters.getFilterByName("Audit Preference").getValue())
                .as("'Audit Preference' filter should be equals 'Automated Audit'")
                .isEqualTo("Automated Audit");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 1").hasSize(1);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(secondTenant.getTenantName());
        appliedFilters.clearAll();

        filter.setFilterByName("Tenant").expand().clickFilterCheckboxByName(firstTenant.getTenantName())
                .clickFilterCheckboxByName(secondTenant.getTenantName()).apply();
        filter.setFilterByName("Audit Preference").expand().clickFilterOptionByName("Manual Audit");
        assertThat(appliedFilters.getFilterByName("Audit Preference").getValue())
                .as("'Audit Preference' filter should be equals 'Manual Audit'")
                .isEqualTo("Manual Audit");
        scans = new StaticScanCell().getAll();
        assertThat(scans).as("Scans count as expected: 2").hasSize(2);
        assertThat(scans.get(0).getTenantName()).as("Only filtered tenant is shown.")
                .isEqualTo(firstTenant.getTenantName());
    }

    @Owner("oradchenko@opentext.com")
    @FodBacklogItem("486024")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("When a scan is canceled or completed (and released), any running clones on that scan should be aborted")
    @Test(groups = {"hf", "regression"}, enabled = true)
    public void abortedClonedStaticJobTest() {
        var tenant = defaultTenantDTO;
        var app = ApplicationDTO.createDefaultInstance();
        var scanDto = StaticScanDTO.createDefaultInstance();
        scanDto.setFileToUpload("payloads/fod/static.java.zip");
        AllureReportUtil.info("Verify with auto scan completion and cancel from tenant site");
        LogInActions.tamUserLogin(tenant).openYourApplications();
        ApplicationActions.createApplication(app).openScans();
        StaticScanActions.createStaticScan(scanDto, app);
        BrowserUtil.openNewTab();
        Selenide.switchTo().window(1);
        var jobsPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        var scan = jobsPage.findScanByAppName(app.getApplicationName());
        var firstJob = scan.getLastJob();
        firstJob.waitForJobStatus(FodCustomTypes.JobStatus.Scanning);
        firstJob.clickClone().setRescanReason("Some reason").pressRescan();
        Selenide.switchTo().window(0);
        StaticScanActions.cancelScanTenant(app);
        Selenide.switchTo().window(1);

        scan = jobsPage.getLatestScanByAppDto(app);
        var jobs = scan.getJobs();
        assertThat(jobs).as("There should be 2 jobs").hasSize(2);
        firstJob = jobs.get(0);
        firstJob.waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);
        assertThat(firstJob.getStatus()).as("Job should be aborted")
                .isIn(FodCustomTypes.JobStatus.Aborted.getTypeValue(), FodCustomTypes.JobStatus.Aborting.getTypeValue());
        var secondJob = jobs.get(1);
        secondJob.waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);
        assertThat(secondJob.getStatus()).as("Job should be aborted")
                .isIn(FodCustomTypes.JobStatus.Aborted.getTypeValue(), FodCustomTypes.JobStatus.Aborting.getTypeValue());

        AllureReportUtil.info("Verify with auto scan completion and cancel from admin site");
        Selenide.switchTo().window(0);
        StaticScanActions.createStaticScan(scanDto, app);
        Selenide.switchTo().window(1);
        scan = jobsPage.getLatestScanByAppDto(app);
        firstJob = scan.getFirstJob();
        firstJob.waitForJobStatus(FodCustomTypes.JobStatus.Scanning);
        firstJob.clickClone().setRescanReason("Some reason").pressRescan();
        scan = jobsPage.getLatestScanByAppDto(app);
        jobsPage = scan.openDetails().pressCancelBtn().setReason("Other").setRefund(false).pressOkBtn()
                .adminTopNavbar.openStatic();
        scan = jobsPage.getLatestScanByAppDto(app);
        jobs = scan.getJobs();
        assertThat(jobs).as("There should be 2 jobs").hasSize(2);
        firstJob = jobs.get(0);
        firstJob.waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);
        assertThat(firstJob.getStatus()).as("Job should be aborted")
                .isIn(FodCustomTypes.JobStatus.Aborted.getTypeValue(), FodCustomTypes.JobStatus.Aborting.getTypeValue());
        scan = jobsPage.getLatestScanByAppDto(app);
        jobs = scan.getJobs();
        secondJob = jobs.get(1);
        secondJob.waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);
        assertThat(secondJob.getStatus()).as("Job should be aborted")
                .isIn(FodCustomTypes.JobStatus.Aborted.getTypeValue(), FodCustomTypes.JobStatus.Aborting.getTypeValue());

        AllureReportUtil.info("Verify with manual scan completion");
        scanDto.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        Selenide.switchTo().window(0);
        StaticScanActions.createStaticScan(scanDto, app);
        Selenide.switchTo().window(1);
        var groupName = "Auto-Group" + UniqueRunTag.generate();
        var scannerGroups = jobsPage.openScannerGroups();
        scannerGroups.pressAddGroup().setGroupName(groupName).pressSave();

        scan = scannerGroups.openScanJobs().getLatestScanByAppDto(app);
        var job = scan.getLastJob();
        job.waitForJobStatus(FodCustomTypes.JobStatus.Scanning);
        job.clickClone().setRescanReason("Some reason").chooseScannerGroup(groupName).pressRescan();
        job.releaseJob(true);
        assertThat(job.getStatus())
                .as("Validate scan status")
                .isEqualTo(FodCustomTypes.JobStatus.ImportSucceeded.getTypeValue());
        scan = jobsPage.getLatestScanByAppDto(app);
        jobs = scan.getJobs();
        assertThat(jobs).as("There should be 2 jobs").hasSize(2);
        firstJob = jobs.get(1);
        firstJob.waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);
        assertThat(firstJob.getStatus()).as("Job should be aborted")
                .isIn(FodCustomTypes.JobStatus.Aborted.getTypeValue(), FodCustomTypes.JobStatus.Aborting.getTypeValue());
    }

    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("732001")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin portal :Static scans :Scan Job Id needs to be a hyper link in the Scans history tab")
    @Test(groups = {"regression"}, enabled = true)
    public void verifyStaticScanJobIDLinkTest() {

        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var scanDto = StaticScanDTO.createDefaultInstance();
        scanDto.setFileToUpload("payloads/fod/static.java.zip");

        AllureReportUtil.info("Start a static scan and complete it");
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications();
        ApplicationActions.createApplication(applicationDTO).openScans();
        StaticScanActions.createStaticScan(scanDto, applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        var staticScanCell = StaticScanActions.completeStaticScan(applicationDTO, true)
                .getLatestScanByAppDto(applicationDTO);
        var jobId = staticScanCell.getFirstJob().getId();
        var historyPage = staticScanCell.openDetails().openHistory();
        var allRows = historyPage.getNotesScanJobIdRows();
        assertThat(allRows.size())
                .as("Verify the Notes column contain Scan Job Id")
                .isNotEqualTo(0);
        var allLinks = historyPage.getAllLinks();
        assertThat(allLinks.size())
                .as("Verify all the Scan Job Ids are hyperlink")
                .isEqualTo(allRows.size());
        allLinks.forEach(t -> assertThat(t)
                .as("Verify all the links are valid")
                .contains("/Static/JobDetails/" + jobId));

        assertThat(historyPage.clickOnScanJobIdLink().getPageTitle())
                .as("Verify when the Scan Job Id is clicked, the Scan Job details page is opened ")
                .isEqualTo("Details for scan job " + jobId);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        scanDto.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(scanDto, applicationDTO);

        AllureReportUtil.info("<<< Cloning Static Scan Job >>>");
        staticScanCell = LogInActions.adminLogIn().adminTopNavbar.openStatic().getLatestScanByAppDto(applicationDTO);
        var secondJobId = staticScanCell.getLastJob().waitForJobStatus(FodCustomTypes.JobStatus.Success)
                .clickClone().setRescanReason("testing").pressRescan()
                .getLatestScanByAppDto(applicationDTO)
                .getLastJob().waitForJobStatus(FodCustomTypes.JobStatus.Scanning).getId();
        historyPage = staticScanCell.getLatestScanByAppDto(applicationDTO).openDetails().openHistory();
        historyPage.getAllLinks().forEach(t -> assertThat(t)
                .as("Verify JobId is not listed if the Job is being cloned")
                .doesNotContain("JobDetails/" + secondJobId));

        var overviewPage = new AdminTopNavbar().openStatic().getLatestScanByAppDto(applicationDTO).openDetails();
        var prevScan = overviewPage.getPreviousScan().split(" ")[0];
        overviewPage.clickOnScanLink(prevScan);
        assertThat(overviewPage.getScanDetailStatus())
                .as("Verify Previous Scan still link to previous scan")
                .contains("Scan ID (" + prevScan + ")");

        var nextScan = overviewPage.getNextScan().split(" ")[0];
        var any = overviewPage.clickOnScanLink(nextScan);
        assertThat(overviewPage.getScanDetailStatus())
                .as("Verify Next Scan still link to next scan")
                .contains("Scan ID (" + nextScan + ")");

    }

}