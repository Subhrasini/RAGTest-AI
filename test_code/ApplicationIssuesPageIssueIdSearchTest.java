package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
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

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class ApplicationIssuesPageIssueIdSearchTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    ReleaseDTO releaseDTO2;
    StaticScanDTO staticScanDTO;
    String fileName = "payloads/fod/chat_application_via_lan.zip";

    @MaxRetryCount(1)
    @Description("Create tenant, application, and required 2 releases")
    @Test(groups = {"regression", "hf"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();

        applicationDTO = ApplicationActions.createApplication(tenantDTO, true);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);

        releaseDTO2 = ReleaseDTO.createDefaultInstance();
        ReleaseActions.createRelease(applicationDTO, releaseDTO2);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, releaseDTO2, FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @MaxRetryCount(1)
    @FodBacklogItem("806011")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareTestData"})
    @Description("Check search for items in release issues and then in application issues")
    public void searchAppAndReleaseIDTest() {
        LogInActions.tamUserLogin(tenantDTO);
        var release1Details = pickReleasePage(applicationDTO.getApplicationName(), applicationDTO.getReleaseName(), false);

        AllureReportUtil.info("Get issue names and ids from release 1");
        var tenantIssueCell = release1Details.openIssueByIndex(0);
        var issue1Name = tenantIssueCell.getTitle();
        var issue1Id = tenantIssueCell.getId();

        release1Details.selectIssueByText(issue1Name);
        release1Details.selectReleaseIssueCheckbox(true);
        var issue2Id = release1Details.getIssueIdByReleaseName(releaseDTO2.getReleaseName());
        AllureReportUtil.info("Open new tab, surf to Applications...");
        BrowserUtil.openNewTab();
        Selenide.switchTo().window(1);
        open(FodConfig.TEST_ENDPOINT_TENANT + "/Applications");
        var yourApplicationsPage = page(YourApplicationsPage.class);
        var searchForIssues = yourApplicationsPage
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues()
                .findWithSearchBox(issue2Id);
        assertThat(searchForIssues.getIssues().get(0).getId())
                .as("Search should return " + issue2Id + " not " + issue1Id)
                .isEqualTo(issue2Id);
    }

    @MaxRetryCount(1)
    @FodBacklogItem("800051")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareTestData"})
    @Description("Test enablement of Include Issues From Other Releases checkbox with individual issues")
    public void verifyMultipleReleaseCheckboxTest() {
        AllureReportUtil.info("Running test in individual issues");
        LogInActions.tamUserLogin(tenantDTO);
        var release1Details = pickReleasePage(applicationDTO.getApplicationName(),
                applicationDTO.getReleaseName(), false);

        assertThat(release1Details.includeOtherReleases.isDisplayed())
                .as("Include Issues From Other Releases checkbox should NOT be visible")
                .isFalse();
        var issue1Name = release1Details.openIssueByIndex(0).getTitle();
        release1Details.selectIssueByText(issue1Name);
        checkCheckBoxIssues(release1Details);

        var release2Details = pickReleasePage(applicationDTO.getApplicationName(),
                releaseDTO2.getReleaseName(), false);
        assertThat(release2Details.includeOtherReleases.isDisplayed())
                .as("Include Issues From Other Releases checkbox should NOT be visible")
                .isFalse();
        var issue2Name = release2Details.openIssueByIndex(0).getTitle();
        release2Details.selectIssueByText(issue2Name);
        checkCheckBoxIssues(release2Details);
    }

    @MaxRetryCount(1)
    @FodBacklogItem("800051")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareTestData"})
    @Description("Test enablement and visibility of Include Issues From Other Releases checkbox when selecting issue groups")
    public void verifyMultipleReleaseCheckboxGroupTest() {
        AllureReportUtil.info("Running test in Issue Groups");
        LogInActions.tamUserLogin(tenantDTO);
        var release1Details = pickReleasePage(applicationDTO.getApplicationName(),
                applicationDTO.getReleaseName(), true);
        assertThat(release1Details.includeOtherReleases.isDisplayed())
                .as("Include Issues From Other Releases checkbox should NOT be visible")
                .isFalse();
        release1Details.groupBy("Category");
        release1Details.selectIssuesGroup(release1Details.getGroupHeaders().get(0));
        checkCheckBoxGroups(release1Details);

        var release2Details = pickReleasePage(applicationDTO.getApplicationName(),
                releaseDTO2.getReleaseName(), true);
        assertThat(release2Details.includeOtherReleases.isDisplayed())
                .as("Include Issues From Other Releases checkbox should NOT be visible")
                .isFalse();
        release2Details.groupBy("Category");
        release2Details.selectIssuesGroup(release2Details.getGroupHeaders().get(0));
        checkCheckBoxGroups(release2Details);
    }

    public ReleaseIssuesPage pickReleasePage(String applicationName, String releaseName, Boolean isGroupTest) {
        var releaseDetails = new TenantTopNavbar().openApplications()
                .getAppByName(applicationName)
                .openYourReleases()
                .openDetailsForRelease(applicationName, releaseName)
                .openIssues();
        if (!isGroupTest)
            return releaseDetails.clickCritical();
        else
            return releaseDetails.clickAll();
    }

    public void checkCheckBoxIssues(ReleaseIssuesPage releaseIssuesPage) {
        assertThat(releaseIssuesPage.includeOtherReleases.isDisplayed())
                .as("Include Issues From Other Releases checkbox should be visible")
                .isTrue();
        assertThat(releaseIssuesPage.includeOtherReleases.checked())
                .as("Include Issues From Other Releases checkbox should NOT be checked")
                .isFalse();
        releaseIssuesPage.selectReleaseIssueCheckbox(true);
        assertThat(releaseIssuesPage.includeOtherReleases.checked())
                .as("Include Issues From Other Releases checkbox should be checked")
                .isTrue();
        assertThat(releaseIssuesPage.getTable().getRowsCount())
                .as("Table should have at least two rows ")
                .isGreaterThan(1);
        assertThat(releaseIssuesPage.getTable()
                .getRowByColumnText(applicationDTO.getReleaseName(), 2).is(Condition.appear))
                .as(applicationDTO.getReleaseName() + " should be visible in table")
                .isTrue();
        assertThat(releaseIssuesPage.getTable()
                .getRowByColumnText(releaseDTO2.getReleaseName(), 2).is(Condition.appear))
                .as(releaseDTO2.getReleaseName() + " should be visible in table")
                .isTrue();
    }

    public void checkCheckBoxGroups(ReleaseIssuesPage releaseIssuesPage) {
        assertThat(releaseIssuesPage.getTable().getRowsCount())
                .as("Table row size should be at least 1")
                .isPositive();
        checkCheckBoxIssues(releaseIssuesPage);
        releaseIssuesPage.selectReleaseIssueCheckbox(false);
        AllureReportUtil.info("After deselect of checkbox, select another group");
        var groupName2 = releaseIssuesPage.selectIssuesGroup(releaseIssuesPage.getGroupHeaders().get(1));
        assertThat(releaseIssuesPage.getTable().getRowsCount())
                .as("Table size should be at least 2")
                .isGreaterThan(1);
        AllureReportUtil.info("Now reselect checkbox...");
        groupName2.selectReleaseIssueCheckbox(true);
        assertThat(releaseIssuesPage.getTable().getRowsCount())
                .as("Table size should be at least 4")
                .isGreaterThan(3);
        assertThat(releaseIssuesPage.getTable()
                .getRowByColumnText(applicationDTO.getReleaseName(), 2).is(Condition.appear))
                .as(applicationDTO.getReleaseName() + " should be present in table")
                .isTrue();
        assertThat(releaseIssuesPage.getTable().getRowByColumnText(releaseDTO2.getReleaseName(), 2).is(Condition.appear))
                .as(releaseDTO2.getReleaseName() + " should be present in table")
                .isTrue();
    }
}
