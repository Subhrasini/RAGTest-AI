package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticJobCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.testng.TestInstanceParameter;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.IOException;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class DiversePayloadTest extends FodBaseTest {

    String translateArg;
    ApplicationDTO webApplicationDTO;
    String[] expectedLogs;
    String[] unexpectedLogs;
    FodCustomTypes.JobStatus expectedJobStatus;
    String fileToUpload;
    String languageLevel;
    int scanId;
    @TestInstanceParameter("Payload")
    String payload;
    @TestInstanceParameter("Argument")
    String argument;

    @Factory(dataProvider = "diversePayloadTestArguments", dataProviderClass = FodUiTestDataProviders.class)
    public DiversePayloadTest(String translateArg, String fileToUpload,
                              String languageLevel, String[] expectedLogs,
                              String[] unexpectedLogs, FodCustomTypes.JobStatus expectedJobStatus) {

        this.payload = fileToUpload;
        this.argument = translateArg;
        this.translateArg = translateArg;
        this.fileToUpload = fileToUpload;
        this.languageLevel = languageLevel;
        this.expectedLogs = expectedLogs;
        this.unexpectedLogs = unexpectedLogs;
        this.expectedJobStatus = expectedJobStatus;
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to create application/scan on " +
            "tenant site and download logs after scan completion.")
    @Test(groups = {"regression"}, priority = 1)
    public void diversePayloadTest() throws ZipException, IOException {
        webApplicationDTO = ApplicationDTO.createDefaultInstance();
        setTestCaseName("Diverse payload");
        this.webApplicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        BrowserUtil.clearCookiesLogOff();
        createStaticScanTest();
        BrowserUtil.clearCookiesLogOff();
        validateScanLogsTest();
    }

    public void createStaticScanTest() {
        LogInActions.tamUserLogin(defaultTenantDTO.getAssignedUser(),
                FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());

        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(fileToUpload);
        staticScanDTO.setAssessmentType("AUTO-STATIC");
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        staticScanDTO.setLanguageLevel(languageLevel);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setIncludeThirdParty(true);

        StaticScanActions.createStaticScan(staticScanDTO, webApplicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    public void validateScanLogsTest() throws IOException, ZipException {
        AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);

        var scanJobsPage = page(ScanJobsPage.class);
        scanJobsPage.adminTopNavbar.openStatic();
        var scan = scanJobsPage.setAutoRefreshTo(false)
                .getLatestScanByAppDto(webApplicationDTO);

        assertThat(scan).as("Scan not found for: " + webApplicationDTO.getApplicationName()).isNotNull();

        this.scanId = scan.getId();
        var job = scan.getLastJob();

        var jobId = job.getId();
        job.clickClone()
                .setBuildArgsElement(translateArg)
                .setRescanReason(translateArg)
                .pressRescan();
        job = new StaticJobCell(jobId);
        if (!job.getStatus().equals(FodCustomTypes.JobStatus.Success.getTypeValue())) {
            job.clickAction(Actions.JobAction.AbortJob);
        }

        job = scanJobsPage.findScanByScanId(scanId).getLastJob();

        if (expectedJobStatus.equals(FodCustomTypes.JobStatus.Halted)) {
            var haltedJob = job.waitForJobStatus(55, expectedJobStatus);
            assertThat(job.getStatus()).as("Validate that job Halted")
                    .isEqualTo(expectedJobStatus.getTypeValue());

            job.clickAction(Actions.JobAction.AbortJob);
            return;
        }

        if (expectedJobStatus.equals(FodCustomTypes.JobStatus.Aborted)) {
            AllureReportUtil.info("Job aborted as expected. No more actions require...");
            return;
        }

        var logFile = job.waitForJobStatus(expectedJobStatus).downloadLog();

        if (expectedLogs.length != 0)
            StaticScanActions.validateScanLogs(logFile, "/scanagent.log", expectedLogs, true);

        if (unexpectedLogs.length != 0) {
            StaticScanActions.validateScanLogs(logFile, "/scanagent.log", unexpectedLogs, false);
        }
    }
}