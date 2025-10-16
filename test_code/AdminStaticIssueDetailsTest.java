package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.statics.scan.StaticScanIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.IssuesActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class AdminStaticIssueDetailsTest extends FodBaseTest {
    ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Mark issue as False Positive, Submit False Positive Challenge, verify status bar")
    @Test(groups = {"regression"})
    public void markAsFPCTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();

        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("17");
        staticScanDTO.setIncludeThirdParty(false);
        staticScanDTO.setFileToUpload("payloads/fod/demo.zip");
        staticScanDTO.setEntitlement("Single Scan");

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var page = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases().openDetailsForRelease(applicationDTO).openIssues();

        page.findWithSearchBox("App.java");
        var issuesCount = page.getAllCount();
        assertThat(issuesCount).as("There should be 1 issue").isEqualTo(1);
        var issue = page.getAllIssues().get(0).markAsFalsePositive(true);
        assertThat(issue.isUnmarkFPCButtonVisible()).as("Unmark as false positive button should be visible").isTrue();
        page.submitFalsePositiveChallenge();
        var status = page.getStatusBarText();
        assertThat(status).as("Status should reflect that FPC in progress")
                .isEqualTo("Static - Scan Challenge In Progress");

        page.openOverview().openIssues().waitTillAllCountGreaterThanOrEqual(1).getAllIssues().get(0)
                .openDetails();
        var isReviewingFPC = issue.isReviewingFPC();
        assertThat(isReviewingFPC)
                .as("Status should be New and 'Mark as false positive' button should be disabled ").isTrue();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("685003")
    @Description("Issue details on FPC scan should differ from generic ones")
    @Test(groups = {"regression"}, dependsOnMethods = {"markAsFPCTest"})
    public void adminFPCIssuesTabDetailsTest() {
        var page = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        page.appliedFilters.clearAll();
        page.filters.expandSideFiltersDrawer().setFilterByName("False Positive Challenge").expand()
                .clickFilterOptionByName("True");
        var issuesPage = page.findScanByAppName(applicationDTO.getApplicationName()).getAll().get(1)
                .openDetails()
                .openIssues();
        var issuesCounters = new IssuesCounters(0, 1, 0, 0, 1);
        IssuesActions.validateIssuesAdmin(issuesCounters);

        issuesPage = page(StaticScanIssuesPage.class);

        issuesPage.waitTillAllCountMatchExpected(1);
        var issue = issuesPage.getAllIssues().get(0);
        var issueDetails = issue.openDetails();
        var vulnTab = issueDetails.openVulnerability();
        var sections = vulnTab.getSections();

        assertThat(sections).as("There should be sections").hasSizeGreaterThan(0)
                .as("Best practices section should be removed").doesNotContain("Best Practices");

        var tabs = issueDetails.getAllTabsNames();
        assertThat(tabs).as("Recommendations tab is present").contains("Recommendations");

        String pattern = "Issue Status.*?New.*?\\(Introduced.*?\\d{4}\\/\\d{2}\\/\\d{2}\\)";
        String pattern2 = "Issue Status.*?New.*?\\(Introduced.*?\\d{2}\\/\\d{2}\\/\\d{4}\\)";
        Pattern regexPattern = Pattern.compile(pattern);
        Pattern regexPattern2 = Pattern.compile(pattern2);
        var statusText = vulnTab.getIssueStatusText();
        Matcher matcher = regexPattern.matcher(statusText);
        Matcher matcher2 = regexPattern2.matcher(statusText);
        assertThat(matcher.find() || matcher2.find()).as("Issue Status is present").isTrue();

        assertThat(vulnTab.getAnalyticPredictionTable().isDisplayed())
                .as("Scan Analytics Prediction table is present").isTrue();

        var auditSections = issue.getAuditSectionTexts();
        assertThat(auditSections).as("Audit sections should not be empty").hasSizeGreaterThan(0)
                .as("'Created By' and 'Introduced Date' should be removed")
                .doesNotContain("Created By", "Introduced Date");

        var recommendationTab = issueDetails.openRecommendations();
        sections = recommendationTab.getSections();
        assertThat(sections).as("Sections should not be empty").hasSizeGreaterThan(0)
                .as("Training section should be removed.").doesNotContain("Training");

        issuesPage = page(StaticScanIssuesPage.class);
        var filterOptions = issuesPage.filters.setFilterByName("Canned Queries").getAllOptions();
        assertThat(filterOptions).as("There should be Audit Required option").contains("Audit Required");

        issuesPage.groupBy("Status");
        var updatedPage = page(StaticScanIssuesPage.class);
        updatedPage.waitTillAllCountMatchExpected(1);
        var resultMap = updatedPage.getIssueGroupsCounts();

        assertThat(resultMap).as("There should be 'New' group").containsKey("New").
                as("Count should be 1").containsEntry("New", 1);

        updatedPage.groupBy("Has Source");
        updatedPage.waitTillAllCountMatchExpected(1);

        updatedPage = page(StaticScanIssuesPage.class);
        resultMap = updatedPage.getIssueGroupsCounts();
        assertThat(resultMap).as("There should be 'Yes' group").containsKey("Yes")
                .as("Count should be 1").containsEntry("Yes", 1);

        var popup = issuesPage.filters.openSettings();
        var filters = popup.getVisibleOptions();
        assertThat(filters).as("Filter settings should contain Scan Analytics Prediction and Has Source")
                .contains("Scan Analytics Prediction", "Has Source");

        var groups = popup.openGroupsTab().getVisibleOptions();
        assertThat(groups).as("Filter settings (Group) should contain Scan Analytics Prediction and Has Source")
                .contains("Scan Analytics Prediction", "Has Source");
    }
}