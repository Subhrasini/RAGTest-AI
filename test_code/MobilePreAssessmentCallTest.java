package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class MobilePreAssessmentCallTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;
    String scanName = "Mobile+ Assessment";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Enable Mobile+ Assessment type scan in tenant if not enabled")
    @Test(groups = {"hf", "regression"})
    public void validateMobilePlusAssessmentTypeTest() {
        var assessmentTypesPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantCode())
                .openAssessmentTypes();
        assessmentTypesPage.selectAllowSingleScan(scanName);
        assessmentTypesPage.selectAllowSubscription(scanName);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("401035")
    @Description("Enabling the Pre-assessment call in Mobile scans allows the users start the scans without the 72 hours window")
    @Test(dependsOnMethods = {"validateMobilePlusAssessmentTypeTest"}, groups = {"hf", "regression"})
    public void validateErrorMessageTest() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        var startMobileScanPopup = ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true)
                .pressStartMobileScan()
                .setAssessmentType(scanName)
                .setEntitlement(mobileScanDTO.getEntitlement())
                .setTimeZone(mobileScanDTO.getTimezone())
                .setFrameworkType(mobileScanDTO.getFrameworkType())
                .setAuditPreference(mobileScanDTO.getAuditPreference().getTypeValue())
                .setAuthenticationRequired(mobileScanDTO.isAuthenticationRequired())
                .setPreAssessmentConfCall(true)
                .pressStartScanBtn();
        startMobileScanPopup.pressNextButton().uploadFile(mobileScanDTO.getFileToUpload());
        startMobileScanPopup.waitForStartButtonEnabled();
        startMobileScanPopup.startScanButton.click();
        assertThat(startMobileScanPopup.getErrorMsg())
                .as("Verify error message in the modal dialog")
                .isEqualTo("Pre-scan calls require at least 72 hour advance notice. Please adjust the scheduled start time or remove the request for a pre-scan call.");

    }
}
