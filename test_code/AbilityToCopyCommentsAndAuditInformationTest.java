package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.ScreenshotsCell;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("753011")
@Slf4j
public class AbilityToCopyCommentsAndAuditInformationTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    ReleaseDTO secondRelease;
    String fprFile = "payloads/fod/static.java.fpr";
    String jsonFile = "payloads/fod/21210_51134_cyclonedx.json";
    List<String> issueHeaders = Arrays.asList("Issue Id", "Release", "Primary Location", "Audited", "Attachments",
            "Scan Tool", "");
    String expectedMessage = "The selected audit changes from issue %s will be applied to " +
            "1 selected issues from other releases. Do you wish to continue?";
    String issueName1 = "ResourceLeakExample.java : 60";
    String issueName2 = "ResourceLeakExample.java : 48";
    String issueName3 = "ResourceLeakExample.java : 26";

    TenantDTO tenantDTO;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Creating Application with importing static and open source scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission",
                        "Allow scanning with no entitlements",
                        //"Scan Third Party Libraries",
                        "Enable Microservices option for web applications",
                        "Enable Source Download"
                });

        secondRelease = ReleaseDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        applicationDTO = ApplicationActions.createApplication(tenantDTO);
        ReleaseActions.createReleases(applicationDTO, secondRelease);
        var totalCount = TenantScanActions
                .importScanTenant(applicationDTO, secondRelease, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .getTotalCount();
        assertThat(totalCount)
                .as("Debricked imported scan issues count should be greater then 0")
                .isPositive();
        TenantScanActions
                .importScanTenant(applicationDTO, secondRelease, fprFile, FodCustomTypes.ScanType.Static)
                .getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        totalCount = TenantScanActions
                .importScanTenant(applicationDTO, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .getTotalCount();
        assertThat(totalCount)
                .as("Debricked imported scan issues count should be greater then 0")
                .isPositive();
        TenantScanActions.importScanTenant(applicationDTO, fprFile, FodCustomTypes.ScanType.Static)
                .getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify audit details in release issues page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void releaseIssuesPageTest() {
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        issuesPage.selectReleaseIssueCheckbox(true);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId1, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        assertThat(url())
                .as("The Issue ID now is a hyper link that opens the issue details")
                .contains(issueId1);
        var instanceId1 = page(ReleaseIssuesPage.class).getInstanceId();
        Selenide.switchTo().window(0);
        issuesPage
                .getTable()
                .getCellByTextAndIndex(issueId2, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        var instanceId2 = page(ReleaseIssuesPage.class).getInstanceId();
        assertThat(instanceId1)
                .as("All other Issues belonging to same release " +
                        "or different with matching InstanceId appeared")
                .isEqualTo(instanceId2);
        Selenide.switchTo().window(0);
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("All headers are present in the table")
                .containsExactlyInAnyOrderElementsOf(issueHeaders);
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a new column named Audited")
                .contains("Audited");
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Audited")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("View audit details");
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.getAuditInfoForSelectedIssue())
                .as("Verify that top section to contain the current audit " +
                        "information for the issue selected")
                .contains("Assigned User", "Developer Status", "Auditor Status", "Severity");
        assertThat(viewAuditDetailsPopup.commentSection.isDisplayed())
                .as("Verify that top section to contain latest comment of selected issue.")
                .isTrue();
        assertThat(viewAuditDetailsPopup.history.isDisplayed())
                .as("Verify that bottom section of the " +
                        "pop-up window will show the history of the issue")
                .isTrue();
        assertThat(viewAuditDetailsPopup.noAttachmentSettingMsg.getText())
                .as("If Issue selected to view details do not contain attachments " +
                        "the message will be like: The selected issue does not have any attachments.")
                .contains("The selected issue does not have any attachments.");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Take a user who is inactive in the system, and verify the scenarios for it")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "releaseIssuesPageTest"})
    public void verifyAuditDetailsForInactiveUser() {
        var comment = "comment-" + UniqueRunTag.generate();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        issuesPage.setAssignedUser(tenantDTO.getUserName());
        issuesPage.setComment(comment).pressSubmitChanges();
        new TenantTopNavbar().openAdministration().openUserManagement()
                .openUsers()
                .editUserByName(tenantDTO.getUserName())
                .setInactive(true).pressSaveBtn();
        new TenantTopNavbar().openApplications().openYourApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup1.selectAllBtn.isDisplayed())
                .as("Verify that a global select all added to " +
                        "select all the audits and comments.")
                .isTrue();
        viewAuditDetailsPopup1.selectAllBtn.click();
        assertThat(viewAuditDetailsPopup1.unselectAllBtn.isDisplayed())
                .as("Verify that a global unselect option is available to unselect all.")
                .isTrue();
        assertThat(viewAuditDetailsPopup1.user.getText())
                .as("Verify that when user is inactive in the system the assigned user " +
                        "in the pop up to show the name of the user name (inactive)")
                .contains("Last Name, First Name(inactive)");
        viewAuditDetailsPopup1.cancelBtn.click();
        assertThat(issuesPage.getAssignedUser())
                .as("Verify that assigned User field to be left as --No Change-- when the Issue selected to " +
                        "copy audit information has a user assigned that is no longer active in system.")
                .contains("--No Change--");
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId1, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        new ScreenshotsCell().openScreenshots().addScreenShot();
        Selenide.switchTo().window(0);
        refresh();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("true");
        var viewAuditDetailsPopup2 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup2.attachment.checked())
                .as("Verify that default state of checkbox is to have " +
                        "it selected for the Issues where Attachments are present.")
                .isTrue();
        viewAuditDetailsPopup2.cancelBtn.click();
        new TenantTopNavbar().openAdministration().openUserManagement()
                .openUsers().editUserByName(tenantDTO.getUserName())
                .setInactive(false).pressSaveBtn();
    }


    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Add a tenant level setting Include attachments while copying audits " +
            "to control at the global level to select to deselect the checkbox")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "verifyAuditDetailsForInactiveUser"})
    public void tenantLevelSettingForIncludeAttachmentTest() {
        LogInActions.adminLogIn().adminTopNavbar
                .openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openTabOptions()
                .setIncludeAttachments(true);
        BrowserUtil.clearCookiesLogOff();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        issuesPage.selectReleaseIssueCheckbox(true);
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.attachmentSettingMsg.getText())
                .as("Verify that Tenant setting should override the actual setting")
                .contains("This setting has been configured by the site's administrator.");
        viewAuditDetailsPopup.copyAuditDetailsBtn.click();
        issuesPage.pressSubmitChanges().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName1);
        issuesPage.selectReleaseIssueCheckbox(true);
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup1.attachment.checked())
                .as("attachment is copied and check box is check for 2nd release")
                .isTrue();
        viewAuditDetailsPopup1.cancelBtn.click();
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId2, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        assertThat(new ScreenshotsCell().openScreenshots().getAllScreenshots())
                .as("Verify that the attachments should be copied " +
                        "if the Include attachments checkbox is enabled.").hasSize(1);
        Selenide.switchTo().window(0);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminLogIn().adminTopNavbar
                .openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openTabOptions().setIncludeAttachments(false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that when user clicks on Submit changes all the " +
            "values from audit fields will be applied to selected list of Issues.")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "tenantLevelSettingForIncludeAttachmentTest"})
    public void auditChangesInReleaseIssuesPageTest() {
        AllureReportUtil.info("verify the audit changes in release issues page");
        var comment = "comment-" + UniqueRunTag.generate();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        issuesPage.setComment(comment).pressSubmitChanges().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.getAllEvents().toString())
                .as("Added comment should visible in audit details")
                .contains(comment);
        viewAuditDetailsPopup.cancelBtn.click();
        issuesPage.selectReleaseIssueCheckbox(true);
        assertThat(issuesPage.getTable().getRowsCount())
                .as("Verify that show Issues from other releases only when" +
                        " Include issues from other releases check box is enabled.")
                .isEqualTo(2);
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup1.isCheckboxAvailable())
                .as("Verify that a checkbox added next to each of those Audit " +
                        "and Comment entry for customer to choose and copy over to selected Issues.")
                .isTrue();
        viewAuditDetailsPopup1.clickCheckboxOfAuditOrComment(comment);
        viewAuditDetailsPopup1.copyAuditDetailsBtn.click();
        assertThat(issuesPage.isAuditFieldDisabled("Assigned User"))
                .as("Verify that the audit fields disabled when the " +
                        "copy audit details are clicked.")
                .isTrue();
        assertThat(issuesPage.isSourceIssueRowEnabled(issueId1))
                .as("Verify that the source Issue row is disabled " +
                        "in the grid when copy audit details is clicked.")
                .isTrue();
        issuesPage.submitChangesBtn.click();
        var modalDialog = new ModalDialog();
        assertThat(modalDialog.getMessage())
                .as("Verify the message in confirmation " +
                        "dialog on Submitting the changes")
                .contains(String.format(expectedMessage, issueId1));
        modalDialog.getButtonByName("Yes").click();
        modalDialog.pressClose();
        issuesPage.expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        issuesPage.selectReleaseIssueCheckbox(true);
        var viewAuditDetailsPopup2 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup2.getAllEvents().toString())
                .as("Verify that when user clicks on Submit changes all the values " +
                        "from audit fields will be applied to selected list of Issues.")
                .contains("Changed Severity from '(Default)' to 'High'");
        assertThat(viewAuditDetailsPopup2.latestComment.getText())
                .as("Verify that Prefix the Audits and Comments that are " +
                        "copied from another issue with the word [Copied]")
                .contains("[Copied] " + comment);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that not to include the attachments in the " +
            "audit information copy if the checkbox: INCLUDE ATTACHMENTS is unselected")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "auditChangesInReleaseIssuesPageTest"})
    public void notToIncludeAttachmentTest() {
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId1, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        new ScreenshotsCell().openScreenshots().addScreenShot();
        Selenide.switchTo().window(0);
        refresh();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("true");
        issuesPage.selectReleaseIssueCheckbox(true);
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        viewAuditDetailsPopup.attachment.setValue(false);
        viewAuditDetailsPopup.copyAuditDetailsBtn.click();
        issuesPage.pressSubmitChanges().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName2);
        issuesPage.selectReleaseIssueCheckbox(true);
        issuesPage.clickCloseButtonByIssueId(issueId1);
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("second release issue doesn't contain attachment" +
                        " as we unselected the checkbox of include attachment")
                .contains("false");
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup1.attachment.checked())
                .as("attachment is not copied and check box is uncheck for 2nd release")
                .isFalse();
        assertThat(viewAuditDetailsPopup1.attachment.isDisabled())
                .as("attachment is not copied and check box is disabled for 2nd release")
                .isTrue();
        viewAuditDetailsPopup1.cancelBtn.click();
        var applicationIssuesPage = new TenantTopNavbar()
                .openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        applicationIssuesPage.clickAll().expandAllGroupByElements();
        applicationIssuesPage.selectIssueByText(issueName2);
        var viewAuditDetailsPopup2 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        viewAuditDetailsPopup2.attachment.setValue(false);
        viewAuditDetailsPopup2.copyAuditDetailsBtn.click();
        applicationIssuesPage.pressSubmitChanges().expandAllGroupByElements();
        applicationIssuesPage.selectIssueByText(issueName2);
        applicationIssuesPage.clickCloseButtonByIssueId(issueId1);
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("second release issue doesn't contain attachment" +
                        " as we unselected the checkbox of include attachment")
                .contains("false");
        var viewAuditDetailsPopup3 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup3.attachment.checked())
                .as("attachment is not copied and check box is uncheck for 2nd release")
                .isFalse();
        assertThat(viewAuditDetailsPopup3.attachment.isDisabled())
                .as("attachment is not copied and check box is disabled for 2nd release")
                .isTrue();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify audit details in Application Issues page")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"})
    public void applicationsIssuesPageTest() {
        AllureReportUtil.info("Navigate to the Application Issues page");
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId1, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        assertThat(url())
                .as("The Issue ID now is a hyper link that opens the issue details")
                .contains(issueId1);
        var instanceId1 = page(ReleaseIssuesPage.class).getInstanceId();
        Selenide.switchTo().window(0);
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId2, issuesPage.getTable()
                        .getColumnIndex("Issue Id")).click();
        Selenide.switchTo().window(1);
        var instanceId2 = page(ReleaseIssuesPage.class).getInstanceId();
        assertThat(instanceId1)
                .as("All other Issues belonging to same release " +
                        "or different with matching InstanceId appeared")
                .isEqualTo(instanceId2);
        Selenide.switchTo().window(0);
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("All headers are present in the table")
                .containsExactlyInAnyOrderElementsOf(issueHeaders);
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a new column named Audited")
                .contains("Audited");
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Audited")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("View audit details");
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.getAuditInfoForSelectedIssue())
                .as("Verify that top section to contain the current audit " +
                        "information for the issue selected")
                .contains("Assigned User", "Developer Status", "Auditor Status", "Severity");
        assertThat(viewAuditDetailsPopup.commentSection.isDisplayed())
                .as("Verify that top section to contain latest comment of selected issue.")
                .isTrue();
        assertThat(viewAuditDetailsPopup.history.isDisplayed())
                .as("Verify that bottom section of the " +
                        "pop-up window will show the history of the issue")
                .isTrue();
        assertThat(viewAuditDetailsPopup.noAttachmentSettingMsg.getText())
                .as("If Issue selected to view details do not contain attachments " +
                        "the message will be like: The selected issue does not have any attachments.")
                .contains("The selected issue does not have any attachments.");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Take a user who is inactive in the system, " +
            "and verify the scenarios for it")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "applicationsIssuesPageTest"})
    public void verifyAuditDetailsForInactiveUserInApplicationIssuesPage() {
        var comment = "comment-" + UniqueRunTag.generate();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        issuesPage.clickCloseButtonByIssueId(issueId2);
        issuesPage.setAssignedUser(tenantDTO.getUserName());
        issuesPage.setComment(comment).pressSubmitChanges();
        new TenantTopNavbar().openAdministration().openUserManagement()
                .openUsers()
                .editUserByName(tenantDTO.getUserName())
                .setInactive(true).pressSaveBtn();
        new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup1.selectAllBtn.isDisplayed())
                .as("Verify that a global select all added to " +
                        "select all the audits and comments.")
                .isTrue();
        viewAuditDetailsPopup1.selectAllBtn.click();
        assertThat(viewAuditDetailsPopup1.unselectAllBtn.isDisplayed())
                .as("Verify that a global unselect option " +
                        "is available to unselect all.")
                .isTrue();
        assertThat(viewAuditDetailsPopup1.user.getText())
                .as("Verify that when user is inactive in the system the assigned user " +
                        "in the pop up to show the name of the user name (inactive)")
                .contains("Last Name, First Name(inactive)");
        viewAuditDetailsPopup1.cancelBtn.click(ClickOptions.usingJavaScript());
        assertThat(issuesPage.getAssignedUser())
                .as("Verify that assigned User field to be left as --No Change-- when the Issue selected to " +
                        "copy audit information has a user assigned that is no longer active in system.")
                .contains("--No Change--");
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId1, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        new ScreenshotsCell().openScreenshots().addScreenShot();
        Selenide.switchTo().window(0);
        refresh();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("true");
        var viewAuditDetailsPopup2 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup2.attachment.checked())
                .as("Verify that default state of checkbox is to have " +
                        "it selected for the Issues where Attachments are present.")
                .isTrue();
        viewAuditDetailsPopup2.cancelBtn.click();
        new TenantTopNavbar().openAdministration().openUserManagement()
                .openUsers().editUserByName(tenantDTO.getUserName())
                .setInactive(false).pressSaveBtn();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Add a tenant level setting Include attachments while copying audits " +
            "to control at the global level to select to deselect the checkbox")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "verifyAuditDetailsForInactiveUserInApplicationIssuesPage"})
    public void tenantLevelSettingForIncludeAttachmentApplicationIssuesPageTest() {
        LogInActions.adminLogIn().adminTopNavbar
                .openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openTabOptions()
                .setIncludeAttachments(true);
        BrowserUtil.clearCookiesLogOff();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.attachmentSettingMsg.getText())
                .as("Verify that Tenant setting should override the actual setting")
                .contains("This setting has been configured by the site's administrator.");
        viewAuditDetailsPopup.copyAuditDetailsBtn.click();
        issuesPage.pressSubmitChanges().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup1.attachment.checked())
                .as("attachment is copied and check box is check for 2nd release")
                .isTrue();
        viewAuditDetailsPopup1.cancelBtn.click();
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId2, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        assertThat(new ScreenshotsCell().openScreenshots().getAllScreenshots())
                .as("Verify that the attachments should be copied " +
                        "if the Include attachments checkbox is enabled.").hasSize(1);
        Selenide.switchTo().window(0);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminLogIn().adminTopNavbar
                .openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openTabOptions().setIncludeAttachments(false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that when user clicks on Submit changes all the " +
            "values from audit fields will be applied to selected list of Issues.")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "tenantLevelSettingForIncludeAttachmentApplicationIssuesPageTest"})
    public void auditChangesInApplicationIssuesPageTest() {
        AllureReportUtil.info("audit changes in application issues page");
        var comment = "comment-" + UniqueRunTag.generate();
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var issueId1 = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        var issueId2 = issuesPage.getIssueIdByReleaseName(secondRelease.getReleaseName());
        issuesPage.clickCloseButtonByIssueId(issueId2);
        issuesPage.setComment(comment).pressSubmitChanges().expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup.getAllEvents().toString())
                .as("Indicate the Issue has attachment associated with it")
                .contains(comment);
        viewAuditDetailsPopup.cancelBtn.click();
        assertThat(issuesPage.getTable().getRowsCount())
                .as("Verify that show Issues from other releases only when" +
                        " Include issues from other releases check box is enabled.")
                .isEqualTo(2);
        var viewAuditDetailsPopup1 = issuesPage.pressViewAuditDetailsByIssueId(issueId1);
        assertThat(viewAuditDetailsPopup1.isCheckboxAvailable())
                .as("Verify that a checkbox added next to each of those Audit " +
                        "and Comment entry for customer to choose and copy over to selected Issues.")
                .isTrue();
        viewAuditDetailsPopup1.clickCheckboxOfAuditOrComment(comment);
        viewAuditDetailsPopup1.copyAuditDetailsBtn.click();
        assertThat(issuesPage.isAuditFieldDisabled("Assigned User"))
                .as("Verify that the audit fields disabled when the " +
                        "copy audit details are clicked.")
                .isTrue();
        assertThat(issuesPage.isSourceIssueRowEnabled(issueId1))
                .as("Verify that the source Issue row is disabled " +
                        "in the grid when copy audit details is clicked.")
                .isTrue();
        issuesPage.submitChangesBtn.click();
        var modalDialog = new ModalDialog();
        assertThat(modalDialog.getMessage())
                .as("Verify the message in confirmation " +
                        "dialog on Submitting the changes")
                .contains(String.format(expectedMessage, issueId1));
        modalDialog.getButtonByName("Yes").click();
        modalDialog.pressClose();
        issuesPage.expandAllGroupByElements();
        issuesPage.selectIssueByText(issueName3);
        var viewAuditDetailsPopup2 = issuesPage.pressViewAuditDetailsByIssueId(issueId2);
        assertThat(viewAuditDetailsPopup2.getAllEvents().toString())
                .as("Verify that when user clicks on Submit changes all the values " +
                        "from audit fields will be applied to selected list of Issues.")
                .contains("Changed Severity from '(Default)' to 'High'");
        assertThat(viewAuditDetailsPopup2.latestComment.getText())
                .as("Verify that Prefix the Audits and Comments that are " +
                        "copied from another issue with the word [Copied]")
                .contains("[Copied] " + comment);
    }
}
