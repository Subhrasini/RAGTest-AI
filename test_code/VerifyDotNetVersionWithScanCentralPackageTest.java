package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("465001")
public class VerifyDotNetVersionWithScanCentralPackageTest extends FodBaseTest {

    StaticScanDTO staticDotNetScan;
    ApplicationDTO webAppDTO;
    String scanCentralWarning = "ScanCentral is recommended for selected" +
            " Technology Stack and Language Level for comprehensive scan results";

    @MaxRetryCount(1)
    @Description("TAM should be able to use Scan Central payload for scan and scan should complete")
    @Test(groups = {"regression"},
            dataProvider = "dotNetLevel", dataProviderClass = VerifyDotNetVersionWithScanCentralPackageTest.class)
    public void testTheDotNetPackageTest(FodCustomTypes.LanguageLevelDotNet dotNetLevel) {
        setTestCaseName("Check dotNet level test with Scan Central Package: " + dotNetLevel.getTypeValue());

        webAppDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webAppDTO, defaultTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        staticDotNetScan = StaticScanDTO.createDefaultInstance();
        var forStaticScanSetupCheck = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(webAppDTO.getApplicationName(), webAppDTO.getReleaseName())
                .pressStartStaticScan();

        staticDotNetScan.setAssessmentType("Static Premium");
        staticDotNetScan.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        String runTimeDotNet = null;
        if (dotNetLevel == FodCustomTypes.LanguageLevelDotNet.DotNet50) {
            runTimeDotNet = "5.0";
        } else {
            runTimeDotNet = "6.0";
        }
        staticDotNetScan.setLanguageLevel(runTimeDotNet);
        staticDotNetScan.setAuditPreference(FodCustomTypes.AuditPreference.Automated);
        staticDotNetScan.setFileToUpload("payloads/fod/CurrencyConverter.zip");

        assertThat(forStaticScanSetupCheck.getScanCentralWarningMessage())
                .as("There should be a warning message regarding Scan Central")
                .contains(scanCentralWarning);
        StaticScanActions.createStaticScan(staticDotNetScan, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .getLatestScanByAppDto(webAppDTO)
                .getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.ImportSucceeded, FodCustomTypes.JobStatus.Success);
    }

    @DataProvider(name = "dotNetLevel")
    public static Object[][] dotNetLevel() {
        return new Object[][]{
                {FodCustomTypes.LanguageLevelDotNet.DotNet50},
                {FodCustomTypes.LanguageLevelDotNet.DotNet60}
        };
    }
}