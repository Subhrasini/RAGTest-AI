package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.pages.common.common.cells.IssueCell;
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
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class AuditAndStatsTest extends FodBaseTest {
    ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create application, dynamic scan, import and complete dynamic scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);

        DynamicScanActions.importDynamicScanTenant(applicationDTO, "payloads/fod/dynamic.zero.fpr");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM should be able to change issue severities and suppress issues, issue counters should update after some time")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void auditIssuesTest() {
        var page = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO);

        var actualIssuesCounters = page.getIssuesCounters();

        var totalCount = page.openIssues().getAllCount();
        page.openOverview();
        page.showFixedSuppressedDropdown.setShowFixed(true);
        page.showFixedSuppressedDropdown.setShowSuppressed(true);
        var fixedIssues = page.getIssuesCounters().minus(actualIssuesCounters);
        page.showFixedSuppressedDropdown.setShowFixed(false);
        page.showFixedSuppressedDropdown.setShowSuppressed(false);
        actualIssuesCounters = page.getIssuesCounters();

        var issuesPage = page.openIssues();
        issuesPage.clickLow();
        issuesPage.openIssueByIndex(0).setSeverity("Medium");
        issuesPage.clickMedium();
        issuesPage.openIssueByIndex(0).setSeverity("High");
        issuesPage.clickHigh();
        issuesPage.openIssueByIndex(0).setSeverity("Critical");

        var expectedIssuesCounters = actualIssuesCounters.plus(new IssuesCounters(1, 0, 0, -1));
        var overviewPage = issuesPage.openOverview();
        overviewPage.waitForValueEquals(expectedIssuesCounters, overviewPage::getIssuesCounters, Duration.ofMinutes(10));
        IssuesActions.validateOverviewIssuesTenant(expectedIssuesCounters);
        issuesPage = overviewPage.openIssues();

        var suppressedMedium = issuesPage.clickMedium().openIssueByIndex(0);
        var suppressedMediumTitle = suppressedMedium.getTitle();
        suppressedMedium.setAuditorStatus("Risk Accepted");

        var suppressedCritical = issuesPage.clickCritical().openIssueByIndex(0);
        var suppressedCriticalTitle = suppressedCritical.getTitle();
        suppressedCritical.setSeverity("Not an Issue");

        expectedIssuesCounters = expectedIssuesCounters.minus(new IssuesCounters(1, 0, 1, 0));
        fixedIssues = fixedIssues.plus(new IssuesCounters(1, 0, 1, 0));

        overviewPage = issuesPage.openOverview();
        overviewPage.waitForValueEquals(expectedIssuesCounters, overviewPage::getIssuesCounters, Duration.ofMinutes(20));
        actualIssuesCounters = overviewPage.getIssuesCounters();

        assertThat(expectedIssuesCounters).isEqualTo(actualIssuesCounters);
        issuesPage = overviewPage.openIssues();
        var expectedTotalCount = totalCount - 2;
        issuesPage.waitTillAllCountMatchExpected(expectedTotalCount);

        overviewPage = issuesPage.openOverview();
        overviewPage.showFixedSuppressedDropdown.setShowFixed(true);
        overviewPage.showFixedSuppressedDropdown.setShowSuppressed(true);
        var newExpectedIssues = expectedIssuesCounters.plus(fixedIssues);
        overviewPage.waitForValueEquals(newExpectedIssues, overviewPage::getIssuesCounters, Duration.ofMinutes(15));
        actualIssuesCounters = overviewPage.getIssuesCounters();
        assertThat(newExpectedIssues).isEqualTo(actualIssuesCounters);

        issuesPage = overviewPage.openIssues();
        expectedTotalCount += fixedIssues.getSum();
        issuesPage.waitTillAllCountMatchExpected(expectedTotalCount);
        assertThat(issuesPage.getAllCount()).isEqualTo(expectedTotalCount);

        var criticals = issuesPage.getCriticalIssues();
        assertThat(criticals.stream().map(IssueCell::getTitle).collect(Collectors.toList()))
                .as("Suppressed critical issue should be here").contains(suppressedCriticalTitle);

        var mediums = issuesPage.getMediumIssues();
        assertThat(mediums.stream().map(IssueCell::getTitle).collect(Collectors.toList()))
                .as("Suppressed medium issue should be here").contains(suppressedMediumTitle);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM should be able to bulk edit issue parameters and validate them")
    @Test(groups = {"regression"}, dependsOnMethods = {"auditIssuesTest"})
    public void bulkAuditIssuesTest() {
        final String devStatus = "In Remediation";
        final String auditorStatus = "Remediation Required";
        final String severity = "Critical";

        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        issuesPage.clickAll();

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
        issuesPage.pressSubmitChanges();

        issuesPage = issuesPage.openOverview().openIssues();
        issuesPage.clickAll();

        for (int i = 0; i < 2; i++) {
            var tmpIssue = issuesPage.openIssueByIndex(i);
            assertThat(tmpIssue.getTitle()).isEqualTo(list.get(i));
            assertThat(tmpIssue.getAuditorStatus()).isEqualTo(auditorStatus);
            assertThat(tmpIssue.getDeveloperStatus()).isEqualTo(devStatus);
            assertThat(tmpIssue.getSeverity()).isEqualTo(severity);
        }
    }
}