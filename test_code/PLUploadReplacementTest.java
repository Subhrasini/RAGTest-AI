package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.dynamic.scan_configuration.ScanConfigurationPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@FodBacklogItem("297007")
@Slf4j
public class PLUploadReplacementTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO application, mobileApplication;
    StaticScanDTO binaryStaticScanDTO;
    MobileScanDTO mobileScanDTO;

    private void initTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission",
                        "Allow scanning with no entitlements",
                        //"Scan Third Party Libraries",
                        "Enable Microservices option for web applications",
                        "Enable Source Download",
                        "Allow Binary Scanning"
                });

        application = ApplicationDTO.createDefaultInstance();

        mobileApplication = ApplicationDTO.createDefaultMobileInstance();

        binaryStaticScanDTO = StaticScanDTO.createDefaultInstance();
        binaryStaticScanDTO.setScanBinary(true);
        binaryStaticScanDTO.setFileToUpload("payloads/fod/WarFileNamedJar.zip");

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
    }

    @MaxRetryCount(3)
    @Description("Preparing test data")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        initTestData();

        AllureReportUtil.info("Create tenant, applications and scans");

        TenantActions.createTenant(tenantDTO);

        ApplicationActions.createApplication(application, tenantDTO, true);
        ApplicationActions.createApplication(mobileApplication, tenantDTO, false);

        StaticScanActions.importScanTenant(application, "payloads/fod/static.java.fpr");

        MobileScanActions.importScanTenant(mobileApplication, "payloads/fod/static.java.fpr");
    }

    @MaxRetryCount(3)
    @Description("Upload binary file for Static scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void uploadBinaryFileForStaticScanTest() {
        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(binaryStaticScanDTO, application);
    }

    @MaxRetryCount(3)
    @Description("Upload Additional documentation/information file on Dynamic scan setup page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void uploadAdditionalDocumentationOnDynamicScanSetupPageTest() {
        var additionalDetailsPanel = LogInActions
                .tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(application)
                .openDynamicScanSetup()
                .getAdditionalDetailsPanel().expand()
                .uploadAdditionalDocumentation("payloads/fod/_test.txt");

        assertThat(
                additionalDetailsPanel.getTable()
                        .getCellByTextAndIndex("_test.txt", 0)
                        .getText())
                .as("File should be uploaded successfully and displayed in the table")
                .contains("_test.txt");
    }

    @MaxRetryCount(3)
    @Description("Upload binary file for Mobile scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void uploadBinaryFileForMobileScanTest() {
        MobileScanActions.createMobileScan(tenantDTO, mobileScanDTO, mobileApplication, null, true,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Description("Upload mobile file")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void uploadMobileFileTest() {
        var mobileDocumentsTab = LogInActions.adminLogIn()
                .adminTopNavbar
                .openMobile()
                .openDetailsFor(mobileApplication.getApplicationName())
                .openFiles()
                .openDocumentsTab();
        mobileDocumentsTab
                .pressUploadButton()
                .setFileName("Test file")
                .uploadFile("payloads/fod/_test.txt")
                .pressUpload()
                .pressClose();

        assertThat(
                mobileDocumentsTab
                        .getTable()
                        .getCellByTextAndIndex("_test",
                                mobileDocumentsTab.getTable().getColumnIndex("File Name"))
                        .getText())
                .as("File should be uploaded successfully and displayed in the table")
                .contains("_test");
    }

    @MaxRetryCount(3)
    @Description("Upload static file")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "uploadBinaryFileForStaticScanTest"})
    public void uploadStaticFileTest() {
        var staticDocumentsTab = LogInActions.adminLogIn()
                .adminTopNavbar
                .openStatic()
                .findScanByAppName(application.getApplicationName(), true)
                .openDetails()
                .openFiles()
                .openDocumentsTab();
        staticDocumentsTab.pressUploadButton()
                .setFileName("Test file")
                .uploadFile("payloads/fod/_test.txt")
                .pressUpload()
                .pressClose();

        assertThat(
                staticDocumentsTab
                        .getTable()
                        .getCellByTextAndIndex("_test",
                                staticDocumentsTab.getTable().getColumnIndex("File Name"))
                        .getText())
                .as("File should be uploaded successfully and displayed in the table")
                .contains("_test");
    }

    @MaxRetryCount(3)
    @Description("Import scan configuration file")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void importScanConfigurationFileTest() {
        LogInActions.adminLogIn()
                .adminTopNavbar
                .openDynamic()
                .openScanConfiguration()
                .importScanConfiguration("payloads/fod/TestScanConfig.json", true);

        var scanConfigTable = new Table(new ScanConfigurationPage().scanConfigTable);
        assertThat(
                scanConfigTable
                        .getCellByTextAndIndex("Test Config", scanConfigTable.getColumnIndex("NAME"))
                        .getText())
                .as("File should be uploaded successfully and displayed in the table")
                .contains("Test Config");
    }

    @MaxRetryCount(3)
    @Description("Upload License file")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void uploadLicenseFileTest() {
        var licenseTab = LogInActions.adminLogIn()
                .adminTopNavbar
                .openTenants()
                .openTenantByName(tenantDTO.getTenantName())
                .openTabLicenses();
        licenseTab.uploadSecurityAssistantLicense("payloads/fod/new.fortify.license");

        assertThat(
                licenseTab.getValidationMessage())
                .as("SAL license should be uploaded and saved successfully.")
                .contains("Saved Successfully.");

        licenseTab.uploadSCALicense("payloads/fod/new.fortify.license");

        assertThat(
                licenseTab.getValidationMessage())
                .as("SCA license should be uploaded and saved successfully.")
                .contains("Saved Successfully.");
    }

    @MaxRetryCount(3)
    @Description("Import config file")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void importConfigFileTest() throws FileNotFoundException {
        var threadsPage = LogInActions.adminLogIn()
                .adminTopNavbar
                .openConfiguration()
                .openTaskService()
                .openThreads();

        var threadConfigFilePath = threadsPage.exportThreadConfig().getPath();

        threadsPage
                .importThreadConfig(threadConfigFilePath)
                .pressSave();

        assertThat(threadsPage
                .infoblocksSave
                .isDisplayed())
                .as("Message for successful import should be displayed")
                .isTrue();
    }

    @MaxRetryCount(3)
    @Description("Vulnerabilities file import")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void vulnerabilitiesFileImport() {
        var vulnerabilityDefinitionsPage = LogInActions.adminLogIn()
                .adminTopNavbar
                .openConfiguration()
                .openVulnerabilityDefinitions();
        var customVulnsFile = vulnerabilityDefinitionsPage.downloadCustomVulnerabilities();
        assertThat(customVulnsFile)
                .as("Name should be customvulns")
                .hasName("customvulns.csv");
        vulnerabilityDefinitionsPage.importCustomVulnerability(customVulnsFile.getAbsolutePath());

        assertThat(vulnerabilityDefinitionsPage
                .successImportInfoblock
                .isDisplayed())
                .as("Message for successful import should be displayed")
                .isTrue();

        vulnerabilityDefinitionsPage.importVulnerabilityValidation("payloads/fod/VulnerabilityLibrary.csv");

        assertThat(vulnerabilityDefinitionsPage
                .successImportInfoblock
                .isDisplayed())
                .as("Message for successful import should be displayed")
                .isTrue();
    }
}