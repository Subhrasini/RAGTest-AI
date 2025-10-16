package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.CollectionCondition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Spinner;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
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
import utils.RetryAnalyzer;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class MyOpenIssuesTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public ReleaseDTO releaseDTO;
    String fileToUpload = "payloads/fod/chat_application_via_lan.fpr";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("400001")
    @Description("Only 3 vulnerabilities are showing when filtering to My Open Issues at the Application Issues page")
    @Test(groups = {"hf", "regression"})
    public void validateMyOpenIssuesTest() {
        AllureReportUtil.info("Prepare Application and static scan!");
        releaseDTO = ReleaseDTO.createDefaultInstance();
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions.importScanTenant(applicationDTO, fileToUpload);

        var yourScansPage = page(YourScansPage.class);
        yourScansPage.tenantTopNavbar.openApplications().openYourScans();
        var scan = yourScansPage.getScanByType(applicationDTO, FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        assertThat(scan.getTotalCount()).as("Validate that scan has issues").isPositive();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        var yourApplicationsPage = TenantLoginPage.navigate().tenantTopNavbar.openApplications();
        var issuesPage = yourApplicationsPage.openDetailsFor(applicationDTO.getApplicationName()).openIssues();

        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type");
        new Spinner().waitTillLoading();
        var issuesMap = issuesPage.getIssueGroupsCounts();
        var totalIssueCount = issuesMap.get("Static");
        assertThat(totalIssueCount).as("Validate issue count").isGreaterThan(10);
        var issue = issuesPage.getAllIssues().get(0);
        issuesPage.selectIssuesGroup(FodCustomTypes.ScanType.Static.getTypeValue());
        issue.setAssignedUser(defaultTenantDTO.getUserName());
        issue.submitChanges();

        issuesPage.clickMyOpenIssues();
        assertThat(issuesPage.appliedFilters.getFilterByName("Canned Query:").getValue())
                .as("Verify default applied filter")
                .isEqualTo("My Open Issues");
        issuesPage
                .clickAll()
                .groupBy("Scan Type")
                .issues
                .shouldHave(CollectionCondition.size(10), Duration.ofMinutes(1));
        assertThat(issuesPage.issues).as("Validate issue count").hasSize(10);
        new Spinner().waitSmallSpinnerTillLoading();
        assertThat(issuesPage.getAllIssues())
                .as("Validate total issues count after filtering to my open issues")
                .hasSize(totalIssueCount);
        assertThat(issue.getAssignedUser())
                .as("Validate assigned username after filtering to my open issues")
                .contains(defaultTenantDTO.getUserName());
    }
}