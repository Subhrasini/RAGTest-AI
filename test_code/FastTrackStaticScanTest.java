package com.fortify.fod.ui.test.regression;

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

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class FastTrackStaticScanTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public ReleaseDTO firstRelease;
    public StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();

    public String settingName = "StaticFastTrackResultDiffTolerancePercent";
    public String settingValue = "10";
    public String summaryName = "Total FOD Issues";
    public String summaryValue = "0";
    public String fileName = "payloads/fod/10JavaDefects.zip";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Set StaticFastTrackResultDiffTolerancePercent flag in admin SiteSettingsPage")
    @Test(groups = {"hf", "regression"})
    public void setFastTrackFlagEnabledTest() {
        SiteSettingsActions.setValueInSettings(settingName, settingValue, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should be able to create static scan")
    @Test(dependsOnMethods = {"setFastTrackFlagEnabledTest"}, groups = {"hf", "regression"})
    public void createStaticScanTest() {
        firstRelease = ReleaseDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        ReleaseActions.createRelease(applicationDTO, firstRelease);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                firstRelease, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the scan has 0 vulns and Publish the results")
    @Test(dependsOnMethods = {"createStaticScanTest"}, groups = {"hf", "regression"})
    public void completeStaticScanTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();
        var scanJobsPage = page(ScanJobsPage.class);
        var staticJobCell = scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), true).getLastJob();
        staticJobCell.waitForJobStatus(FodCustomTypes.JobStatus.Success);
        assertThat(staticJobCell.getStatus())
                .as("Validate scan job row should change success")
                .isEqualTo(FodCustomTypes.JobStatus.Success.getTypeValue());
        assertThat(staticJobCell.openDetails().getSummaryValueByName(summaryName))
                .as("Verify Total FOD Issues(Vulns) should be 0").isEqualTo(summaryValue);
        StaticScanActions.completeStaticScan(applicationDTO, false);
        assertThat(scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), false).getStatus())
                .as("Validate scan should be completed")
                .isEqualTo(FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a second static scan using the same attached Java Payload")
    @Test(dependsOnMethods = {"completeStaticScanTest"}, groups = {"hf", "regression"})
    public void createSecondStaticScanTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                firstRelease, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("515019")
    @Description("Validate second static scan should be fast tracked - it should get an automated audit ")
    @Test(dependsOnMethods = {"createSecondStaticScanTest"}, groups = {"hf", "regression"})
    public void verifyFastrackScanTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();
        var staticScanCell = page(ScanJobsPage.class).findScanByAppName(applicationDTO.getApplicationName(), true);
        var staticJobCell = staticScanCell.getLastJob();
        staticJobCell.waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded);
        assertThat(staticJobCell.getStatus())
                .as("Validate scan job row status should change to ImportSucceeded")
                .isEqualTo(FodCustomTypes.JobStatus.ImportSucceeded.getTypeValue());
        assertThat(staticScanCell.getStatus())
                .as("Validate scan should be completed")
                .isEqualTo(FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }
}
