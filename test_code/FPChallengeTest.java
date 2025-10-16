package com.fortify.fod.ui.test.regression;


import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Spinner;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.tenant.cells.TenantIssueCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.IssuesActions;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class FPChallengeTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    DynamicScanDTO firstDynamicScan;
    DynamicScanDTO secondDynamicScan;
    String manualIssueId = "Hosting Controller Stats Browse";
    String manualIssueLocation;

    IssuesCounters dynSuper5counters = new IssuesCounters(1, 2, 3, 0, 6);
    IssuesCounters expectedAfterSuppressedCounters = new IssuesCounters(1, 2, 2, 0, 5);
    IssuesCounters dynSuper7counters = new IssuesCounters(2, 2, 1, 2, 7);
    IssuesCounters expectedAfterPressFixedSuppressedCounters = new IssuesCounters(3, 3, 3, 2);

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create application and two dynamic scans, create manual issue, complete dynamic scans")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = defaultTenantDTO;
        applicationDTO = ApplicationDTO.createDefaultInstance();

        firstDynamicScan = DynamicScanDTO.createDefaultInstance();
        secondDynamicScan = DynamicScanDTO.createDefaultInstance();
        manualIssueLocation = String.format("/MANUAL-%s", tenantDTO.getRunTag().substring(5));

        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        DynamicScanActions.createDynamicScan(firstDynamicScan, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(
                applicationDTO.getReleaseName(),
                "payloads/fod/DynSuper7.fpr",
                true,
                true,
                true,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);

        IssuesActions.validateIssuesAdmin(dynSuper7counters);

        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(secondDynamicScan, applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn();
        IssuesActions.addManualIssueExtendedAdmin(
                FodCustomTypes.ScanType.Dynamic,
                applicationDTO.getApplicationName(),
                manualIssueId,
                manualIssueLocation,
                FodCustomTypes.Severity.Critical,
                true);

        IssuesActions.validateAddedManualIssue(FodCustomTypes.Severity.Critical);
        DynamicScanActions.importDynamicScanAdmin(
                applicationDTO.getReleaseName(),
                "payloads/fod/DynSuper5.fpr",
                true,
                true,
                true,
                false);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        IssuesActions.validateIssuesAdmin(dynSuper5counters);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Suppress an issue and validate")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void suppressIssueTest() {

        var releaseDetailsPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO);

        // enable "show fixed and suppressed"
        releaseDetailsPage.showFixedSuppressedDropdown.setShowFixed(true);
        releaseDetailsPage.showFixedSuppressedDropdown.openShowDropdown();
        assertThat(releaseDetailsPage.showFixedSuppressedDropdown.isShowFixedChecked())
                .as("'Fixed' should be checked")
                .isTrue();
        releaseDetailsPage.showFixedSuppressedDropdown.setShowSuppressed(true);
        releaseDetailsPage.showFixedSuppressedDropdown.openShowDropdown();
        assertThat(releaseDetailsPage.showFixedSuppressedDropdown.isShowFixedChecked())
                .as("'Suppressed' should be checked")
                .isTrue();

        //going to issues page
        var issuesPage = releaseDetailsPage.openIssues();
        // disable "show fixed and suppressed"
        var totalIssuesBeforeFixedOff = issuesPage.getAllCount();
        issuesPage.getShowFixedSuppressedDropdown().setShowFixed(false);
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(false);
        issuesPage.waitTillAllCountMatchExpected(6);
        var totalIssuesBeforeFixedOn = issuesPage.getAllCount();

        var issue = issuesPage.getAllIssues().get(0);

        issue.setAuditorStatus("Risk Accepted");

        issuesPage = issuesPage.openIssues();
        var totalAfterSuppressed = issuesPage.waitTillAllCountMatchExpected(5).getAllCount();
        assertThat(totalIssuesBeforeFixedOn - totalAfterSuppressed)
                .as("Total issues should decrease by 1").isEqualTo(1);

        issuesPage.getShowFixedSuppressedDropdown().setShowFixed(true);
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(true);

        var totalIssuesAfterFixedOn = issuesPage.getAllCount();

        assertThat(totalIssuesAfterFixedOn)
                .as("Total issues count after enabling 'Show suppressed' should remain the same")
                .isEqualTo(totalIssuesBeforeFixedOff);

        releaseDetailsPage = issuesPage.openOverview();

        var actualAfterSuppressedCounters = new IssuesCounters(
                releaseDetailsPage.getCriticalCount(),
                releaseDetailsPage.getHighCount(),
                releaseDetailsPage.getMediumCount(),
                releaseDetailsPage.getLowCount());

        assertThat(actualAfterSuppressedCounters).as("Overview stats in Show Fixed mode have remained the same")
                .isEqualTo(expectedAfterPressFixedSuppressedCounters);

        releaseDetailsPage.showFixedSuppressedDropdown.setShowSuppressed(false);
        releaseDetailsPage.showFixedSuppressedDropdown.setShowFixed(false);

        releaseDetailsPage.waitForValueEquals(2, releaseDetailsPage::getMediumCount, Duration.ofMinutes(15));

        var actualAfterNoSuppressedCounters = new IssuesCounters(
                releaseDetailsPage.getCriticalCount(),
                releaseDetailsPage.getHighCount(),
                releaseDetailsPage.getMediumCount(),
                releaseDetailsPage.getLowCount());

        assertThat(actualAfterNoSuppressedCounters).as("Medium count should decrease by 1")
                .isEqualTo(expectedAfterSuppressedCounters);
    }

    @MaxRetryCount(1)
    @FodBacklogItem("685003")
    @Severity(SeverityLevel.NORMAL)
    @Description("Start FPC with several issues, validate on both admin and tenant sites\n" +
            "TEST in no longer valid")
    @Test(groups = {"regression"},
            dependsOnMethods = {"suppressIssueTest"},

            enabled = false)
    public void fpChallengeTest() {
        final String suppressedIssueLocation = "/258AE5";
        var expectedChallengedIssues = new ArrayList<String>() {
            {
                add(manualIssueLocation);
                add("/64BB1F");
                add("/879966");
            }
        };

        var issuesToMark = new ArrayList<String>();
        issuesToMark.addAll(expectedChallengedIssues);
        issuesToMark.add(suppressedIssueLocation);

        var issuesPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.
                openApplications().openYourReleases().openDetailsForRelease(applicationDTO).openIssues();

        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(true);
        issuesPage.clickAll();
        for (var issueLocation : issuesToMark) {
            issuesPage.findWithSearchBox(issueLocation);
            var issue = issuesPage.getAllIssues().get(0);

            issue.markAsFalsePositive(false);
            assertThat(issue.isUnmarkFPCButtonVisible()).as("Unmark as false positive button should be visible")
                    .isTrue();

            issuesPage.openOverview().openIssues().waitTillAllCountGreaterThanOrEqual(1);
            issuesPage.findWithSearchBox(issueLocation);
            issuesPage.getAllIssues().get(0).openDetails();
        }

        issuesPage.submitFalsePositiveChallenge();
        var status = issuesPage.getStatusBarText();
        assertThat(status).as("Status should reflect that FPC in progress")
                .isEqualTo("Dynamic - Scan Challenge In Progress");
        issuesPage.waitTillAllCountGreaterThanOrEqual(1);

        for (var issueLocation : expectedChallengedIssues) {
            List<TenantIssueCell> issues;
            TenantIssueCell issue = null;
            var endTime = LocalTime.now().plusMinutes(5);
            do {
                issuesPage.findWithSearchBox(issueLocation);
                new Spinner().waitTillLoading(5, true);
                issues = issuesPage.getAllIssues();
                if (issues.isEmpty()) {
                    sleep(5000);
                    refresh();
                } else {
                    issue = issues.get(0);
                    break;
                }
            } while (LocalTime.now().isBefore(endTime));

            assertThat(issue).as(issueLocation + " is not found...").isNotNull();
            var isReviewingFPC = issue.isReviewingFPC();
            assertThat(isReviewingFPC)
                    .as("Status should be New and 'Mark as false positive' button should be disabled ").isTrue();

        }

        var releaseDetailsPage = issuesPage.openOverview();

        status = releaseDetailsPage.getStatusBarText();
        assertThat(status).as("Status should reflect that FPC in progress")
                .isEqualTo("Dynamic - Scan Challenge In Progress");

        assertThat(releaseDetailsPage.getDynamicScanStatusTitle())
                .as("Scan status icon should reflect Challenge in progress")
                .isEqualTo("False positive challenge in progress on " + applicationDTO.getReleaseName());

        var releasesPage = releaseDetailsPage.tenantTopNavbar.openApplications().openYourReleases();
        releasesPage.findWithSearchBox(applicationDTO.getApplicationName());
        var scanStatus = releasesPage.getScanStatus(applicationDTO.getApplicationName(), FodCustomTypes.ScanType.Dynamic);
        assertThat(scanStatus).as("Scan status on Your Releases Page should contain `scan-status-challenged`")
                .contains("scan-status-challenged");

        BrowserUtil.clearCookiesLogOff();
        var dynamicPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic();
        dynamicPage.filters.setFilterByName("Analysis Status").clickFilterCheckboxByName("In Progress");
        var dynamicIssuesPage = dynamicPage.findWithSearchBox(applicationDTO.getApplicationName())
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();

        var popup = dynamicIssuesPage.pressPublishScanButton();
        assertThat(popup.getErrorMessage()).as("There should be error message 'Unable to Publish Scan'")
                .contains("Error - Unable to publish results because one or more issues are still in the false positive reviewing state.");

        popup.close();
        var actualIssues = dynamicIssuesPage.clickAll().groupBy("<None: no issue grouping>")
                .getAllIssues().stream().map(i -> i.getTitle()).collect(Collectors.toList());

        assertThat(actualIssues).as("There should be 3 issues").hasSize(3)
                .as("Should contain challenged issues").containsAll(expectedChallengedIssues);

        for (var issue : actualIssues) {
            dynamicIssuesPage.findWithSearchBox(issue).getAllIssues().get(0)
                    .setFalsePositiveChallenge("Issue Confirmed");
        }

        dynamicIssuesPage.pressPublishScanButton().pressOk();

        BrowserUtil.clearCookiesLogOff();
        releasesPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openApplications().openYourReleases();

        releasesPage.paging.setRecordsPerPage(releasesPage.paging.getTotalRecords());
        releasesPage.findWithSearchBox(applicationDTO.getApplicationName());
        scanStatus = releasesPage.getScanStatus(applicationDTO.getApplicationName(), FodCustomTypes.ScanType.Dynamic);
        assertThat(scanStatus).as("Scan status on Your Releases Page should be completed`")
                .contains("scan-status-completed");

        issuesPage = releasesPage.openDetailsForRelease(applicationDTO).openIssues()
                .waitTillAllCountGreaterThanOrEqual(1);
        issuesPage.getShowFixedSuppressedDropdown().setShowSuppressed(true);

        for (var issueName : expectedChallengedIssues) {
            issuesPage.findWithSearchBox(issueName);
            var issue = issuesPage.findWithSearchBox(issueName).getAllIssues().get(0);
            assertThat(issue.isConfirmed()).as("Issue should have 'Confirmed' badge").isTrue();
        }

        var suppressedStatus = issuesPage.findWithSearchBox(suppressedIssueLocation).getAllIssues().get(0).getAuditorStatus();
        assertThat(suppressedStatus).as("Suppressed issue should have 'Risk accepted'").isEqualTo("Risk Accepted");
        assertThat(issuesPage.isStatusBarVisible())
                .as("There should not be status bar with 'Scan Challenge in Progress'").isFalse();
    }
}
