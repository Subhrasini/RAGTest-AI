package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Owner("tmagill@opentext.com")
@Slf4j
public class NavPanelsIssuesPageTest extends FodBaseTest {

    ApplicationDTO applicationDTO;

    List<String> GroupByItems = new ArrayList<>() {
        {
            add("Assigned User");
            add("Auditor Status");
            add("Category");
            add("Sink");
            add("Source");
            add("Developer Status");
            add("Has Attachments");
            add("Bug Submitted");
            add("Has Comments");
            add("Microservice");
            add("OWASP 2014 Mobile");
            add("OWASP 2017");
            add("OWASP 2021");
            add("Package");
            add("Scan Type");
            add("Status");
            add("DISA STIG 6.1");
            add("CWE Top 25 2023");
            add("NIST SP 800-53 Rev. 5");
            add("<None: no issue grouping>");
        }
    };

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant, start static scan and create a security lead")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions
                .importScanTenant(applicationDTO, "payloads/fod/chat_application_via_lan.fpr");
    }

    @MaxRetryCount(3)
    @Description("TAM should be able to enable Bug Tracker functionality")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void enableBugTracker() {
        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openBugTrackerTab().enableBugTracker(true).setBugTracker("Other").pressSave();
    }

    @MaxRetryCount(3)
    @Description("TAM should be able to select multiple issues and edit them")
    @Test(groups = {"regression"}, dependsOnMethods = {"enableBugTracker"})
    public void multiSelectIssueTest() {

        final String devStatus = "In Remediation";
        final String auditorStatus = "Remediation Required";
        final String severity = "Medium";

        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        issuesPage.clickCritical();

        var list = new ArrayList<String>();
        for (int i = 0; i < 2; i++) {
            var tmpIssue = issuesPage.openIssueByIndex(i);
            list.add(tmpIssue.getTitle());
            tmpIssue.clickCheckbox();
        }

        var issue = issuesPage.openIssueByIndex(0);
        issue.setSeverity(severity);
        issue.setDeveloperStatus(devStatus);
        issue.setAuditorStatus(auditorStatus);
        issue.setAssignedUser(defaultTenantDTO.getUserName());
        issuesPage.pressSubmitChanges();
        issuesPage = issuesPage.openOverview().openIssues();
        issuesPage.clickMedium();

        var tmpIssue = issuesPage.openIssueByIndex(0);
        SoftAssertions softAssert = new SoftAssertions();
        softAssert.assertThat(tmpIssue.getTitle().contains("incognito.java")).as("Issues should contain incognito.java").isTrue();
        softAssert.assertThat(tmpIssue.getAssignedUser()).as("Name should equal created sec lead from prepare test data")
                .isEqualTo(defaultTenantDTO.getLastName() + ", " + defaultTenantDTO.getFirstName() + " (" + defaultTenantDTO.getUserName() + ")");
        softAssert.assertThat(tmpIssue.getAuditorStatus()).as("Auditor Status should be: " + auditorStatus).isEqualTo(auditorStatus);
        softAssert.assertThat(tmpIssue.getDeveloperStatus()).as("Developer Status should be: " + devStatus).isEqualTo(devStatus);
        softAssert.assertThat(tmpIssue.getSeverity()).as("Severity should be: " + severity).isEqualTo(severity);
        softAssert.assertThat(issue.submitBugBtn.isEnabled()).as("Submit Bug Button should be enabled").isTrue();
        softAssert.assertAll();

    }

    @MaxRetryCount(3)
    @Description("TAM should be able to validate that badges with value of '0' or greater are always selectable")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "multiSelectIssueTest"},
            alwaysRun = true)
    public void verifyIssueBadgesTest() {

        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();

        HashMap<String, Integer> map = new HashMap<>();
        map.put("All", issuesPage.getAllCount());
        map.put("Critical", issuesPage.getCriticalCount());
        map.put("High", issuesPage.getHighCount());
        map.put("Medium", issuesPage.getMediumCount());
        map.put("Low", issuesPage.getLowCount());

        SoftAssertions softAssert = new SoftAssertions();

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() == 0) {
                if (e.getKey() == "High") {
                    issuesPage.clickHigh();
                    softAssert.assertThat(issuesPage.highBadge.isDisplayed()).as("High badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.criticalBadge.isDisplayed()).as("Critical badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.mediumBadge.isDisplayed()).as("Medium badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.lowBadge.isDisplayed()).as("Low badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.allBadge.isDisplayed()).as("All badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.groupByElement.isDisplayed()).as("Group By dropdown should be displayed").isTrue();
                    softAssert.assertAll();

                }
                if (e.getKey() == "Critical") {
                    issuesPage.clickCritical();
                    softAssert.assertThat(issuesPage.highBadge.isDisplayed()).as("High badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.criticalBadge.isDisplayed()).as("Critical badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.mediumBadge.isDisplayed()).as("Medium badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.lowBadge.isDisplayed()).as("Low badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.allBadge.isDisplayed()).as("All badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.groupByElement.isDisplayed()).as("Group By dropdown should be displayed").isTrue();
                    softAssert.assertAll();

                }
                if (e.getKey() == "Medium") {
                    issuesPage.clickMedium();
                    softAssert.assertThat(issuesPage.highBadge.isDisplayed()).as("High badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.criticalBadge.isDisplayed()).as("Critical badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.mediumBadge.isDisplayed()).as("Medium badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.lowBadge.isDisplayed()).as("Low badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.allBadge.isDisplayed()).as("All badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.groupByElement.isDisplayed()).as("Group By dropdown should be displayed").isTrue();
                    softAssert.assertAll();

                }
                if (e.getKey() == "Low") {
                    issuesPage.clickLow();
                    softAssert.assertThat(issuesPage.highBadge.isDisplayed()).as("High badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.criticalBadge.isDisplayed()).as("Critical badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.mediumBadge.isDisplayed()).as("Medium badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.lowBadge.isDisplayed()).as("Low badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.allBadge.isDisplayed()).as("All badge should be displayed").isTrue();
                    softAssert.assertThat(issuesPage.groupByElement.isDisplayed()).as("Group By dropdown should be displayed").isTrue();
                    softAssert.assertAll();

                }

            }
        }


    }

    @MaxRetryCount(3)
    @Description("TAM, for each Group By type, should be able to collapse and expand given group by type")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "multiSelectIssueTest"},
            alwaysRun = true)
    public void verifyGroupByTest() {
        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();

        SoftAssertions softAssert = new SoftAssertions();
        for (var item : GroupByItems) {
            issuesPage.clickAll().groupBy(item).collapseAllGroupByElements();
            softAssert.assertThat(issuesPage.groupBy(item).groupsCollapsed())
                    .as("Group By tree should be collapsed for element: " + item).isTrue();

            issuesPage.clickAll().groupBy(item).expandAllGroupByElements();
            softAssert.assertThat(issuesPage.groupBy(item).groupsCollapsed())
                    .as("Group By tree should be expanded for element: " + item).isFalse();
            softAssert.assertAll();
        }
    }

}
