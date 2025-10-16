package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
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
@FodBacklogItem("417021")
public class VerifySuppressIssuesAdminPortalTest extends FodBaseTest {
    String dynamicScanAssessmentType = "Dynamic Website Assessment";
    String mobileScanAssessmentType = "Mobile Assessment";
    ApplicationDTO webAppDTO, mobileAppDTO;
    DynamicScanDTO initialDynamicScanDTO, secondDynamicScanDTO, thirdDynamicScanDTO;
    MobileScanDTO initialMobileScanDTO, secondMobileScanDTO, thirdMobileScanDTO;

    String appendToAddress;

    @MaxRetryCount(3)
    @Description("TAM should create web and mobile apps and dynamic and mobile scans and add issues and set filters")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        appendToAddress = "trace" + UniqueRunTag.generate();
        webAppDTO = ApplicationDTO.createDefaultInstance();
        mobileAppDTO = ApplicationDTO.createDefaultMobileInstance();

        initialDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        initialMobileScanDTO = MobileScanDTO.createDefaultScanInstance();

        ApplicationActions.createApplication(webAppDTO, defaultTenantDTO, true);
        ApplicationActions.createApplication(mobileAppDTO, defaultTenantDTO, false);

        initialDynamicScanDTO.setAssessmentType(dynamicScanAssessmentType);
        DynamicScanActions.createDynamicScan(initialDynamicScanDTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);

        initialMobileScanDTO.setAssessmentType(mobileScanAssessmentType);
        initialMobileScanDTO.setFrameworkType("iOS");
        initialMobileScanDTO.setFileToUpload("payloads/fod/iGoat.ipa");
        MobileScanActions.createMobileScan(initialMobileScanDTO, mobileAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn();
        IssuesActions.addManualIssueExtendedAdmin(FodCustomTypes.ScanType.Dynamic,
                webAppDTO.getApplicationName(),
                "16",
                appendToAddress,
                FodCustomTypes.Severity.Critical,
                false);
        DynamicScanActions.completeDynamicScanAdmin(webAppDTO, false);
        BrowserUtil.clearCookiesLogOff();
        setAuditFilter(webAppDTO.getApplicationName(), webAppDTO.getReleaseName());

        LogInActions.adminLogIn().adminTopNavbar.openMobile();
        IssuesActions.addManualIssueExtendedAdmin
                (FodCustomTypes.ScanType.Mobile,
                        mobileAppDTO.getApplicationName(),
                        "M10",
                        appendToAddress,
                        FodCustomTypes.Severity.Critical,
                        false);
        MobileScanActions.completeMobileScan(mobileAppDTO, false);
        setAuditFilter(mobileAppDTO.getApplicationName(), mobileAppDTO.getReleaseName());

    }

    @MaxRetryCount(3)
    @Description("TAM should create dynamic scan and publish it and verify issues suppressed vs not suppressed")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            dataProvider = "scanType", dataProviderClass = VerifySuppressIssuesAdminPortalTest.class)
    public void checkSuppressedIssueAdminPortalTest(FodCustomTypes.ScanType scanType) {
        setTestCaseName("Check suppressed issue test on Admin Portal with scan type: " + scanType.getTypeValue());
        LogInActions.tamUserLogin(defaultTenantDTO);
        if (scanType == FodCustomTypes.ScanType.Dynamic) {
            secondDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
            secondDynamicScanDTO.setAssessmentType(dynamicScanAssessmentType);
            DynamicScanActions.createDynamicScan(secondDynamicScanDTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
            BrowserUtil.clearCookiesLogOff();
            var publishScan = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                    .openDetailsFor(webAppDTO.getApplicationName());

            publishScan.pressImportScanButton()
                    .uploadFile("payloads/fod/DynSuper1.fpr")
                    .uploadWithProceedImport(true);
            publishScan.pressPublishScanButton()
                    .pressOk();

            publishScan.openIssues().setShowSuppressed(false);
            publishScan.openIssues().setShowFixed(false);

            var expectedValue = publishScan.openIssues().getAllCount();

            publishScan.openIssues().getShowFixedSuppressedDropdown().setShowSuppressed(true);
            var expectedValue2 = publishScan.openIssues().getAllCount();
            assertThat(expectedValue2)
                    .as("Value should be: " + expectedValue2)
                    .isEqualTo(expectedValue + 1);
        } else {
            secondMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
            secondMobileScanDTO.setAssessmentType(mobileScanAssessmentType);
            MobileScanActions.createMobileScan(secondMobileScanDTO, mobileAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
            BrowserUtil.clearCookiesLogOff();
            var publishScan = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                    .openDetailsFor(mobileAppDTO.getApplicationName());

            publishScan.pressImportScanButton()
                    .uploadFile("payloads/fod/mobile_IOS.fpr")
                    .uploadWithProceedImport(false);
            publishScan.pressPublishScanButton()
                    .pressOk();

            publishScan.waitForMobiusSuccess();
            publishScan.openIssues().setShowSuppressed(false);
            publishScan.openIssues().setShowFixed(false);

            var expectedValue = publishScan.openIssues().getAllCount();
            publishScan.openIssues().getShowFixedSuppressedDropdown().setShowSuppressed(true);

            var expectedValue2 = publishScan.openIssues().getAllCount();
            assertThat(expectedValue2)
                    .as("Value should be: " + expectedValue2)
                    .isEqualTo(expectedValue + 1);
        }

    }

    @MaxRetryCount(3)
    @Description("Admin should validate created issue is suppressed relative to webApp and its dynamic scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"checkSuppressedIssueAdminPortalTest"},
            dataProvider = "scanType", dataProviderClass = VerifySuppressIssuesAdminPortalTest.class)
    public void checkSuppressedIssueTenantPortalTest(FodCustomTypes.ScanType scanType) {
        setTestCaseName("Check suppressed issue test on Tenant Portal with scan type: " + scanType.getTypeValue());
        if (scanType == FodCustomTypes.ScanType.Dynamic) {
            checkTenantSuppressed(webAppDTO.getApplicationName());
        } else {
            checkTenantSuppressed(mobileAppDTO.getApplicationName());
        }
    }

    @MaxRetryCount(3)
    @Description("Admin should see that issue has suppressed icon after TAM executes another scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"checkSuppressedIssueTenantPortalTest"},
            dataProvider = "scanType", dataProviderClass = VerifySuppressIssuesAdminPortalTest.class)
    public void checkSuppressedIconAdminPortalTest(FodCustomTypes.ScanType scanType) {
        setTestCaseName("Check suppressed issue icon test on Admin Portal with scan type: " + scanType.getTypeValue());
        LogInActions.tamUserLogin(defaultTenantDTO);
        if (scanType == FodCustomTypes.ScanType.Dynamic) {
            thirdDynamicScanDTO = DynamicScanDTO.createDefaultInstance();
            thirdDynamicScanDTO.setAssessmentType(dynamicScanAssessmentType);
            DynamicScanActions.createDynamicScan(thirdDynamicScanDTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
            BrowserUtil.clearCookiesLogOff();

            var publishScan = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                    .openDetailsFor(webAppDTO.getApplicationName());

            var getAdminIssue = publishScan
                    .openIssues()
                    .findWithSearchBox(appendToAddress)
                    .getAllIssues().get(0);
            assertThat(getAdminIssue.isSuppressed)
                    .as("Icon indicator should be visible")
                    .isTrue();

            publishScan.pressImportScanButton()
                    .uploadFile("payloads/fod/DynSuper1.fpr")
                    .uploadWithProceedImport(true);
            publishScan.pressPublishScanButton()
                    .pressOk();
        } else {
            thirdMobileScanDTO = MobileScanDTO.createDefaultScanInstance();
            thirdMobileScanDTO.setAssessmentType(mobileScanAssessmentType);
            MobileScanActions.createMobileScan(thirdMobileScanDTO, mobileAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
            BrowserUtil.clearCookiesLogOff();

            var publishScan = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                    .openDetailsFor(mobileAppDTO.getApplicationName());

            var getAdminIssue = publishScan
                    .openIssues()
                    .findWithSearchBox(appendToAddress)
                    .getCriticalIssues().get(0);
            assertThat(getAdminIssue.isSuppressed)
                    .as("Icon indicator should be visible")
                    .isTrue();

            publishScan.pressImportScanButton()
                    .uploadFile("payloads/fod/mobile_IOS.fpr")
                    .uploadWithProceedImport(false);
            publishScan.pressPublishScanButton()
                    .pressOk();
        }
    }

    public void setAuditFilter(String applicationName, String releaseName) {
        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationName, releaseName)
                .openIssues()
                .findWithSearchBox(appendToAddress)
                .getAllIssues().get(0)
                .pressAddAuditFilter()
                .setSuppress()
                .pressCreateFilter();
        new ModalDialog()
                .pressClose(true);
        BrowserUtil.clearCookiesLogOff();
    }

    public void checkTenantSuppressed(String applicationName) {
        var suppressedIssue = LogInActions.tamUserLogin(defaultTenantDTO).openYourApplications()
                .openDetailsFor(applicationName)
                .openIssues()
                .showSuppressed()
                .findWithSearchBox(appendToAddress)
                .getAllIssues().get(0);
        assertThat(suppressedIssue.isSuppressed)
                .as("Icon indicator should be visible")
                .isTrue();
        BrowserUtil.clearCookiesLogOff();
    }

    @DataProvider(name = "scanType", parallel = true)
    public static Object[][] scanType() {
        return new Object[][]{
                {FodCustomTypes.ScanType.Mobile},
                {FodCustomTypes.ScanType.Dynamic}
        };
    }

}
