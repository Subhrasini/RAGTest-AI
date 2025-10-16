package com.fortify.fod.ui.test.regression;

import com.fortify.common.api.providers.OctaneRestClient;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import jdk.jfr.Description;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.internal.collections.Pair;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

@Slf4j
@FodBacklogItem("306001")
@FodBacklogItem("1639008")
@Owner("oradchenko@opentext.com")
public class BugtrackerTest extends FodBaseTest {

    final String octaneUrl = FodConfig.OCTANE_BASE_URI.split("/api")[0];
    final String workspacePath = "/shared_spaces/112082/workspaces/12002/";
    final String octaneUser = FodConfig.OCTANE_CLIENT_ID;
    final String octanePassword = FodConfig.OCTANE_CLIENT_SECRET;
    final String redirectUrlPath = "/Redirect/Issues/";
    final String defectTypeValue = "{\"defect_type\":  { \"type\": \"list_node\", \"id\": " +
            "\"list_node.defect_type.PreRelease\", \"activity_level\": 0, \"logical_name\": " +
            "\"list_node.defect_type.PreRelease\",\"name\": \"Pre-release\"}}";
    List<String> bugIdsToDelete = new ArrayList<>();
    ApplicationDTO app1, app2, app3, app4;

    String octaneDropdownName = "ValueEdge/ALM Octane";

    @MaxRetryCount(2)
    @Test(groups = {"regression"})
    @Description("Update the defect in bugtracker system with the new release issue link  when release with issue " +
            "associated with defect gets copied over. Bug state management is ON")
    public void octaneIntegrationBugStateManagementOnTest() {
        bugtrackerOctaneTest(true);
    }

    @MaxRetryCount(2)
    @Test(groups = {"regression"})
    @Description("Update the defect in bugtracker system with the new release issue link  when release with issue " +
            "associated with defect gets copied over. Bug state management is OFF")
    public void octaneIntegrationBugStateManagementOffTest() {
        bugtrackerOctaneTest(false);
    }

    private void bugtrackerOctaneTest(boolean bugStateManagement) {

        LogInActions.tamUserLogin(defaultTenantDTO);
        var applicationDTO = ApplicationActions.createApplication();
        new TenantTopNavbar()
                .openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openBugTrackerTab().enableBugTracker(true)
                .setBugTracker(octaneDropdownName).setBugtrackerUrl(octaneUrl).setBugtrackerLogin(octaneUser)
                .setBugtrackerPassword(octanePassword) .pressAuthenticate()
                .verifySortedDomains(String.valueOf(FodCustomTypes.BugTrackerType.OCTANE))
                .setBugtrackerToolDomain("ESP_646895359")
                .verifySortedComponent(String.valueOf(FodCustomTypes.BugTrackerType.OCTANE))
                .setBugtrackerToolComponent("Fortify Automation - 8H")
                .setBugStateManagement(bugStateManagement)
                .pressSave();

        var page = StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/static.java.fpr")
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues().waitTillAllCountGreaterThanOrEqual(1);
        var issues = page.getIssues();
        var issue = issues.get(0);
        var issueId = issue.getId();
        var popup = issue.pressSubmitBug();
        var bugDescriptionPre = popup.getBugDescription();
        var bugDescription = bugDescriptionPre
                .replaceAll("target=\"_blank\"", "");

        var bugSubject = popup.getBugSubject();
        var releases = popup.getAllReleases();
        var releasesSize = releases.size();
        popup.selectRelease(releases.get(releasesSize - 1));

        popup.pressSubmit();

        with().pollInterval(Duration.ofSeconds(10)).atMost(Duration.ofMinutes(5)).await().pollInSameThread().
                untilAsserted(
                        () -> assertThat(
                                page.openScans().openIssues().getIssues().get(0).isViewBugBtnEnabled())
                                .as("Bug should be created")
                                .isTrue());

        var bugUrl = issues.get(0).getBugUrl();
        var bugId = bugUrl.split("&id=")[1];
        bugIdsToDelete.add(bugId);

        await().pollInSameThread().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("name"))
                        .as("Defect Name from API should contain issue ID").contains(issueId)
                        .as("Name from API should be equal the one in the submit bug popup")
                        .isEqualToIgnoringWhitespace(bugSubject));

        await().pollInSameThread().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("description"))
                        .as("Defect Description from API should contain issue link")
                        .contains(redirectUrlPath + issueId)
                        .as("Defect Description from API should contain the one from the submit bug popup")
                        .containsIgnoringWhitespaces(bugDescription));

        var client = new OctaneRestClient();
        client.putRequest(workspacePath + "defects/" + bugId, defectTypeValue);

        var copyRelease = ReleaseDTO.createDefaultInstance();
        copyRelease.setCopyState(true);
        copyRelease.setCopyFromReleaseName(applicationDTO.getReleaseName());
        issue = ReleaseActions.createReleases(applicationDTO, copyRelease).openIssues()
                .waitTillAllCountGreaterThanOrEqual(1).getIssues().get(0);
        var issueId2 = issue.getId();

        await().pollInSameThread().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("name"))
                        .as("Defect name from API should contain issue 1 ID").contains(issueId)
                        .as("Defect name from API should contain issue 2 ID").contains(issueId2)
                        .as("Original defect name should be contained in the name from API")
                        .contains(bugSubject));

        await().pollInSameThread().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("description"))
                        .as("Defect Description from API should contain issue 1 link")
                        .contains(redirectUrlPath + issueId)
                        .as("Defect Description from API should contain issue 2 link")
                        .contains(redirectUrlPath + issueId2)
                        .as("Defect Description from API should contain the one from the submit bug popup")
                        .containsIgnoringWhitespaces(bugDescription));

        AllureReportUtil.info("Update defect with custom text");
        var customText = UniqueRunTag.generate();
        var updateObject = new JSONObject();
        updateObject.put("description", getDefectInfo(bugId)
                .getString("description") + customText);
        client.putRequest(workspacePath + "defects/" + bugId, updateObject.toString());

        var copyRelease2 = ReleaseDTO.createDefaultInstance();
        copyRelease2.setCopyState(true);
        copyRelease2.setCopyFromReleaseName(copyRelease.getReleaseName());

        issue = ReleaseActions.createReleases(applicationDTO, copyRelease2).openIssues()
                .waitTillAllCountGreaterThanOrEqual(1).getIssues().get(0);
        var issueId3 = issue.getId();

        await().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("name"))
                        .as("Defect name from API should contain issue 1 ID").contains(issueId)
                        .as("Defect name from API should contain issue 2 ID").contains(issueId2)
                        .as("Defect name from API should contain issue 3 ID").contains(issueId3)
                        .as("Original defect name should be contained in the name from API")
                        .contains(bugSubject));

        await().untilAsserted(
                () -> assertThat(getDefectInfo(bugId).getString("description"))
                        .as("Defect Description from API should contain custom text edited in Octane")
                        .contains(customText)
                        .as("Defect Description from API should contain issue 1 link")
                        .contains(redirectUrlPath + issueId)
                        .as("Defect Description from API should contain issue 2 link")
                        .contains(redirectUrlPath + issueId2)
                        .as("Defect Description from API should contain issue 3 link")
                        .contains(redirectUrlPath + issueId3)
                        .as("Defect Description from API should contain the one from the submit bug popup")
                        .containsIgnoringWhitespaces(bugDescription));
    }

    @MaxRetryCount(2)
    @Description("Check that copy state still works for \"other\" bugtracker with manually added link")
    @Test(groups = {"regression"})
    void otherBugtrackerTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        var applicationDTO = ApplicationActions.createApplication();
        new TenantTopNavbar()
                .openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openBugTrackerTab().enableBugTracker(true)
                .setBugTracker("Other").pressSave();

        var page = StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/static.java.fpr")
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues().waitTillAllCountGreaterThanOrEqual(1);
        var issues = page.getIssues();
        var issue = issues.get(0);
        var popup = issue.pressSubmitBug();
        var url = String.format("https://bugtracker.io/%s", UniqueRunTag.generate());
        popup.setBugUrl(url);
        popup.pressSave();

        with().pollInterval(Duration.ofSeconds(10)).atMost(Duration.ofMinutes(5)).await().pollInSameThread().
                untilAsserted(
                        () -> assertThat(
                                page.getIssues().get(0).isViewBugBtnEnabled())
                                .as("Bug should be created").isTrue());

        var copyRelease = ReleaseDTO.createDefaultInstance();
        copyRelease.setCopyState(true);
        copyRelease.setCopyFromReleaseName(applicationDTO.getReleaseName());
        issue = ReleaseActions.createReleases(applicationDTO, copyRelease).openIssues()
                .waitTillAllCountGreaterThanOrEqual(1).getIssues().get(0);
        assertThat(issue.isViewBugBtnEnabled()).as("'View bug' should be enabled for copied release").isTrue();
        assertThat(issue.getBugUrl()).as("Copied release should have an assigned bug with correct url")
                .isEqualTo(url);
    }

    @FodBacklogItem("797039")
    @FodBacklogItem("841022")
    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Description("Verify Purging applications with vulnerabilities submitted to bug tracker does not deletes the bug " +
            "tracker link on other applications")
    @Test(groups = {"regression", "hf"})
    public void verifyBugTrackerLinkOfApplicationsAfterPurging() {
        AllureReportUtil.info("Create multiple applications and submit bugs to the bug tracker");
        LogInActions.tamUserLogin(defaultTenantDTO);
        app1 = ApplicationDTO.createDefaultInstance();
        app2 = ApplicationDTO.createDefaultInstance();
        app3 = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplications(app1, app2, app3);
        submitVulnerabilitiesToBugTracker(app1, false);
        submitVulnerabilitiesToBugTracker(app2, false);
        submitVulnerabilitiesToBugTracker(app3, false);

        AllureReportUtil.info("Delete one application from tenant site");
        ApplicationActions.purgeApplication(app1);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Purge deleted application from admin site");
        LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName())
                .openApplications()
                .getApplicationByName(app1.getApplicationName())
                .pressPurge()
                .pressYes();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, true, Table::isEmpty, Duration.ofMinutes(1), true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Check Vulnerabilities submitted to bug tracker for all applications");
        LogInActions.tamUserLogin(defaultTenantDTO);
        verifyIssuesPageViewBugButton(app2);
        verifyIssuesPageViewBugButton(app3);
    }

    @FodBacklogItem("771001")
    @Severity(SeverityLevel.NORMAL)
    @Owner("tmagill@opentext.com")
    @MaxRetryCount(2)
    @Description("Test to verify update to issue does not state of button label from 'View Bug' to 'Submit'")
    @Test(groups = {"regression"})
    public void bugTrackerIssueUpdateTest() {
        StaticScanDTO staticScanDTO;
        LogInActions.tamUserLogin(defaultTenantDTO);
        app4 = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplications(app4);
        Pair<String, String> issuesStrings = submitVulnerabilitiesToBugTracker(app4, true);
        String bugID = issuesStrings.first();
        String issueID = issuesStrings.second();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload("payloads/fod/10JavaDefects.zip");
        StaticScanActions.createStaticScan(staticScanDTO, app4, FodCustomTypes.SetupScanPageStatus.Queued);
        var name = getDefectInfo(bugID).get("name");
        var client = new OctaneRestClient();
        AllureReportUtil.info("Appending 'test' to issue name");
        String updateName = "{\"name\"" + ":\"" + name + "test\"}";
        client.putRequest(workspacePath + "defects/" + bugID, updateName);
        var applicationsPage = new TenantTopNavbar().openApplications();
        applicationsPage.openYourApplications().openDetailsFor(app4.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        var issuesPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(app4.getApplicationName())
                .openIssues();
        issuesPage.getShowFixedSuppressedDropdown()
                .setShowFixed(true);
        AllureReportUtil.info("Ensuring Status equals 'Fix Validated'");
        var issue = issuesPage.findWithSearchBox(issueID).openIssues().getIssues().get(0);
        var statusText = issue.getStatus();
        assertThat(statusText)
                .as("Status should have 'Fix Validated' text")
                .isEqualTo("Fix Validated");
        AllureReportUtil.info("Refreshing per reproduction steps in issue");
        refresh();
        assertThat(statusText)
                .as("Post refresh, Status text should be 'Fix Validated'")
                .isEqualTo("Fix Validated");
        assertThat(getDefectInfo(bugID).get("name").toString())
                .as("Showing updated name of issue is true")
                .contains("test");
        AllureReportUtil.info("Verify that the button label for submitted issue still says 'View Button'");
        verifyIssuesPageViewBugButton(app4);
    }

    @MaxRetryCount(2)
    @FodBacklogItem("799031")
    @Owner("kbadia@opentext.com")
    @Description("Verify purging a release with vulnerabilities submitted to bug tracker" +
            " does not deletes the bug tracker link on other releases")
    @Test(groups = {"regression", "hf"})
    public void verifyBugTrackerLinkOfReleasesAfterPurging() {
        AllureReportUtil.info("Create a new application and submit bugs to the bug tracker");
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        submitVulnerabilitiesToBugTracker(applicationDTO, false);
        AllureReportUtil.info("Create a copy state release from first release");
        var copyReleaseDTO = ReleaseDTO.createDefaultInstance();
        copyReleaseDTO.setCopyState(true);
        copyReleaseDTO.setCopyFromReleaseName(applicationDTO.getReleaseName());
        ReleaseActions.createRelease(applicationDTO, copyReleaseDTO);
        assertThat(isViewBugButtonDisplayed(applicationDTO.getApplicationName(), copyReleaseDTO.getReleaseName()))
                .as("The vulnerabilities submitted to the bug tracker should be showing " +
                        "as View bug for new Release which is copy stated from the first release")
                .isTrue();
        ReleaseActions.purgeRelease(applicationDTO, copyReleaseDTO);
        BrowserUtil.clearCookiesLogOff();
        ReleaseActions.purgeDeletedReleaseFromAdmin(defaultTenantDTO.getTenantName(), applicationDTO.getApplicationName(),
                copyReleaseDTO.getReleaseName(), true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(defaultTenantDTO);
        assertThat(isViewBugButtonDisplayed(applicationDTO.getApplicationName(), applicationDTO.getReleaseName()))
                .as("The vulnerabilities submitted to the bug tracker should be showing as View bug")
                .isTrue();
    }

    public boolean isViewBugButtonDisplayed(String appName, String releaseName) {
        return new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(appName, releaseName)
                .openIssues()
                .getIssues()
                .get(0)
                .isViewBugBtnEnabled();
    }

    public Pair<String, String> submitVulnerabilitiesToBugTracker(ApplicationDTO applicationDTO, boolean bugStateManagement) {
        AllureReportUtil.info("Configure and authenticate bug tracker tool");
        new TenantTopNavbar().openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openBugTrackerTab().enableBugTracker(true)
                .setBugTracker(octaneDropdownName)
                .setBugtrackerUrl(octaneUrl)
                .setBugtrackerLogin(octaneUser)
                .setBugtrackerPassword(octanePassword)
                .pressAuthenticate()
                .verifySortedDomains(String.valueOf(FodCustomTypes.BugTrackerType.OCTANE))
                .setBugtrackerToolDomain("ESP_646895359")
                .verifySortedComponent(String.valueOf(FodCustomTypes.BugTrackerType.OCTANE))
                .setBugtrackerToolComponent("Fortify Automation - 8H")
                .setBugStateManagement(bugStateManagement)
                .pressSave();

        AllureReportUtil.info("Complete static scan using fpr import");
        var issuesPage = StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/static.java.fpr")
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues()
                .groupBy("Category")
                .waitTillAllCountGreaterThanOrEqual(1);

        AllureReportUtil.info("Submit Vulnerabilities to bug tracker");
        var issueCell = issuesPage.getIssues().get(0);
        var issueID = issuesPage.getIssues().get(0).getId();
        var popup = issueCell.pressSubmitBug();
        var releases = popup.getAllReleases();
        var releasesSize = releases.size();
        popup.selectRelease(releases.get(releasesSize - 1));

        popup.pressSubmit();
        WaitUtil.waitFor(WaitUtil.Operator.Equals, true, issueCell::isViewBugBtnEnabled,
                Duration.ofMinutes(1), true);
        var bugId = issueCell.getBugUrl().split("&id=")[1];
        bugIdsToDelete.add(bugId);
        assertThat(issueCell.openDetails().openHistory().getAllEvents().size())
                .as("Verify history tab to confirm vulnerabilities are submitted successfully")
                .isGreaterThan(1);
        return Pair.of(bugId, issueID);
    }

    public void verifyIssuesPageViewBugButton(ApplicationDTO applicationDTO) {
        assertThat(new TenantTopNavbar().openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openIssues()
                .groupBy("Category")
                .getIssues()
                .get(0)
                .isViewBugBtnEnabled())
                .as("The vulnerabilities submitted to the bug tracker should be showing as View bug")
                .isTrue();
    }

    private JSONObject getDefectInfo(String bugId) {
        var client = new OctaneRestClient();
        var result = client.getRequest(workspacePath + "defects/" + bugId + "?fields=description,name")
                .body().peek();
        return new JSONObject(result.print());
    }

    @AfterClass
    void eraseDefects() {
        var client = new OctaneRestClient();
        var response = client.getRequest(workspacePath + "defects/" + "?fields=name");
        var id = response.body().jsonPath().getList("data.id");
        var name = response.body().jsonPath().getList("data.name");
        int size = id.size();
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(id.get(i).toString(), name.get(i).toString());
        }
        for (Map.Entry<String, String> set : map.entrySet()) {
            if (set.getValue().contains("FoD Security Vulnerability") && !bugIdsToDelete.contains(set.getKey())) {
                bugIdsToDelete.add(set.getKey());
            }
        }
        for (var bugId : bugIdsToDelete) {
            AllureReportUtil.info("Deleting defect " + bugId);
            client.deleteRequest(workspacePath + "defects/" + bugId);
        }
    }
}
