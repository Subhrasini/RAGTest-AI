package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import groovy.util.logging.Slf4j;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;


@Owner("tmagill@opentext.com")
@Slf4j
public class IssuesVerifyIntroDateTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;
    String introducedDate, issueId, lastFoundDate;

    @MaxRetryCount(3)
    @Description("Import fpr, static scan and static scan same payload as fpr")
    @Test(groups = {"regression", "hf"})
    public void prepareTestData() {

        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/chat_application_via_lan.fpr");

        var releaseScansPage = page(ReleaseScansPage.class);
        releaseScansPage.getFirstScan().waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        var issuePage = new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();
        var issue = issuePage.getCriticalIssues()
                .get(0);
        issueId = issue.getId();
        introducedDate = issue.getIntroducedDate();
        lastFoundDate = issue.getLastFoundDate();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScanDTO.setFileToUpload("payloads/fod/NodeGoat-main.zip");

        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);

        new TenantTopNavbar().openApplications()
                .openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName());

        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setFileToUpload("payloads/fod/chat_application_via_lan.zip");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @FodBacklogItem("816024")
    @Severity(SeverityLevel.CRITICAL)
    @MaxRetryCount(3)
    @Description("Ensure that intro dates remain the same after issue goes to closed state")
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareTestData"})
    public void validateIntroDateTest() {
        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues();

        var issue = issuesPage.findWithSearchBox(issueId).groupBy("Status").getIssues()
                .get(0);
        AllureReportUtil.info("Compare dates...");
        assertThat(issue.getIntroducedDate())
                .as("Current introduced date should match previous introduced date")
                .isEqualTo(introducedDate);
        assertThat(lastFoundDate)
                .as("Comparing global last found date to global introduced date... they should match")
                .isEqualTo(introducedDate);
        assertThat(issue.getLastFoundDate())
                .as("Comparing latest last found date to original intro date... they should NOT match")
                .isNotEqualTo(introducedDate);
        AllureReportUtil.info("Checking the Group By Header for issue.");
        assertThat(issue.getGroupHeaders().get(0))
                .as("Checking to ensure Reopen is displayed for issue and its group by heading")
                .contains("Reopen");
    }
}
