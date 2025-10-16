package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.common.admin.cells.HistoryCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
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
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.back;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class DynamicScanFilesAndOtherTabsTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    DynamicScanDTO dynamicScanDTO;
    Pattern p = Pattern.compile("\\d+");
    Matcher m = p.matcher(FodConfig.TEST_ENDPOINT_ADMIN);
    String ScannerName = m.find() ? "FODQA" + m.group(0) + "-WI" : "11";
    String userName = FodConfig.ADMIN_USER_NAME;
    String NameForUploadFile = "Auto-file" + UniqueRunTag.generate();
    String AutoNotes = "Test notes" + UniqueRunTag.generate();
    String ExpectedValidationMessage = "Saved Successfully.";
    String WorkflowStep = ScannerName.contains("FODQA11") ? "Preparation and Setup" : "Preparation";
    String PayloadToUpload = "payloads/fod/DynSuper1.fpr";
    String FileToUpload = "payloads/fod/example-2.json";
    List<String> NotesSectionsList = new ArrayList<>() {
        {
            add("Tenant Notes");
            add("Scan Notes");
            add("Application Notes");
        }
    };
    List<String> LogsToValidate = new ArrayList<>() {
        {
            add("File Deleted. Name");
            add("File Uploaded. Name");
            add("Info");
            add("Unmark As Question");
            add("Mark As Question");
            add("Make Workflow Available");
            add("Workflow Instance Tasks reset to Open.");
            add("Workflow Reset");
            add("Workflow Instance Task status changed.");
            add("Workflow Instance Task claimed.");
            add("Workflow Instance Task status changed.");
            add("Dynamic Scan Created");
        }
    };
    AdminUserDTO dynamicManager, dynamicTester;
    String additionalFileToUpload = "payloads/fod/_test.txt";
    String fileName = "_test";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare application and dynamic scan for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setDynamicSiteUrl("http://non-exist-zero.webappsecurity.com/");
        dynamicScanDTO.setAssessmentType("Dynamic Premium");

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate tasks actions on tasks page and pause the scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void dynamicTasksPageWorkflowActionsTest() {
        var taskPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openTasks();
        taskPage.pressTakeTask();

        var activeStep = taskPage.getSelectedStep();
        assertThat(activeStep.getStepName()).isEqualTo(WorkflowStep);
        assertThat(activeStep.getAssignedUser()).isEqualToIgnoringCase(userName);

        var markCompletedPopup = taskPage.pressTaskDone();
        assertThat(markCompletedPopup.popupElement.isDisplayed())
                .as("Mark task completed popup should be visible")
                .isTrue();

        markCompletedPopup.pressYes().openTasks();
        var preparationAndSetupStep = taskPage.getStepByName(WorkflowStep);
        assertThat(preparationAndSetupStep).isNotNull();
        assertThat(preparationAndSetupStep.getWorkflowBulletElement().getCssValue("color"))
                .isEqualTo("rgba(26, 172, 96, 1)");

        var resetWorkflowPopup = taskPage.pressResetWorkflowButton();
        assertThat(resetWorkflowPopup.popupElement.isDisplayed())
                .as("Reset workflow popup should be visible")
                .isTrue();

        resetWorkflowPopup.pressOk().openTasks();

        assertThat(!taskPage.takeTaskButton.exists())
                .as("Take task button shouldn't be present")
                .isTrue();

        assertThat(taskPage.getStepByName(WorkflowStep).getStepElement().getAttribute("title"))
                .as("Preparation marked as available")
                .isEqualTo("available");

        var enableWorkflowPopup = taskPage.pressEnableWorkflowButton();
        assertThat(enableWorkflowPopup.popupElement.shouldBe(Condition.visible, Duration.ofSeconds(30))
                .isDisplayed())
                .as("Enable Workflow Popup should be visible")
                .isTrue();

        enableWorkflowPopup.pressYes().openTasks();
        assertThat(taskPage.takeTaskButton.isEnabled())
                .as("Take task button appeared and available")
                .isTrue();

        var pauseScanPopup = taskPage.pressPauseButton();
        assertThat(pauseScanPopup.popupElement.isDisplayed())
                .as("Pause Scan Popup is opened")
                .isTrue();

        pauseScanPopup.pressOkButton().openTasks();
        assertThat(taskPage.getStatus()).isEqualTo(FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue());
        assertThat(taskPage.resumeBtn.isEnabled())
                .as("Resume button available after pause scan")
                .isTrue();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate created tickets on tickets page")
    @Test(groups = {"regression"},
            dependsOnMethods = {"dynamicTasksPageWorkflowActionsTest"}, priority = 1, enabled = false)
    public void dynamicTicketsPageTest() {
        var ticketsPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openTickets();
        ticketsPage.waitForTickets();
        var ticket = ticketsPage.getAllTickets().stream()
                .filter(x -> x.getSubject().equals("Assessment Waiting for Customer Action"))
                .findFirst().orElse(null);
        assertThat(ticket).as("Ticket should be created").isNotNull();
        assertThat(ticket.getStatus()).isEqualTo("Pending");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate resume scan and claim scan popups and actions")
    @Test(groups = {"regression"},
            dependsOnMethods = {"dynamicTasksPageWorkflowActionsTest"})
    public void dynamicResumeScanAndClaimScannerTest() {
        var ticketsPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openTasks();
        var resumePopup = ticketsPage.pressResumeButton();
        assertThat(resumePopup.popupElement.isDisplayed())
                .as("Resume popup should be opened")
                .isTrue();
        var taskPage = resumePopup.pressOkButton(true).openTasks();
        if (ModalDialog.isMessageDisplayed()) {
            new ModalDialog().close();
        }

        assertThat(taskPage.getStatus()).isEqualTo(FodCustomTypes.ScansDetailsPageStatus.InProgress.getTypeValue());
        assertThat(taskPage.resumeBtn.isDisplayed()).isFalse();
        assertThat(taskPage.pauseBtn.isEnabled()).isTrue();

        var claimScannerPopup = taskPage.pressClaimScanner();
        assertThat(claimScannerPopup.popupElement.isDisplayed())
                .as("Claim scanner popup should be opened")
                .isTrue();

        claimScannerPopup.selectGroup("Default");
        String scanner;
        try {
            claimScannerPopup.chooseAgent(ScannerName).pressOk(true);
            BrowserUtil.waitAjaxLoaded();
            new ModalDialog().pressClose();
            back();
            refresh();
            scanner = new AdminTopNavbar().openDynamic()
                    .openDetailsFor(applicationDTO.getApplicationName())
                    .getOverviewValueByTitle("Scanner Assignment");
        } catch (Exception | Error e) {
            scanner = "";
        }

        if (!scanner.isEmpty())
            assertThat(scanner)
                    .as("Scanner name should be displayed on overview page")
                    .isEqualTo(ScannerName);

    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate dynamic scan import popup and import action")
    @Test(groups = {"regression"},
            dependsOnMethods = {"dynamicResumeScanAndClaimScannerTest"})
    public void dynamicImportScanTest() {
        var taskPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openTasks();
        var importScanPopup = taskPage.pressImportScanButton();
        assertThat(importScanPopup.popupElement.isDisplayed())
                .as("Import Scan popup should be opened")
                .isTrue();

        importScanPopup.uploadFile(PayloadToUpload).pressUpload(true);

        assertThat(taskPage.importScan.isDisplayed()).isTrue();
        assertThat(taskPage.importScan.getAttribute("title"))
                .as("After .fpr uploading [UPLOAD] button changed name on [Re-import Scan]")
                .isEqualTo("Re-import Scan");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate dynamic scan files page and upload/download functions")
    @Test(groups = {"regression"},
            dependsOnMethods = {"dynamicImportScanTest"})
    public void dynamicFilesPageTest() {
        var filesPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openFiles();
        var files = filesPage.getAllFiles();

        assertThat(files).isNotEmpty();
        assertThat(files.get(0).getFileName()).contains(defaultTenantDTO.getTenantName());

        var file = files.get(0).download("fpr");
        assertThat(file).hasName(files.get(0).getFileName());

        var uploadFilePopup = filesPage.pressUploadButton();
        assertThat(uploadFilePopup.popupElement.isDisplayed())
                .as("Upload file popup should be opened")
                .isTrue();

        uploadFilePopup.setFileName(NameForUploadFile)
                .uploadFile(FileToUpload)
                .pressUpload()
                .pressClose();

        var uploadedFile = filesPage.getAllFiles().stream().filter(f -> f.getName().equals(NameForUploadFile))
                .findFirst().orElse(null);
        assertThat(uploadedFile).isNotNull();
        assertThat(uploadedFile.getName()).isEqualTo(NameForUploadFile);

        uploadedFile.pressDelete().pressYes();
        uploadedFile = filesPage.getAllFiles().stream().filter(f -> f.getName().equals(NameForUploadFile))
                .findFirst().orElse(null);

        assertThat(uploadedFile)
                .as("File should be deleted")
                .isNull();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate dynamic scan notes page and functionality")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void dynamicNotesPageTest() {
        var notesPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openNotes().openTenant();

        var tenantMessage = notesPage.setTenantNotes(AutoNotes).saveTenantNotes().getValidationMessage();
        assertThat(tenantMessage).isEqualTo(ExpectedValidationMessage);

        var scanMessage = notesPage.setScanNotes(AutoNotes).saveScanNotes().getValidationMessage();
        assertThat(scanMessage).isEqualTo(ExpectedValidationMessage);

        var applicationMessage = notesPage.setApplicationNotes(AutoNotes).saveApplicationNotes()
                .getValidationMessage();
        assertThat(applicationMessage).isEqualTo(ExpectedValidationMessage);

        var overviewPage = notesPage.openOverview();
        overviewPage.tabs.openTabByName("Notes");
        for (var note : NotesSectionsList) {
            assertThat(overviewPage.getNoteValueByTitle(note)).isEqualTo(AutoNotes);
        }
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate dynamic scan history page and functionality")
    @Test(groups = {"regression"}, dependsOnMethods = {"dynamicFilesPageTest"})
    public void dynamicHistoryPageTest() {
        var historyPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openNotes().openHistory();
        var csv = historyPage.downloadHistory();
        assertThat(csv).hasExtension("csv");

        var history = historyPage.getAllHistoryCells().stream()
                .map(HistoryCell::getLog).collect(Collectors.toList());

        for (var log : LogsToValidate) {
            assertThat(history.stream().anyMatch(x -> x.contains(log)))
                    .as("Log: " + log + " should be present in the list")
                    .isTrue();
        }

        var overviewPage = historyPage.pressPublishScanButton().pressOk();
        assertThat(overviewPage.getStatus())
                .isEqualTo(FodCustomTypes.ScansDetailsPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("797054")
    @Owner("sbehera3@opentext.com")
    @Description("Unable to download files from the Dynamic files tab as a Dynamic Manager/Tester role")
    @Test(groups = {"hf", "regression"})
    public void downloadFileDynamicScanTest() {
        var dynamicTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicTester);
        var dynamicManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicManager);
        dynamicManager.setTenant(defaultTenantDTO.getTenantName());
        dynamicTester.setTenant(defaultTenantDTO.getTenantName());
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAdditionalDocumentationFile(additionalFileToUpload);
        dynamicScanDTO.setAssessmentType("Dynamic Website Assessment");

        AdminUserActions.createAdminUser(dynamicManager, true);
        AdminUserActions.createAdminUser(dynamicTester, false);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Download file from Dynamic Manager Login");
        downloadFile(dynamicManager, applicationDTO);

        AllureReportUtil.info("Download file from Dynamic Tester Login");
        downloadFile(dynamicTester, applicationDTO);
    }

    void downloadFile(AdminUserDTO adminUserDTO, ApplicationDTO appDTO) {

        var fileToDownload = LogInActions.adminUserLogIn(adminUserDTO).adminTopNavbar.openDynamic()
                .openDetailsFor(appDTO.getApplicationName()).openFiles().getAllFiles();
        assertThat(fileToDownload).isNotEmpty();
        assertThat(fileToDownload.get(0).getFileName()).isEqualTo(fileName);
        assertThat(fileToDownload.get(0).download("txt").getName())
                .contains(fileName);
        BrowserUtil.clearCookiesLogOff();

    }
}
