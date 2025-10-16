package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("383023")
@Slf4j
public class StaticScanFastTrackTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    StaticScanDTO manualAuditStaticScanDTO, autoAutoStaticScanDTO;

    @MaxRetryCount(3)
    @Description("Admin should be able to set Configuration, create Tenant; TAM should generate scan and complete scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        SiteSettingsActions.setValueInSettings("StaticFastTrackResultDiffTolerancePercent", "10", true);
        BrowserUtil.clearCookiesLogOff();
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);

        manualAuditStaticScanDTO = StaticScanDTO.createDefaultInstance();
        manualAuditStaticScanDTO.setAssessmentType("Static Premium");
        manualAuditStaticScanDTO.setLanguageLevel("10");
        manualAuditStaticScanDTO.setFileToUpload("payloads/fod/10JavaDefects.zip");
        manualAuditStaticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(manualAuditStaticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        //Manual scan; completing process below...
        StaticScanActions.completeStaticScan(applicationDTO, true);
        //Showing that the scan has 0 issues...
        var getTotalIssues = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues()
                .getAllCount();
        assertThat(getTotalIssues)
                .as("For test, payload should have 0 issues")
                .isEqualTo(0);

    }

    @Description("Tam executes scan on same app and release, validate scan is Fast Tracked (Manual audit closed automatically)")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyFastTrackTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        autoAutoStaticScanDTO = StaticScanDTO.createDefaultInstance();
        autoAutoStaticScanDTO.setAssessmentType("Static Premium");
        autoAutoStaticScanDTO.setLanguageLevel("10");
        autoAutoStaticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        autoAutoStaticScanDTO.setFileToUpload("payloads/fod/10JavaDefects.zip");
        //Manual scan should be completed automatically i.e. fast tracked...
        StaticScanActions.createStaticScan(autoAutoStaticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        var getScanStatus = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByAppName(applicationDTO.getApplicationName())
                .getLatestScanByAppDto(applicationDTO)
                .getStatus();
        assertThat(getScanStatus)
                .as("Status should be Completed")
                .isEqualTo("Completed");

    }

    @AfterClass
    @Description("Reset StaticFastTrackResultDiffTolerancePercent back to empty")
    public void resetConfiguration() {
        setupDriver("cleanUp");
        SiteSettingsActions.setValueInSettings("StaticFastTrackResultDiffTolerancePercent", "", true);
        attachTestArtifacts();
    }
}
