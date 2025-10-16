package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.IssuesActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class ImportDynamicScanTest extends FodBaseTest {

    ApplicationDTO dynamicAppDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create dynamic app on tenant site.")
    @Test(groups = {"regression"})
    public void createDynamicApplicationTest() {
        dynamicAppDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to import dynamic scan and validate issues on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"createDynamicApplicationTest"})
    public void importDynamicScanTest() {
        DynamicScanActions.importDynamicScanTenant(defaultTenantDTO, dynamicAppDTO, "payloads/fod/dynamic.zero.fpr", true);
        var releaseScansPage = page(ReleaseScansPage.class);
        var assessmentType = releaseScansPage.getAllScans(true).get(0).getAssessmentType();
        assertThat(assessmentType).as("Verify assessment type contains 'imported'").containsIgnoringCase("imported");

        IssuesCounters issuesCounters = new IssuesCounters(1, 3, 4, 5, 25);
        IssuesActions.validateScanIssuesTenant(issuesCounters);

        releaseScansPage.openIssues();
        IssuesActions.validateReleaseIssuesAdmin(issuesCounters);

        var issuesPage = page(ReleaseIssuesPage.class);
        issuesPage.openOverview();

        IssuesActions.validateOverviewIssuesTenant(issuesCounters);

        var detailsPage = page(ReleaseDetailsPage.class);
        var scanStatus = detailsPage.getDynamicScanStatus();
        assertThat(scanStatus).as("Status should be completed").isEqualToIgnoringCase("Completed");
    }
}