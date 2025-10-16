package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;


@Owner("sbehera3@opentext.com")
@Slf4j
@FodBacklogItem("492008")
public class CopiedFromReleaseIdTest extends FodBaseTest {

    public String fileName = "payloads/fod/dynamic.zero.fpr";
    DynamicScanDTO dynamicScanDTO;
    ApplicationDTO applicationDTO;
    ReleaseDTO secondRelease, thirdRelease;
    String releaseId;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create an Application in Tenant,create releases with copy release state and run scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        secondRelease = ReleaseDTO.createDefaultInstance();
        thirdRelease = ReleaseDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);

        AllureReportUtil.info("Create first scan on first release");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create second scan on first release");
        LogInActions.tamUserLogin(defaultTenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        releaseId = DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false).getReleaseId();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create second release copied from first release and run a scan");
        secondRelease.setCopyState(true);
        secondRelease.setCopyFromReleaseName(applicationDTO.getReleaseName());
        thirdRelease.setCopyState(true);
        thirdRelease.setCopyFromReleaseName(applicationDTO.getReleaseName());
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .createNewRelease(secondRelease)
                .pressStartDynamicScan();
        DynamicScanActions.createDynamicScan(dynamicScanDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(secondRelease.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create third scan on first release");
        LogInActions.tamUserLogin(defaultTenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create third release copied from first release and run a scan");
        LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .createNewRelease(thirdRelease)
                .pressStartDynamicScan();
        DynamicScanActions.createDynamicScan(dynamicScanDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
        DynamicScanActions.importDynamicScanAdmin(thirdRelease.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate scan id and release id of the copied releases")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})

    public void validateCopiedReleaseTest() {

        var scanSummary_1 = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getAllScans(false).get(1).pressScanSummary();
        var scanID = scanSummary_1.getValueByName("Scan Id");
        scanSummary_1.pressClose();
        var scanSummaryRelease_2 = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), secondRelease.getReleaseName())
                .openScans().getAllScans(false).get(0).pressScanSummary();

        assertThat(scanID)
                .as("Validate Previous value of the ScanId of second release is the same as " +
                        "the current for the 2nd scan in the first release")
                .isEqualTo(scanSummaryRelease_2.getPreviousValueByName("Scan Id"));

        assertThat(releaseId)
                .as("Validate Previous value of the Copied From Release Id of second release is the same as " +
                        "the current for the 2nd scan in the first release")
                .isEqualTo(scanSummaryRelease_2.getPreviousValueByName("Copied From Release Id"));

        scanSummaryRelease_2.pressClose();

        var scanSummary_2 = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getAllScans(false).get(0).pressScanSummary();
        var scanID_2 = scanSummary_2.getValueByName("Scan Id");
        scanSummary_2.pressClose();

        var scanSummaryRelease_3 = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), thirdRelease.getReleaseName())
                .openScans().getAllScans(false).get(0).pressScanSummary();
        assertThat(scanID_2)
                .as("Validate Previous value of the ScanId of third release is the same as "
                        + "the current for the 3rd scan in the first release")
                .isEqualTo(scanSummaryRelease_3.getPreviousValueByName("Scan Id"));

        assertThat(releaseId)
                .as("Validate Previous value of the Copied From Release Id of third release is the same " +
                        "as the current for the 3rd scan in the first release")
                .isEqualTo(scanSummaryRelease_3.getPreviousValueByName("Copied From Release Id"));

        scanSummaryRelease_2.pressClose();
    }
}
