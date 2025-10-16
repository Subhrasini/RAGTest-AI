package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class ReleaseAnalysisTabTest extends FodBaseTest {

    ApplicationDTO webApp;
    DynamicScanDTO dynamicScanDTO;
    StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant, entitlement, application, release, create and complete static and dynamic scans")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        webApp = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp);
        BrowserUtil.clearCookiesLogOff();

        IssuesActions.importScanAndValidateDynamicIssuesAdmin(
                webApp.getApplicationName(),
                "payloads/fod/dynamic.zero.fpr",
                false,
                true,
                false,
                new IssuesCounters(1, 3, 4, 5, 25),
                true);
        DynamicScanActions.completeDynamicScanAdmin(webApp, false);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.validateDynamicScanCompletedTenant(defaultTenantDTO, webApp);
        StaticScanActions.createStaticScan(staticScanDTO, webApp);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(webApp, true);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to view release analytics results on Analysis tab of Release Details page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void releaseAnalysisTabTest() {
        var notAssigned = "(Not Set)";
        var secLeadName = String.format("%s, %s", defaultTenantDTO.getLastName(), defaultTenantDTO.getFirstName());
        String[][] paramsArray = new String[][]{
                {notAssigned, "Open", "Pending Review"},
                {notAssigned, "In Remediation", "Remediation Required"},
                {notAssigned, "Remediated", "Remediation Deferred"},
                {secLeadName, "Will Not Fix", "Risk Mitigated"},
                {secLeadName, "Third Party Component", "Risk Accepted"},
                {secLeadName, "Open", "Not An Issue"}
        };
        var expectedAuditorStatuses = Arrays.asList(
                new String[]{"Remediation Required", "Remediation Deferred", "Risk Mitigated", "Risk Accepted", "Not an Issue"});

        var expectedDeveloperStatuses = Arrays.asList(
                new String[]{"In Remediation", "Remediated", "Will Not Fix", "Third Party Component"});

        var overViewPage = LogInActions
                .tamUserLogin(defaultTenantDTO.getAssignedUser(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .openYourReleases().openDetailsForRelease(webApp);

        var issuesPage = overViewPage.openIssues();

        issuesPage.showSuppressed();
        issuesPage.clickAll();
        int allIssuesCount = issuesPage.getAllCount();
        issuesPage.groupBy("<None: no issue grouping>");

        for (int i = 0; i < paramsArray.length; i++) {
            var issue = issuesPage.openIssueByIndex(i);
            issue.setAssignedUser(paramsArray[i][0]);
            issue.setFalsePositiveChallenge(paramsArray[i][1]);
            issue.setAuditorStatus(paramsArray[i][2]);
        }

        overViewPage = issuesPage.openOverview().openAnalysisTab().groupBy("Assignment");

        var cells = overViewPage.getCells();
        var groupBy = overViewPage.getGroupByText();
        assertThat(cells.size()).as("There should be 2 rows").isEqualTo(2);
        assertThat(groupBy).as("Group by Assignment applied").containsIgnoringCase("Assignment");
        assertThat(cells.get(0).getName()).as("First cell should contain 'Not Assigned'").containsIgnoringCase(notAssigned);
        assertThat(cells.get(1).getName()).as("Second cell should contain SecLead Name").containsIgnoringCase(secLeadName);
        int secLeadIssuesCount = cells.get(1).getTotalNumber();
        assertThat(secLeadIssuesCount).as("Only 3 issues assigned to SecLead").isEqualTo(3);
        int notAssignedCount = cells.get(0).getTotalNumber();
        assertThat(notAssignedCount).as("All other is 'Not Set'").isEqualTo(allIssuesCount - secLeadIssuesCount);


        issuesPage = overViewPage.getCells().get(1).clickName();
        assertThat(issuesPage.getPageTitle())
                .as("Clicking on [SecLead] redirects to the <Release Issues> page.")
                .isEqualToIgnoringCase("Release Issues");

        assertThat(issuesPage.getAllCount()).as("Only 3 issues which assigned to the security lead are displayed.").isEqualTo(3);

        overViewPage = issuesPage.openOverview().openAnalysisTab().groupBy("Auditor Status");
        cells = overViewPage.getCells();
        assertThat(cells.size()).as("Group by 'Auditor status' applied. There are 6 rows").isEqualTo(6);

        for (var status : overViewPage.getAnalysisNames()
                .stream().filter(Predicate.not(e -> e.equals("Pending Review")))
                .collect(Collectors.toList())) {
            boolean contains = false;
            log.debug("Checking: {}", status);
            for (var expectedStatus : expectedAuditorStatuses) {
                log.debug("Expected status: {} ", expectedStatus);
                if (status.contains(expectedStatus)) {
                    contains = true;
                    break;
                }
            }
            assertThat(contains).as(String.format("Check if Auditor Status %s is one of expected", status)).isTrue();
        }

        assertThat(overViewPage.getCellByName("Pending Review").getTotalNumber())
                .as("All other are Pending Review")
                .isEqualTo(allIssuesCount - expectedAuditorStatuses.size());

        issuesPage = overViewPage.getCellByName("Pending Review").clickName();
        assertThat(issuesPage.getPageTitle())
                .as("Clicking on [Pending Review] redirects to the <Release Issues> page.")
                .isEqualToIgnoringCase("Release Issues");

        var filterValue = issuesPage.appliedFilters.getFilterByName("Auditor Status").getValue();
        assertThat(filterValue).as("Only issues with Auditor status = Pending Review should be displayed.")
                .containsIgnoringCase("Pending Review");

        issuesPage.appliedFilters.clearAll();
        issuesPage.clickAll();
        issuesPage.groupBy("Category");
        var issuesMap = issuesPage.getIssueGroupsCounts();
        overViewPage = issuesPage.openOverview().openAnalysisTab().groupBy("Category");
        for (var issue : issuesMap.entrySet()) {
            var analysisCell = overViewPage.getCellByName(issue.getKey());
            assertThat(analysisCell.getName()).as("Check if issue exist").isNotNull();
            assertThat(analysisCell.getTotalNumber())
                    .as("Check if issues count matches issues count in IssuesGroups")
                    .isEqualTo(issue.getValue());
        }
        Selenide.refresh();
        overViewPage.openAnalysisTab().groupBy("Developer Status");

        cells = overViewPage.getCells();
        groupBy = overViewPage.getGroupByText();
        assertThat(cells.size()).as("There should be 5 rows").isEqualTo(5);
        assertThat(groupBy).as("Group by Developer Status applied").containsIgnoringCase("Developer Status");

        for (var status : expectedDeveloperStatuses) {
            var analysisCell = overViewPage.getCellByName(status);
            assertThat(analysisCell.getName()).as("Check if issue exist").isNotNull();
            assertThat(analysisCell.getTotalNumber()).as("Value should be 1").isEqualTo(1);
        }

        var openCell = overViewPage.getCellByName("Open");
        assertThat(openCell.getTotalNumber()).as("Open count should equal'")
                .isEqualTo(allIssuesCount - expectedDeveloperStatuses.size());

        issuesPage = openCell.clickTotal();
        assertThat(issuesPage.getPageTitle())
                .as("Clicking on [Total Count] redirects to the <Release Issues> page.")
                .isEqualToIgnoringCase("Release Issues");

        filterValue = issuesPage.appliedFilters.getFilterByName("Developer Status").getValue();
        assertThat(filterValue).as("Only issues with Developer Status = Pending Review should be displayed.")
                .containsIgnoringCase("Open");

        issuesPage.appliedFilters.clearAll();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type");
        issuesMap = issuesPage.getIssueGroupsCounts();
        overViewPage = issuesPage.openOverview().openAnalysisTab().groupBy("Scan Type");
        for (var issue : issuesMap.entrySet()) {
            var analysisCell = overViewPage.getCellByName(issue.getKey());
            assertThat(analysisCell.getName()).as("Check if issue exist").isNotNull();
            assertThat(analysisCell.getTotalNumber())
                    .as("Check if issues count matches issues count in IssuesGroups")
                    .isEqualTo(issue.getValue());
        }
    }
}
