package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@FodBacklogItem("622014")
@Slf4j
public class WebServicesToAPITest extends FodBaseTest {

    ApplicationDTO appForMobileScan, appForDynamicScan;
    MobileScanDTO mobileScanDTO;
    String assessmentName = "Dynamic+ API Assessment";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Enable Dynamic+ API Assessment scan for the tenant")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantCode())
                .openAssessmentTypes()
                .getAssessmentByName(assessmentName)
                .setAllowSubscription(true)
                .setAllowSingleScan(true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify web service rename to API in dynamic scan setup and summary page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validationInDynamicScanTest() {

        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        appForDynamicScan = ApplicationDTO.createDefaultInstance();
        var dynamicScanSetupPage = ApplicationActions.createApplication(appForDynamicScan, defaultTenantDTO, true).pressStartDynamicScan();

        dynamicScanSetupPage.setAssessmentType(assessmentName);

        if (!dynamicScanDTO.isRemediation()) {
            dynamicScanSetupPage.setDynamicSiteUrl(dynamicScanDTO.getDynamicSiteUrl())
                    .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing());
        }
        var apiPanel =
                dynamicScanSetupPage.setEntitlement(dynamicScanDTO.getEntitlement())
                .setTimeZone(dynamicScanDTO.getTimezone())
                .getApiPanel().expand()
                .enableApi(true);

        assertThat(dynamicScanSetupPage.getAssessmentTypeDropdown().getOptionsTextValues())
                .as("In Assessment types drop-down, Dynamic+ Web service section rename to Dynamic+ API Assessment")
                .contains(assessmentName);
        assertThat(apiPanel.getApiCheckbox().isDisplayed())
                .as("Web service check box rename to APIs")
                .isTrue();
        assertThat(apiPanel.getApiTypeDropdown().isDisplayed())
                .as("Web service type check box rename to API Type")
                .isTrue();

        apiPanel.setApiType(FodCustomTypes.DynamicScanApiType.REST);
        var dynamicScanPopup = dynamicScanSetupPage.pressStartScanBtn()
                .pressNextButtonUntilStartAvailable();

        assertThat(dynamicScanPopup.getSummarySectionHeader("APIs").isDisplayed())
                .as("Start Dynamic Scan window Summary page Web Service section rename to API")
                .isTrue();
        dynamicScanPopup.pressStartButton();
        dynamicScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify web service rename to API in mobile scan setup and summary page")
    @Test(groups = {"regression"}, dependsOnMethods = {"validationInDynamicScanTest"})
    public void validationInMobileScanTest() {
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        appForMobileScan = ApplicationDTO.createDefaultMobileInstance();
        var mobileScanSetupPage = ApplicationActions.createApplication(appForMobileScan, defaultTenantDTO, true).pressStartMobileScan();

        mobileScanSetupPage.setAssessmentType(mobileScanDTO.getAssessmentType())
                .setEntitlement(mobileScanDTO.getEntitlement())
                .setTimeZone(mobileScanDTO.getTimezone())
                .setFrameworkType(mobileScanDTO.getFrameworkType())
                .setAuditPreference(mobileScanDTO.getAuditPreference().getTypeValue())
                .setAuthenticationRequired(mobileScanDTO.isAuthenticationRequired());

        assertThat(mobileScanSetupPage.getSectionHeader("APIs").isDisplayed())
                .as("Mobile Scan Setup page under Web service section rename to API")
                .isTrue();
        assertThat(mobileScanSetupPage.getAccessToWebServicesCheckbox().isDisplayed())
                .as("Access to Web Service checkbox rename to Access to API")
                .isTrue();

        var startMobileScanPopup = mobileScanSetupPage.pressStartScanBtn()
                .pressNextButton()
                .uploadFile(mobileScanDTO.getFileToUpload());
        WaitUtil.waitForTrue(
                () -> startMobileScanPopup.startScanButton.isEnabled(),
                Duration.ofSeconds(10),
                () -> startMobileScanPopup.pressNextButton());

        assertThat(startMobileScanPopup.getSummaryField("Access to APIs").isDisplayed())
                .as("Start Mobile Scan window Summary page Web Service section rename to API")
                .isTrue();

        startMobileScanPopup.pressStartButton();
        mobileScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.Scheduled);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify web service rename to API in admin login")
    @Test(groups = {"regression"}, dependsOnMethods = {"validationInMobileScanTest"})
    public void validationInAdminTest() {

        var assessmentTypeCell = LogInActions.adminLogIn().adminTopNavbar.openAssessmentTypes().getAssessmentTypeByName(assessmentName);
        assertThat(assessmentTypeCell.getCategoryName())
                .as("Assessment Categories rename Dynamic Plus APIs ")
                .isEqualTo("Dynamic Plus APIs");
        var editPopUp = assessmentTypeCell.pressEdit().setAssessmentCategory("Dynamic APIs");
        assertThat(editPopUp.getAssessmentCategoryOptions())
                .as("In Assessment category drop-down, Dynamic Web service rename to Dynamic APIs")
                .contains("Dynamic APIs");
        editPopUp.pressCancelButton();
        var dynamicScanOverviewPage = new AdminTopNavbar().openDynamic().openDetailsFor(appForDynamicScan.getApplicationName());
        assertThat(dynamicScanOverviewPage.getOverviewValueByTitle("Assessment Type"))
                .as("Assessment type name in Dynamic ScanOverview page")
                .isEqualTo("Dynamic+ API Assessment");
        assertThat(dynamicScanOverviewPage.getOverviewValueByTitle("Assessment Category"))
                .as("Assessment category name in Dynamic ScanOverview page")
                .isEqualTo("Dynamic Plus APIs");

        var mobileScanOverviewPage = new AdminTopNavbar().openMobile().openDetailsFor(appForMobileScan.getApplicationName());
        assertThat(mobileScanOverviewPage.getOverviewValueByTitle("Access to APIs"))
                .as("Assessment type name in Mobile ScanOverview page")
                .isEqualTo("Yes");

    }
}
