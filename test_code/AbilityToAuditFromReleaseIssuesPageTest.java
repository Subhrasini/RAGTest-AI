package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Checkbox;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.HistoryCell;
import com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.ScreenshotsCell;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import com.fortify.fod.ui.test.actions.TenantScanActions;
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

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("626008")
@Slf4j
public class AbilityToAuditFromReleaseIssuesPageTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    ReleaseDTO secondRelease;
    String fprFile = "payloads/fod/static.java.fpr";
    String jsonFile = "payloads/fod/21210_51134_cyclonedx.json";
    List<String> groupHeaders = Arrays.asList("Item Count", "Release", "Group Name", "");
    List<String> issueHeaders = Arrays.asList("Issue Id", "Release", "Primary Location", "Audited", "Attachments",
            "Scan Tool", "");

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Creating Application with importing static and open source scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        secondRelease = ReleaseDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        ReleaseActions.createReleases(applicationDTO, secondRelease);
        var totalCount = TenantScanActions
                .importScanTenant(applicationDTO, secondRelease, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .getTotalCount();
        assertThat(totalCount)
                .as("Debricked imported scan issues count should be greater then 0")
                .isPositive();
        TenantScanActions
                .importScanTenant(applicationDTO, secondRelease, fprFile,
                        FodCustomTypes.ScanType.Static).getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        totalCount = TenantScanActions
                .importScanTenant(applicationDTO, jsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .getTotalCount();
        assertThat(totalCount)
                .as("Debricked imported scan issues count should be greater then 0")
                .isPositive();
        TenantScanActions
                .importScanTenant(applicationDTO, fprFile,
                        FodCustomTypes.ScanType.Static).getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Select and unselect group and issues in release issues page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void releaseIssuesPageTest() {
        AllureReportUtil.info("Navigate to the release issue page");
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        issuesPage.clickAll();
        AllureReportUtil.info("Check the checkbox with one of a groups name issues are grouped by");
        issuesPage.groupBy("Scan Type");
        var subIssues = issuesPage
                .selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue())
                .selectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        assertThat(subIssues.getTable().getColumnHeaders())
                .as("All headers should present in the table")
                .containsExactlyInAnyOrderElementsOf(groupHeaders);
        assertThat(subIssues.getTable().getAllDataRows().texts())
                .as("It should appear the table with selected " +
                        "group and number of issues for each releases")
                .contains("26\t" + applicationDTO.getReleaseName() + "\tOpen Source")
                .contains("4\t" + applicationDTO.getReleaseName() + "\tStatic");
        AllureReportUtil.info("Items for each release can be unselected separately");
        issuesPage.unSelectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue())
                .unSelectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        AllureReportUtil.info("If the entire group selected, user can't select issues inside the group");
        issuesPage.selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        assertThat(subIssues.getIssues().get(0).isCheckboxDisabled())
                .as("Checkbox is disabled for issues which is " +
                        "available under checkbox selected group")
                .isFalse();
        AllureReportUtil.info("User can only select issues inside other (not selected) groups");
        var issueCell = subIssues.getIssuesByHeaders(
                FodCustomTypes.ScanType.Static.getTypeValue()).get(0);
        assertThat(issueCell.isCheckboxDisabled())
                .as("Checkbox is enabled for issues which is " +
                        "available under checkbox unselected group")
                .isFalse();
        issueCell.clickCheckbox();
    }


    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify audit field in release issues page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void releaseIssuesPageAuditFieldsTest() {
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        var lowCount = issuesPage.getLowCount();
        var criticalCount = issuesPage.clickCritical().getCriticalCount();
        issuesPage.groupBy("Scan Type");
        var subIssues = issuesPage
                .selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        AllureReportUtil.info("Make some changes for audit fields " +
                "and press the [submit changes] button");
        issuesPage.setSeverity("Low").pressSubmitChanges();
        assertThat(issuesPage.getLowCount())
                .as("Issues in the selected group will have updates, " +
                        "all Open Source critical issues moved from critical to low")
                .isEqualTo(lowCount + criticalCount);
        assertThat(issuesPage.getCriticalCount())
                .as("Issues in the selected group will have updates, " +
                        "Now critical issue count is zero")
                .isZero();
        issuesPage.clickLow().getIssuesByHeaders(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        assertThat(new HistoryCell().openHistory().getAuditContents())
                .as("Issues in the selected group will have updates")
                .contains("Changed Severity from '(Default)' to 'Low'");
        issuesPage.clickAll();
        AllureReportUtil.info("Check the checkbox near the issue name");
        subIssues.getIssuesByHeaders(
                FodCustomTypes.ScanType.OpenSource.getTypeValue()).get(0).clickCheckbox();
        var issueCell = issuesPage.openIssueByIndex(0);
        assertThat(issueCell.isDropdownDisplayed("Assigned User"))
                .as("Audit fields should have Assigned user").isTrue();
        assertThat(issueCell.isDropdownDisplayed("Developer Status"))
                .as("Audit fields should have Developer Status").isTrue();
        assertThat(issueCell.isDropdownDisplayed("Auditor Status"))
                .as("Audit fields should have Auditor Status").isTrue();
        assertThat(issueCell.isDropdownDisplayed("Severity"))
                .as("Audit fields should have Severity").isTrue();
        assertThat(issueCell.comment.isDisplayed())
                .as("Audit fields should have Comment section").isTrue();
        assertThat(subIssues.getTable().getColumnHeaders())
                .as("All headers are present in the table")
                .containsExactlyInAnyOrderElementsOf(issueHeaders);
        assertThat(subIssues.getTable().getRowsCount())
                .as("Multi Issue audit view, only one issue still shown")
                .isEqualTo(1);
        assertThat(String.join(" ", subIssues.getTable().getAllDataRows().texts()))
                .as("Multi Issue audit view, issue details shown")
                .contains(issueCell.getId(), applicationDTO.getReleaseName(),
                        "cryptography@37.0.2", "false", "Debricked");
        var releaseIssueCheckbox = new Checkbox($("#releaseIssueCheckboxChecked"));
        assertThat(releaseIssueCheckbox.isDisplayed())
                .as("Include Issues From Other Releases checkbox " +
                        "is present Under the Multiple Issues Audit Title").isTrue();
        assertThat(releaseIssueCheckbox.checked())
                .as("checkbox Include Issues From Other " +
                        "Releases unchecked by default").isFalse();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify by including the issues from other releases")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void includeIssuesFromOtherReleaseTest() {
        AllureReportUtil.info("Check the checkbox Include Issues From Other Releases");
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.selectIssueByText("cryptography@37.0.2");
        new Checkbox($("#releaseIssueCheckboxChecked")).click();
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
                .as("There is a new column named Attachments")
                .contains("Attachments");
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("false");
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a new column to indicate the scan tool for issues")
                .contains("Scan Tool");
        assertThat(issuesPage.getTable().getAllColumnValues(issuesPage.getTable()
                .getColumnIndex("Scan Tool")))
                .as("indicate the tool is used for scanning")
                .contains("Debricked");
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a column named Issue ID")
                .contains("Issue Id");
        assertThat(issuesPage.closeBtn.isDisplayed())
                .as("The issue in the list have the (X) button")
                .isTrue();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Audit and save changes by unselecting one of the issue")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "releaseIssuesPageAuditFieldsTest"})
    public void auditAndSaveChangesTest() {
        AllureReportUtil.info("Unselect one of the issues, " +
                "then provide some changes in audit and save the changes");
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        var mediumCount = issuesPage.getMediumCount();
        var highCount = issuesPage.clickHigh().getHighCount();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        var issueId = issuesPage
                .selectIssueByText("ResourceLeakExample.java : 24").getId();
        issuesPage.selectIssueByText("ResourceLeakExample.java : 26");
        issuesPage.clickCloseButtonByIssueId(issueId);
        issuesPage.setSeverity("Medium").pressSubmitChanges();
        assertThat(issuesPage.getMediumCount())
                .as("Changes applied to all selected issues, " +
                        "issue count increased by 1")
                .isEqualTo(mediumCount + 1);

        assertThat(issuesPage.getHighCount())
                .as("Changes not applied to issues that were unselected")
                .isEqualTo(highCount - 1);

        issuesPage.clickMedium().expandAllGroupByElements();
        issuesPage.openIssueByText("ResourceLeakExample.java : 26");
        assertThat(new HistoryCell().openHistory().getAuditContents())
                .as("All audit changes logged in history page")
                .contains("Changed Severity from '(Default)' to 'Medium'");

        issuesPage.clickHigh().expandAllGroupByElements();
        issuesPage.openIssueByText("ResourceLeakExample.java : 24");
        assertThat(new HistoryCell().openHistory().getAllEvents())
                .as("No changes logged in history of issues that were unselected")
                .doesNotContain("Changed Severity from '(Default)' to 'Medium'");

        issuesPage.clickHigh().expandAllGroupByElements();
        issuesPage.selectIssueByText("ResourceLeakExample.java : 48");
        issuesPage.setSeverity("Medium").pressSubmitChanges();
        issuesPage.clickMedium().expandAllGroupByElements();
        issuesPage.openIssueByText("ResourceLeakExample.java : 26");

        new ScreenshotsCell().openScreenshots().addScreenShot();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.clickMedium().expandAllGroupByElements();
        issuesPage.selectIssueByText("ResourceLeakExample.java : 26");

        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("true");
    }
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify release filter and by select and unselect " +
            "issues in application issue page")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"})
    public void applicationsIssuesPageTest() {
        AllureReportUtil.info("Navigate to the Application Issues page");
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        issuesPage.openSettings().openFiltersTab().addFilterByName("Release");
        Filters filter = new Filters();
        filter.expandSideFiltersDrawer();
        assertThat(filter.getAllFilters())
                .as("There should be a filter with Release")
                .contains("Release");
        filter.setFilterByName("Release").expand()
                .clickFilterCheckboxByName(secondRelease.getReleaseName()).apply();
        assertThat(issuesPage.getAllCount())
                .as("Only issues belonging to a particular release are displayed")
                .isEqualTo(30);
        issuesPage.appliedFilters.clearAll();
        issuesPage.clickAll();
        AllureReportUtil.info("Check the checkbox with one of a groups name issues are grouped by");
        issuesPage.groupBy("Scan Type");
        var subIssues = issuesPage
                .selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue())
                .selectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        assertThat(subIssues.getTable().getColumnHeaders())
                .as("All headers should present in the table")
                .containsExactlyInAnyOrderElementsOf(groupHeaders);
        assertThat(subIssues.getTable().getAllDataRows().texts())
                .as("It should appear the table with selected " +
                        "group and number of issues for each releases")
                .contains("26\t" + applicationDTO.getReleaseName() + "\tOpen Source")
                .contains("4\t" + applicationDTO.getReleaseName() + "\tStatic");
        AllureReportUtil.info("Items for each release can be unselected separately");
        issuesPage.unSelectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue())
                .unSelectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        AllureReportUtil.info("If the entire group selected, user can't select issues inside the group");
        issuesPage.selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        assertThat(subIssues.getIssues().get(0).isCheckboxDisabled())
                .as("Checkbox is disabled for issues which is " +
                        "available under checkbox selected group")
                .isFalse();
        AllureReportUtil.info("User can only select issues inside other (not selected) groups");
        var issueCell = subIssues.getIssuesByHeaders(
                FodCustomTypes.ScanType.Static.getTypeValue()).get(0);
        assertThat(issueCell.isCheckboxDisabled())
                .as("Checkbox is enabled for issues which is " +
                        "available under checkbox unselected group")
                .isFalse();
        issueCell.clickCheckbox();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify audit field in application issues page")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "releaseIssuesPageAuditFieldsTest"})
    public void applicationIssuesPageAuditFieldsTest() {
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        var criticalCount = issuesPage.getCriticalCount();
        var lowCount = issuesPage.clickLow().getLowCount();
        issuesPage.groupBy("Scan Type");
        var subIssues = issuesPage
                .selectIssuesGroup(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        AllureReportUtil.info("Make some changes for audit fields " +
                "and press the [submit changes] button");
        issuesPage.setSeverity("Critical").pressSubmitChanges();
        assertThat(issuesPage.getCriticalCount())
                .as("Issues in the selected group will have updates, " +
                        "all OS low issues moved from low to critical")
                .isEqualTo(lowCount + criticalCount);
        assertThat(issuesPage.getLowCount())
                .as("Issues in the selected group will have updates, " +
                        "Now low issue count is zero")
                .isZero();
        issuesPage.clickCritical().getIssuesByHeaders(FodCustomTypes.ScanType.OpenSource.getTypeValue());
        issuesPage.expandAllGroupByElements();
        issuesPage.selectIssueByText("cryptography@37.0.2");
        var issueId = issuesPage.getIssueIdByReleaseName(applicationDTO.getReleaseName());
        issuesPage.getTable()
                .getCellByTextAndIndex(issueId, issuesPage.getTable().getColumnIndex("Issue Id"))
                .click();
        Selenide.switchTo().window(1);
        assertThat(url())
                .as("The Issue ID now is a hyper link that opens the issue details")
                .contains(issueId);
        assertThat(new HistoryCell().openHistory().getAllEvents().toString())
                .as("Issues in the selected group will have updates")
                .contains("Changed Severity from 'Low' to 'Critical'");
        Selenide.switchTo().window(0);
        issuesPage.clickAll().expandAllGroupByElements();
        AllureReportUtil.info("Check the checkbox near the issue name");
        subIssues.getIssuesByHeaders(
                FodCustomTypes.ScanType.Static.getTypeValue()).get(0).clickCheckbox();
        assertThat(subIssues.getTable().getColumnHeaders())
                .as("All headers are present in the table")
                .containsExactlyInAnyOrderElementsOf(issueHeaders);
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a new column named Attachments")
                .contains("Attachments");
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("false");
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a new column to indicate the scan tool for issues")
                .contains("Scan Tool");
        assertThat(issuesPage.getTable().getAllColumnValues(issuesPage.getTable()
                .getColumnIndex("Scan Tool")))
                .as("indicate the tool is used for scanning")
                .contains("SAST");
        assertThat(issuesPage.getTable().getColumnHeaders())
                .as("There is a column named Issue ID")
                .contains("Issue Id");
        assertThat(issuesPage.closeBtn.isDisplayed())
                .as("The issue in the list have the (X) button")
                .isTrue();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify audit and save changes by unselecting one of the issue")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"})
    public void applicationAuditAndSaveChangesTest() {
        AllureReportUtil.info("Unselect one of the issues, " +
                "then provide some changes in audit and save the changes");
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        var mediumCount = issuesPage.getMediumCount();
        var highCount = issuesPage.clickHigh().getHighCount();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        var issueId = issuesPage
                .selectIssueByText("cryptography@37.0.2").getId();
        issuesPage.selectIssueByText("lxml@4.6.3");
        issuesPage.clickCloseButtonByIssueId(issueId);
        issuesPage.setSeverity("Medium").pressSubmitChanges();
        assertThat(issuesPage.getMediumCount())
                .as("Changes applied to all selected issues, " +
                        "issue count increased by 3")
                .isEqualTo(mediumCount + 3);
        assertThat(issuesPage.getHighCount())
                .as("Changes not applied to issues that were unselected")
                .isEqualTo(highCount - 3);
        issuesPage.clickMedium().openIssueByText("lxml@4.6.3");
        assertThat(new HistoryCell().openHistory().getAuditContents())
                .as("All audit changes logged in history page")
                .contains("Changed Severity from '(Default)' to 'Medium'");
        issuesPage.clickHigh().openIssueByText("cryptography@37.0.2");
        assertThat(new HistoryCell().openHistory().getAllEvents())
                .as("No changes logged in history of issues that were unselected")
                .doesNotContain("Changed Severity from '(Default)' to 'Medium'");
        issuesPage.clickMedium().openIssueByText("lxml@4.6.3");
        new ScreenshotsCell().openScreenshots().addScreenShot();
        issuesPage.clickAll().expandAllGroupByElements();
        issuesPage.clickMedium().selectIssueByText("lxml@4.6.3");
        assertThat(issuesPage.getTable()
                .getAllColumnValues(issuesPage.getTable().getColumnIndex("Attachments")))
                .as("Indicate the Issue has attachment associated with it")
                .contains("true");
    }
}
