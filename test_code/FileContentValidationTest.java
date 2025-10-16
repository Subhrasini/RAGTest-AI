package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.exceptions.FodElementNotCreatedException;
import com.fortify.fod.ui.pages.admin.statics.scan.OverviewPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticScanCell;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.pages.tenant.applications.release.dynamic_scan_setup.DynamicScanSetupPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.reports.yourreports.YourReportsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@Slf4j
public class FileContentValidationTest extends FodBaseTest {

    public String labelMessage = "There are no items to display.";
    public String documentUploadError = "Content of file does not match specified filetype";
    private SupportMethods supportMethods = new SupportMethods();

    public File testFile = new File("payloads/fod/BraveBrowserSetup.fpr");

    List<String> extensionsToValidate = new ArrayList<>() {{
        add("doc");
        add("docx");
        add("ppt");
        add("pptx");
        add("xls");
        add("xlsx");
        add("pdf");
        add("yml");
        add("csv");
        add("correct_docx");
    }};

    List<String> files = new ArrayList<>() {{
        add("doc");
        add("docx");
        add("xls");
        add("xlsx");
        add("pdf");
        add("correct_docx");
    }};

    List<String> adminImportFiles = new ArrayList<>() {{
        add("fpr");
        add("zip");
        add("correct_fpr");
    }};

    List<String> adminReUploadFiles = new ArrayList<>() {{
        add("zip");
        add("war");
        add("ear");
        add("correct_zip");
    }};

    public class SupportMethods {

        public File copyFile(File file, String ext) {
            var copiedFile = new File("payloads/fod/" + UniqueRunTag.generate() + "." + ext);
            try {
                FileUtils.copyFile(file, copiedFile);
            } catch (Exception e) {
                log.error("File was not copied...");
                throw new FodElementNotCreatedException("File was not copied...");
            }
            return copiedFile;
        }

        @Step("Verify admin re-upload resource with file extension: {extension}")
        public void verifyAdminReUploadSource(String extension) {
            File file;
            if (!extension.equals("correct_zip")) {
                file = copyFile(testFile, extension);
            } else {
                file = new File("payloads/fod/static.java.zip");
            }
            var overviewPage = page(OverviewPage.class);
            if (overviewPage.getStatus().equals("Queued"))
                WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual,
                        "Queued", overviewPage::getStatus, Duration.ofMinutes(3), true);

            var reUploadSourcePopup = overviewPage.clickReUploadSourceButton();
            reUploadSourcePopup.clickYesButton();
            reUploadSourcePopup.uploadFile(file.getAbsolutePath());
            reUploadSourcePopup.clickUpload();
            reUploadSourcePopup.waitUntilProgressBarDisappears();
            if (extension.equals("correct_zip")) {
                var isReUploadWindowOpened = reUploadSourcePopup.reUploadSourcePopupIsOpened();
                assertThat(isReUploadWindowOpened)
                        .as("Verify admin reUpload Source window is opened after correct zip file uploaded")
                        .isFalse();
            } else {
                var reUploadedError = reUploadSourcePopup.getReUploadErrorMessage();
                assertThat(reUploadedError)
                        .as("Verify the error message after uploading files")
                        .isEqualTo(documentUploadError);
                var isReUploadWindowOpened = reUploadSourcePopup.reUploadSourcePopupIsOpened();
                assertThat(isReUploadWindowOpened)
                        .as("Verify admin reUpload Source window is opened after file uploaded")
                        .isTrue();
                refresh();
            }
        }

        @Step("Verify admin import scan file extension: {extension}")
        public void verifyAdminImportScan(String extension) {
            File file;
            if (!extension.equals("correct_fpr")) {
                file = copyFile(testFile, extension);
            } else {
                file = new File("payloads/fod/10JavaDefects_ORIGINAL.fpr");
            }
            var overviewPage = page(OverviewPage.class);
            var importStaticScanPopup = overviewPage.pressImportScanButton();
            importStaticScanPopup.uploadFile(file.getAbsolutePath());
            importStaticScanPopup.clickUpload();
            if (extension.equals("correct_fpr")) {
                importStaticScanPopup.clickProceedWithImport();
                var uploadMessage = importStaticScanPopup.getModalMessage();
                assertThat(uploadMessage)
                        .as("Verify the successful message after uploading correct fpr file")
                        .isEqualTo("Scan results successfully imported.");
            } else {
                importStaticScanPopup.waitUntilProgressBarDisappears();
                var uploadedError = importStaticScanPopup.getErrorBlock();
                assertThat(uploadedError)
                        .as("Verify the error message after uploading fpr file")
                        .isEqualTo(documentUploadError);
                var isModalWindowOpened = importStaticScanPopup.staticImportScanPopupIsOpened();
                assertThat(isModalWindowOpened)
                        .as("Verify admin import scan Popup window is opened after fpr file uploaded")
                        .isTrue();
                refresh();
            }
        }

        @Step("Verify import report with file extension: {extension}")
        public void verifyImportReport(String extension) {
            File file;
            if (!extension.equals("correct_docx")) {
                file = copyFile(testFile, extension);
            } else {
                file = new File("payloads/fod/correct_doc.docx");
            }
            var importCustomReportPopup = page(YourReportsPage.class).pressImportReportBtn();
            importCustomReportPopup.uploadFile(file.getAbsolutePath());
            importCustomReportPopup.setReportName("newReport" + UniqueRunTag.generate());
            importCustomReportPopup.pressUpload();
            importCustomReportPopup.waitUntilProgressBarDisappears();
            if (extension.equals("correct_docx")) {
                var docxUploaded = importCustomReportPopup.importCustomReportPopupIsOpened();
                assertThat(docxUploaded)
                        .as("Verify import custom Popup window is closed after correct docx file uploaded")
                        .isFalse();
            } else {
                var errorMessage = importCustomReportPopup.getImportErrorBlock();
                assertThat(errorMessage)
                        .as("Verify the error message after uploading the file")
                        .isEqualTo(documentUploadError);
                refresh();
            }
        }

        @Step("Verify dynamic scan with file extension: {extension}")
        public void verifyDynamicScanSetup(String extension) {
            File extensionsToValidate;
            if (!extension.equals("correct_docx")) {
                extensionsToValidate = copyFile(testFile, extension);
            } else {
                extensionsToValidate = new File("payloads/fod/correct_doc.docx");
            }
            var dynamicScanPage = page(DynamicScanSetupPage.class);
            var additionalDetailsPanel = dynamicScanPage.getAdditionalDetailsPanel().expand()
                    .uploadAdditionalDocumentation(extensionsToValidate.getAbsolutePath());
            dynamicScanPage.waitUntilProgressBarFillUp();
            if (extension.equals("correct_docx")) {
                if (dynamicScanPage.isProgressBarVisible()) {
                    log.info("progress bar is appearing after uploading correct docx file");
                } else {
                    log.info("progress bar is not appearing after uploading correct docx file");
                }
                var docxUploadError = dynamicScanPage.isDocumentUploadErrorExists();
                assertThat(docxUploadError)
                        .as("Verify the error message doesn't appear after uploading correct docx file")
                        .isFalse();
                additionalDetailsPanel.getTable().getTableElement().scrollIntoView(true);
                assertThat(additionalDetailsPanel.getTable()
                        .waitForRecordsCount(1, 1).getColumnHeaders())
                        .as("Verify the table headers")
                        .contains("Name", "Created");
                assertThat(additionalDetailsPanel.getTable().getAllDataRows().texts().toString())
                        .as("Verify the uploading file name")
                        .contains("correct_doc.docx");
            } else {
                var uploadErrors = dynamicScanPage.getDocumentUploadError();
                assertThat(uploadErrors)
                        .as("Verify the error message after uploading files")
                        .isEqualTo(documentUploadError);
                var labelMessages = dynamicScanPage.getLabelMessage();
                assertThat(labelMessages)
                        .as("Verify the existence of label message under dynamic scan setup page")
                        .isEqualTo(labelMessage);
            }
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify file content by uploading different type of files to additional documentations field under dynamic setup page")
    @Test(groups = {"regression"})
    public void dynamicScanSetupFileUploadTest() {
        ApplicationActions.createApplication(defaultTenantDTO, true);
        var applicationsPage = page(YourApplicationsPage.class);
        applicationsPage.openTab("Dynamic");
        for (var extensions : extensionsToValidate) {
            supportMethods.verifyDynamicScanSetup(extensions);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("490021")
    @Description("Verify file content by uploading different type of files to container scan")
    @Test(groups = {"regression"}, enabled = false)
    public void startContainerScanFileUploadTest() {
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(FodConfig.OVERRIDE_DEFAULT_TENANT_NAME).openTabOptions().setContainerScan(true);
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        var yourReleasesPage = new TenantTopNavbar().openApplications().openYourReleases();
        var containerScanSetupPage = yourReleasesPage
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartContainerScan();
        File file = new SupportMethods().copyFile(testFile, "tar");
        var containerScanPopup = containerScanSetupPage
                .pressStartScanBtn()
                .uploadFile(file.getAbsolutePath())
                .pressNextButton()
                .pressStartButton()
                .waitUntilProgressBarDisappears();
        var tarUploadedError = containerScanPopup.getErrorMessages();
        assertThat(tarUploadedError)
                .as("Verify the error message after uploading tar file")
                .isEqualTo(documentUploadError);
        refresh();
        var containerScanPopups = containerScanSetupPage
                .pressStartScanBtn()
                .uploadFile("payloads/fod/registry_image.tar")
                .pressNextButton()
                .pressStartButton()
                .waitUntilProgressBarDisappears();
        var isModalWindowOpened = containerScanPopups.containerScanPopupIsOpened();
        assertThat(isModalWindowOpened)
                .as("Verify Container Scan Popup window is closed after correct tar file uploaded")
                .isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify file content by uploading different type of files to import custom reports field")
    @Test(groups = {"regression"})
    public void validateImportReportFileUploadTest() {
        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openReports();

        for (var extensions : files) {
            supportMethods.verifyImportReport(extensions);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify file content by uploading different type of files to import scan field")
    @Test(groups = {"regression"})
    public void validateImportScansFileUploadTest() {
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        var applicationsPage = page(YourApplicationsPage.class);
        applicationsPage.openTab("Scans");
        var releaseScansPage = page(ReleaseScansPage.class);
        File file = new SupportMethods().copyFile(testFile, "fpr");
        releaseScansPage
                .pressImportByScanType(FodCustomTypes.ScanType.Static)
                .uploadFile(file.getAbsolutePath())
                .pressImportButton()
                .waitUntilProgressBarDisappear();
        var fprUploadedError = releaseScansPage.getImportPopupErrorText();
        assertThat(fprUploadedError)
                .as("Verify the error message after uploading fpr file")
                .isEqualTo(documentUploadError);
        var modalWindowOpened = releaseScansPage.importScanPopupIsOpened();
        assertThat(modalWindowOpened)
                .as("Verify import scan Popup window is opened after fpr file uploaded")
                .isTrue();
        refresh();
        releaseScansPage
                .pressImportByScanType(FodCustomTypes.ScanType.Static)
                .uploadFile("payloads/fod/10JavaDefects_ORIGINAL.fpr")
                .pressImportButton()
                .waitUntilProgressBarDisappear();
        var isModalWindowOpened = releaseScansPage.importScanPopupIsOpened();
        assertThat(isModalWindowOpened)
                .as("Verify import custom Popup window is closed after correct fpr file uploaded")
                .isFalse();
        refresh();
        var assessmentType = releaseScansPage.getLastScanAssessmentType();
        assertThat(assessmentType)
                .as("Verify assessment type")
                .isEqualTo("SCA (Imported)");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify file content by uploading different type of files to admin import scan field")
    @Test(groups = {"regression"})
    public void validateAdminImportScansFileUploadTest() {
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        var scanDTO = StaticScanDTO.createDefaultInstance();
        scanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(scanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn().adminTopNavbar.openStatic();
        var jobsPage = page(ScanJobsPage.class);
        jobsPage.findScanByAppName(applicationDTO.getApplicationName()).openDetails();

        for (var extensions : adminImportFiles) {
            supportMethods.verifyAdminImportScan(extensions);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify file content by uploading different type of files to admin reUpload source field")
    @Test(groups = {"regression"})
    public void validateAdminReUploadSourceTest() {
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        StaticScanActions.createStaticScan(StaticScanDTO.createDefaultInstance(), applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn().adminTopNavbar.openStatic();
        var jobsPage = page(ScanJobsPage.class);
        jobsPage.findScanByAppName(applicationDTO.getApplicationName()).openDetails();

        for (var extensions : adminReUploadFiles) {
            supportMethods.verifyAdminReUploadSource(extensions);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan ID when manually importing static FPRs")
    @Test(groups = {"regression"})
    public void validateScanIDForImportedStaticFPRs() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        var scanDTO = StaticScanDTO.createDefaultInstance();
        scanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(scanDTO,
                applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        var staticPage = LogInActions.adminLogIn().adminTopNavbar
                .openStatic();

        Supplier<StaticScanCell> scan = () ->
                staticPage.findScanByAppName(applicationDTO.getApplicationName());
        var detailsPage = WaitUtil
                .waitFor(WaitUtil.Operator.DoesNotEqual, null, scan, Duration.ofMinutes(3), true)
                .openDetails();
        WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual,
                "Queued", detailsPage::getStatus, Duration.ofMinutes(3), true);
        var importStaticScanPopup = detailsPage.pressImportScanButton();

        importStaticScanPopup.uploadFile("payloads/fod/static.java-RLE48.fpr");
        importStaticScanPopup.clickUpload();
        importStaticScanPopup.waitUntilProgressBarDisappears();
        var importErrorMessage = importStaticScanPopup.getImportErrorMessage();
        assertThat(importErrorMessage)
                .as("Verify the import error message after uploading fpr file")
                .isEqualTo("The FPR file name does not contain the Scan ID.  " +
                        "Please confirm the proper FPR is selected in order to " +
                        "proceed with the import.");
    }
}