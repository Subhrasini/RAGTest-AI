package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("725014")
public class StaticScanFastTrackCopyStateTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO, secondStaticScanDTO;
    ReleaseDTO copyReleaseDTO;

    @BeforeClass
    public void setFeatureFlag() {
        setupDriver("setFeatureFlag");
        SiteSettingsActions.setFeatureFlag("CopyStateTask_IncludeScanJobLog=true", true);
        SiteSettingsActions
                .setValueInSettings(
                        "StaticFastTrackResultDiffTolerancePercent", "10", false);
        attachTestArtifacts();
    }

    @MaxRetryCount(3)
    @Description("Admin should be able to set Configuration, create Tenant; TAM should generate scan and complete scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAssessmentType("Static Premium");
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setFileToUpload("payloads/fod/static.java.zip");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        //Manual scan; completing process below...
        StaticScanActions.completeStaticScan(applicationDTO, true);

        copyReleaseDTO = ReleaseDTO.createDefaultInstance();
        copyReleaseDTO.setCopyState(true);
        copyReleaseDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        copyReleaseDTO.setCopyFromReleaseName(applicationDTO.getReleaseName());

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openApplications();
        ReleaseActions.createRelease(applicationDTO, copyReleaseDTO);

    }

    @Description("TAM should be able to use Scan Central payload for scan and scan should complete")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void checkFastTrackTest() {
        secondStaticScanDTO = StaticScanDTO.createDefaultInstance();
        secondStaticScanDTO.setAssessmentType("Static Premium");
        secondStaticScanDTO.setLanguageLevel("10");
        secondStaticScanDTO.setFileToUpload("payloads/fod/static.java.zip");
        secondStaticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases();
        StaticScanActions.createStaticScan(secondStaticScanDTO, applicationDTO, copyReleaseDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        var getJobCell = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .getLatestScanByAppDto(applicationDTO)
                .getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded);
        assertThat(getJobCell.getStatus())
                .as("Job Status should be ImportSucceeded")
                .contains("Import Succeeded");

    }

    @AfterClass
    @Description("Reset StaticFastTrackResultDiffTolerancePercent back to empty")
    public void resetConfiguration() {
        setupDriver("cleanUp");
        SiteSettingsActions.setValueInSettings("StaticFastTrackResultDiffTolerancePercent", "", true);
        attachTestArtifacts();
    }
}