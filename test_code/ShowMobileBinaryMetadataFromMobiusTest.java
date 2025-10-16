package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.exceptions.FodElementNotFoundException;
import com.fortify.fod.ui.pages.common.tenant.popups.ScanSummaryPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ShowMobileBinaryMetadataFromMobiusTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO = MobileScanDTO.createDefaultScanInstance();

    String platform = "Android";
    String name = "Flickrj-android-sample-android";
    String identifier = "com.gmail.yuyang226.flickrj.sample.android";
    String version = "1.0";
    String fileSize = "147969";
    String minimumOsRequirements = "8 (Froyo 2.2.x)";
    String[] pagesToValidate = new String[]{"Release Scans", "Application Scans", "Your Scans"};

    HashMap<String, String> scanSummaryParams = new HashMap<>() {{
        put("Time Zone", "");
        put("Multi-factor Authentication", "");
        put("Pre-Assessment Call", "");
        put("Additional Notes", "");
        put("Framework Type", platform);
        put("Version", version);
        put("identifier", identifier);
    }};

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create mobile application and mobile scan for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        mobileScanDTO.setStartInFuture(true);

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        MobileScanActions.publishMobileScanWithoutImportFpr(applicationDTO, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate binary metadata from mobius on app information tab")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void mobileBinaryMetadataAppInfoTabTest() {
        var releaseDetailsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openAppInfoTab();

        HashMap<String, String[]> valuesMap = new HashMap<>() {{

            /*
            valuesMap:
            1) String - Name
            2) String[] :  1. Expected value. 2. Actual value
            */

            put("Platform", new String[]{platform, releaseDetailsPage.getAppInfoValueByName("Platform")});
            put("Name", new String[]{name, releaseDetailsPage.getAppInfoValueByName("Name")});
            put("Identifier", new String[]{identifier, releaseDetailsPage.getAppInfoValueByName("Identifier")});
            put("Version", new String[]{version, releaseDetailsPage.getAppInfoValueByName("Version")});
            put("File Size (bytes)", new String[]
                    {fileSize, releaseDetailsPage.getAppInfoValueByName("File Size (bytes)")});
            put("Minimum OS Requirements", new String[]
                    {minimumOsRequirements, releaseDetailsPage.getAppInfoValueByName("Minimum OS Requirements")});
        }};

        for (var entry : valuesMap.entrySet()) {
            var name = entry.getKey();
            var expectedValue = entry.getValue()[0];
            var actualValue = entry.getValue()[1];

            assertThat(actualValue)
                    .as(name + " should be equal: " + expectedValue)
                    .isEqualTo(expectedValue);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate binary metadata from mobius on scan summary popup")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void mobileBinaryMetadataScanSummaryPopupTest() {
        var topNavBar = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar;

        for (var page : pagesToValidate) {
            ScanSummaryPopup scanSummaryPopup;
            switch (page) {
                case "Release Scans":
                    scanSummaryPopup = topNavBar.openApplications()
                            .openYourReleases()
                            .openDetailsForRelease(applicationDTO)
                            .openScans().getScanByType(FodCustomTypes.ScanType.Mobile).pressScanSummary();
                    break;
                case "Application Scans":
                    scanSummaryPopup = topNavBar.openApplications()
                            .openDetailsFor(applicationDTO.getApplicationName()).openScans()
                            .getScanByAppDto(applicationDTO)
                            .pressScanSummary();
                    break;
                case "Your Scans":
                    scanSummaryPopup = topNavBar.openApplications().openYourScans()
                            .getScanByAppDto(applicationDTO)
                            .pressScanSummary();
                    break;
                default:
                    throw new FodElementNotFoundException(page + " not found");
            }

            for (var value : scanSummaryParams.entrySet()) {
                if (value.getValue().isEmpty())
                    assertThat(scanSummaryPopup.nameIsPresent(value.getKey())).isFalse();
                else
                    assertThat(value.getValue()).isEqualTo(scanSummaryPopup.getValueByName(value.getKey()));
            }
            scanSummaryPopup.pressClose();
        }
    }
}
