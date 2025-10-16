package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.admin.statics.scan.FilesPage;
import com.fortify.fod.ui.pages.admin.statics.scan.HistoryPage;
import com.fortify.fod.ui.pages.admin.statics.scan.OverviewPage;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.HistoryCell;
import com.fortify.fod.ui.pages.common.common.pages.IssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("1621039")
public class StaticScanAviator extends FodBaseTest {
    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;
    String aviatorCheckboxConfirmationMessage = "SAST Aviator service provides auditing and assistance with " +
            "remediation. The remediation assistance takes the form of comments added to the Fortify results, " +
            "including code snippets. This service will consume 1 additional assessment unit.\n" +
            "\n" +
            "Disclaimer: Aviator leverages Generative Artificial Intelligence(GAI) with a Large Language Model(LLM), " +
            "to generate content and deliver product functionality to you.You agree to comply with all terms and " +
            "conditions of the End User License Agreement(EULA) available at " +
            "https://aws.amazon.com/marketplace/pp/prodview-3b3i27cz6kzw2 in the Usage Information Section.\n" +
            "OpenText does not guarantee the accuracy and/or rights to use of AI-generated content. " +
            "It is your responsibility to apply your judgement and consider multiple sources before making any decisions.\n" +
            "Website: https://aws.amazon.com/marketplace/pp/prodview-3b3i27cz6kzw2";

    String langSupport = "SAST Aviator is available for all technology stacks.";

    private boolean runAfterMethod = true;
    String releaseName;
    String applicationName;
    int scanId;

    @FodBacklogItem("1621039")
    @Owner("pgaikwad@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify SAST Aviator service in Static Scan Setup Page")
    @Test(groups = {"regression"})
    public void fortifyAviatorScanSetupPageValidation() {
        webApp = ApplicationDTO.createDefaultInstance();
        ModalDialog modal = new ModalDialog();
        var scanSetupPage = ApplicationActions.createApplication(webApp, defaultTenantDTO, true).pressStartStaticScan();
        SoftAssertions softAssert = new SoftAssertions();
        softAssert.assertThat(scanSetupPage.getAviatorCheckbox().isEnabled())
                .as("Validate SAST Aviator checkbox is enabled")
                .isTrue();

        softAssert.assertThat(scanSetupPage.getAviatorCSHLink().getAttribute("href"))
                .as("Validate SAST Aviator csh link")
                .contains("/Docs/en/index.htm#cshid=1040");

        scanSetupPage.getAviatorCheckbox().click();

        softAssert.assertThat(modal.getMessage())
                .as("Validate SAST Aviator confirmation dialogue box message")
                .contains(aviatorCheckboxConfirmationMessage);

        modal.clickButtonByText("Yes");

        softAssert.assertThat(scanSetupPage.getAviatorAWSLink().isDisplayed())
                .as("Validate SAST Aviator AWS Link is displayed")
                .isTrue();

        scanSetupPage.chooseAssessmentType("Static Basic");

        softAssert.assertThat(scanSetupPage.getAviatorCheckbox().checked())
                .as("Validate SAST Aviator checkbox is Checked For Static Assessment Type")
                .isTrue();

        scanSetupPage.chooseAuditPreference(FodCustomTypes.AuditPreference.Manual);

        softAssert.assertThat(scanSetupPage.getAviatorCheckbox().checked())
                .as("Validate SAST Aviator checkbox is unchecked for Static Assessment Type")
                .isFalse();

        scanSetupPage.getAviatorCheckbox().click();

        modal.clickButtonByText("Yes");

        softAssert.assertThat(scanSetupPage.auditPreferenceDropdown.getSelectedOption().getText())
                .as("Validate Automated is selected after selecting SAST Aviator Checkbox")
                .isEqualTo("Automated");

        softAssert.assertThat(scanSetupPage.getLangSupportText().getText())
                .as("Validate SAST Aviator csh link")
                .contains(langSupport);

        softAssert.assertAll();

        BrowserUtil.clearCookiesLogOff();

        var configuration = LogInActions.adminLogIn().adminTopNavbar.
                openConfiguration();

        var supportedTechStacks = configuration.getSettingValueByName("FortifyAviatorSupportedTechStacks");

        assertThat(supportedTechStacks)
                .as("Validate SAST Aviator supported tech stacks")
                .contains("-1");

        configuration.openSettingByName("FortifyAviatorDisabledTenantIds")
                .setValue(new FodSQLUtil().getTenantIdByName(defaultTenantDTO.getTenantName())).save();

        BrowserUtil.clearCookiesLogOff();

        var scanSetup = LogInActions.tamUserLogin(defaultTenantDTO).openDetailsFor(webApp.getApplicationName())
                .getReleaseByName(webApp.getReleaseName()).openReleaseDetails().pressStartStaticScan();

        WaitUtil.waitFor(WaitUtil.Operator.Equals, false, () -> scanSetup.getAviatorCheckbox().isDisplayed()
                , Duration.ofMinutes(5), true);

        assertThat(scanSetup.getAviatorCheckbox().isDisplayed())
                .as("Validate SAST Aviator checkbox is disabled")
                .isFalse();
        runAfterMethod = true;
    }

    @FodBacklogItem("1684011")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the scan summary page details with SAST Aviator enabled")
    @Test(groups = {"Regression"})
    public void fortifyAviatorScanSummaryPageDetailsValidation() {

        var scanSetup = LogInActions.tamUserLogin(defaultTenantDTO).openDetailsFor(webApp.getApplicationName())
                .getReleaseByName(webApp.getReleaseName()).openReleaseDetails().pressStartStaticScan();

        WaitUtil.waitFor(WaitUtil.Operator.Equals, true, () -> scanSetup.getAviatorCheckbox().isDisplayed()
                , Duration.ofMinutes(5), true);

        AllureReportUtil.info("Verify the scan summary page details when a payload doesn't have any issues");
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setIncludeFortifyAviator(true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/10JavaDefects.zip");

        releaseName = webApp.getReleaseName();
        applicationName = webApp.getApplicationName();

        var scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);

        var scanSummaryPage = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).pressScanSummary();
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Requested"))
                .as("Validate the value of SAST Aviator field")
                .isEqualTo("Yes");

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Status"))
                .as("Validate the value of SAST Aviator Service Status field")
                .isEqualTo("No new vulnerabilities were found so the SAST Aviator service did not run.");

        scanSummaryPage.pressClose();

        AllureReportUtil.info("Verify the scan summary page details with the python payload");
        staticScanDTO.setFileToUpload("payloads/fod/PythonDjango-5.zip");

        scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        scanSummaryPage = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).pressScanSummary();

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Requested"))
                .as("Validate the value of SAST Aviator field")
                .isEqualTo("Yes");

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Status"))
                .as("Validate the value of SAST Aviator Service Status field")
                .isEqualTo("SAST Aviator service has run successfully");

        scanSummaryPage.pressClose();

        AllureReportUtil.info("Verify the scan summary page details with the successful java payload");
        staticScanDTO.setFileToUpload("payloads/fod/WebGoat5.0.zip");

        scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);

        scanId = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).getScanId();

        scanSummaryPage = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).pressScanSummary();

        softAssertions.assertThat(scanSummaryPage.nameIsPresent("SAST Aviator Service Requested"))
                .as("Validate the new row named as SAST Aviator is added or not")
                .isTrue();
        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Requested"))
                .as("Validate the value of SAST Aviator field")
                .isEqualTo("Yes");

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Status"))
                .as("Validate the value of SAST Aviator Service Status field")
                .isEqualTo("SAST Aviator service has run successfully");

        scanSummaryPage.pressClose();
        BrowserUtil.clearCookiesLogOff();

        SiteSettingsActions.setValueInSettings("FortifyAviatorDisabledTenantIds",
                new FodSQLUtil().getTenantIdByName(defaultTenantDTO.getTenantName()),
                true, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify the SAST Aviator checkbox for same application when we disabled the tenant for SAST aviator");
        var setupScan = LogInActions.tamUserLogin(defaultTenantDTO).openDetailsFor(webApp.getApplicationName())
                .getReleaseByName(webApp.getReleaseName()).openReleaseDetails().pressStartStaticScan();

        assertThat(setupScan.getAviatorCheckbox().isDisplayed())
                .as("Validate SAST Aviator checkbox is displayed")
                .isTrue();

        softAssertions.assertAll();
    }

    @FodBacklogItem("1687012")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the Static Scan details with SAST Aviator enabled")
    @Test(groups = {"Regression"}, dependsOnMethods = {"fortifyAviatorScanSummaryPageDetailsValidation"})
    public void fortifyAviatorScanDetailsValidation() {

        OverviewPage overviewPage = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByScanId(scanId).openDetails();

        AllureReportUtil.info("Verify the FA notes message on admin history page");
        HistoryPage historyPage = overviewPage.openHistory();
        String actualFANotesMessage = historyPage.getAllHistoryCells().get(1).getNotes();

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(actualFANotesMessage)
                .as("Verify the Notes Message on History page for FA enabled scan")
                .contains("SAST Aviator completed Successfully");

        AllureReportUtil.info("Verify the various types of history messages on issues page");
        String eventDate = historyPage.getAllHistoryCells().get(1).getDate().split(" ")[0];
        String timeStamp = "23:59:59 PM";
        String timestampRegex = "\\d{2}:\\d{2}:\\d{2} (AM|PM)";

        String expectedSystemEventMessage = "OpenText™ Core Application Security " + eventDate + " " + timeStamp + "\n" +
                "Issue found in scan " + scanId + " of release " + releaseName + ".";

        String expectedAuditActionMessage = "Fortify Aviator " + eventDate + " " + timeStamp + "\n" +
                "Changed Auditor Status from 'Pending Review' to ";

        String expectedCommentsMessage1 = "Fortify Aviator " + eventDate + " " + timeStamp + "\n" +
                "Currently not supported by Fortify Aviator.";

        String expectedCommentsMessage2 = "Fortify Aviator " + eventDate + " " + timeStamp + "\n" +
                "Fortify Aviator running in test mode - no real comments.\n" +
                "Fortify Aviator running in test mode - no real code fix.";

        IssuesPage issuesPage = historyPage.openIssues();
        WaitUtil.randomSleep(4, 5, true);

        HistoryCell historyCells = issuesPage.openIssueByIndex(0).openDetails().openHistory();
        List<String> eventsLists = historyCells.getAllEvents();
        int eventsSize = eventsLists.size();

        assertThat(eventsLists.get(0).replaceAll(timestampRegex, timeStamp))
                .as("Verify the system event history message of aviator based scan")
                .isEqualTo(expectedSystemEventMessage);

        if (eventsSize > 2) {
            if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.NotAnIssue.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, timeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.NotAnIssue.getTypeValue() + "'");

            } else if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, timeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue() + "'");

            } else if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.Suspicious.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, timeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.Suspicious.getTypeValue() + "'");
            }
        }

        assertThat(historyCells.getAviatorLogo())
                .as("Verify the aviator logo displayed in audit history message section")
                .isTrue();

        if (eventsLists.get(eventsSize - 1).contains("Currently not supported")) {
            assertThat(eventsLists.get(eventsSize - 1).replaceAll(timestampRegex, timeStamp))
                    .as("Verify the comments history message of aviator based scan")
                    .isEqualTo(expectedCommentsMessage1);
        } else {
            assertThat(eventsLists.get(eventsSize - 1).replaceAll(timestampRegex, timeStamp))
                    .as("Verify the comments history message of aviator based scan")
                    .isEqualTo(expectedCommentsMessage2);
        }

        FilesPage filePage = historyPage.openFiles();
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        filePage.getFileCellByName("Log File").download("zip");
        filePage.getFileCellByName("FPR File").download("fpr");

        runAfterMethod = false;

        softAssertions.assertAll();
    }

    @FodBacklogItem("1685016")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the FA enabled message Validations on issues page in tenant site")
    @Test(groups = {"Regression"}, dependsOnMethods = {"fortifyAviatorScanDetailsValidation"})
    public void fortifyAviatorIssuesPageValidation() {

        ReleaseIssuesPage issuesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases().openDetailsForRelease(applicationName, releaseName).openIssues();

        issuesPage.groupBy("Auditor Status");
        List<String> groupHeaders = issuesPage.getGroupHeaders();

        assertThat(groupHeaders.size())
                .as("Verify the numbers of group headers displayed when filtered by Auditor Status")
                .isEqualTo(2);

        assertThat(groupHeaders.get(0))
                .as("Verify the first group header displayed when filtered by Auditor Status")
                .isEqualTo("Remediation Required");

        assertThat(groupHeaders.get(1))
                .as("Verify the another group header displayed when filtered by Auditor Status")
                .isEqualTo("Suspicious");

        assertThat(groupHeaders.contains("Not an Issue"))
                .as("Verify issues with Auditor status Not an Issue will be suppressed in the portal")
                .isFalse();

        issuesPage.setShowSuppressed(true);
        WaitUtil.randomSleep(4, 5, true);

        assertThat(issuesPage.getGroupHeaders().contains("Not an Issue"))
                .as("Verify issues with Auditor status Not an Issue is visible now")
                .isTrue();

        AllureReportUtil.info("Verify the various types of history messages on issues page");
        HistoryCell historyCells = issuesPage.openIssueByIndex(0).openDetails().openHistory();

        List<String> eventsLists = historyCells.getAllEvents();
        int eventsSize = eventsLists.size();

        String eventTimeStamp = "01/01/1999 23:59:59 PM";
        String timestampRegex = "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2} (AM|PM)";

        String expectedSystemEventMessage = "OpenText™ Core Application Security " + eventTimeStamp + "\n" +
                "Issue found in scan " + scanId + " of release " + releaseName + ".";

        String expectedAuditActionMessage = "Fortify Aviator " + eventTimeStamp + "\n" +
                "Changed Auditor Status from 'Pending Review' to ";

        String expectedCommentsMessage1 = "Fortify Aviator " + eventTimeStamp + "\n" +
                "Currently not supported by Fortify Aviator.";

        String expectedCommentsMessage2 = "Fortify Aviator " + eventTimeStamp + "\n" +
                "Fortify Aviator running in test mode - no real comments.\n" +
                "Fortify Aviator running in test mode - no real code fix.";


        assertThat(eventsLists.get(0).replaceAll(timestampRegex, eventTimeStamp))
                .as("Verify the system event history message of aviator based scan")
                .isEqualTo(expectedSystemEventMessage);

        if (eventsSize > 2) {
            if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.NotAnIssue.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, eventTimeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.NotAnIssue.getTypeValue() + "'");

            } else if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, eventTimeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.RemediationRequired.getTypeValue() + "'");

            } else if (eventsLists.get(1).contains(FodCustomTypes.AuditorStatus.Suspicious.getTypeValue())) {
                assertThat(eventsLists.get(1).replaceAll(timestampRegex, eventTimeStamp))
                        .as("Verify the audit action history message of aviator based scan")
                        .isEqualTo(expectedAuditActionMessage + "'" + FodCustomTypes.AuditorStatus.Suspicious.getTypeValue() + "'");
            }

        }

        assertThat(historyCells.getAviatorLogo())
                .as("Verify the aviator logo displayed in audit history message section")
                .isTrue();

        if (eventsLists.get(eventsSize - 1).contains("Currently not supported")) {
            assertThat(eventsLists.get(eventsSize - 1).replaceAll(timestampRegex, eventTimeStamp))
                    .as("Verify the comments history message of aviator based scan")
                    .isEqualTo(expectedCommentsMessage1);
        } else {
            assertThat(eventsLists.get(eventsSize - 1).replaceAll(timestampRegex, eventTimeStamp))
                    .as("Verify the comments history message of aviator based scan")
                    .isEqualTo(expectedCommentsMessage2);
        }

        var scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);

        AllureReportUtil.info("Verify no duplicate audit action history messages are coming");
        issuesPage = scanSetupPage.openIssues();
        WaitUtil.randomSleep(3, 5, true);

        historyCells = issuesPage.openIssueByIndex(0).openDetails().openHistory();
        List<String> auditContents = historyCells.getAuditContents();

        assertThat(auditContents.size() == 1)
                .as("Verify the single audit action history message should be displayed")
                .isTrue();

        runAfterMethod = false;
    }


    @AfterMethod
    public void tearDown() {

        if (runAfterMethod) {
            SiteSettingsActions.removeValueInSettings("FortifyAviatorDisabledTenantIds",
                    new FodSQLUtil().getTenantIdByName(defaultTenantDTO.getTenantName()),
                    true, true);
        } else {
            System.out.println("Skipping Tear Down");
        }
    }

}

