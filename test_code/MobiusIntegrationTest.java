package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class MobiusIntegrationTest extends FodBaseTest {

    ApplicationDTO mobileApp;
    MobileScanDTO scanDTO = MobileScanDTO.createDefaultScanInstance();

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare mobile application and mobile scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        scanDTO.setFileToUpload("payloads/fod/flickrj.apk");
        scanDTO.setAssessmentType("Mobile Express");
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, true);
        new TenantTopNavbar().openApplications().openYourReleases();
        MobileScanActions.createMobileScan(scanDTO, mobileApp);
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Mobile scan with audit preference 'Manual' shouldn't be published automatically")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateManualMobileScan() {
        MobileScanActions.publishMobileScanWithoutImportFpr(mobileApp, true);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.validateMobileScanCompletedTenant(defaultTenantDTO, mobileApp);
        var scan = new TenantTopNavbar().openApplications().openYourScans().
                getScanByType(mobileApp, FodCustomTypes.ScanType.Mobile);
        assertThat(scan.getLowCount()).as("Low count should be equal 4").isEqualTo(4);
        assertThat(scan.getMediumCount()).as("Medium count should be equal 2").isEqualTo(2);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Mobile scan with audit preference 'Automatically publish' should be published automatically")
    @Test(groups = {"regression"}, dependsOnMethods = {"validateManualMobileScan"})
    public void validateAutoMobileScan() {
        scanDTO.setAuditPreference(FodCustomTypes.AuditPreference.MobileAutomated);
        MobileScanActions.createMobileScan(defaultTenantDTO, scanDTO, mobileApp, null, true,
                FodCustomTypes.SetupScanPageStatus.Completed);
        MobileScanActions.validateMobileScanCompletedTenant(defaultTenantDTO, mobileApp, false);
        var scan = new TenantTopNavbar().openApplications().openYourScans().
                getScanByType(mobileApp, FodCustomTypes.ScanType.Mobile);
        assertThat(scan.getLowCount()).as("Low count should be equal 4").isEqualTo(4);
        assertThat(scan.getMediumCount()).as("Medium count should be equal 2").isEqualTo(2);
    }
}
