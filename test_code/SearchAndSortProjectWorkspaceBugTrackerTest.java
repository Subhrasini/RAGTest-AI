package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.config.FodConfig;

import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.tenant.applications.application.settings.BugTrackerPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;

import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import jdk.jfr.Description;
import lombok.extern.slf4j.Slf4j;

import org.testng.annotations.Test;

import utils.FodBacklogItem;
import utils.MaxRetryCount;

@Owner("ananda2@opentext.com")
@Slf4j
@FodBacklogItem("1724011")
public class SearchAndSortProjectWorkspaceBugTrackerTest extends FodBaseTest {
    ApplicationDTO applicationDTO;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create an application and release for overall validation of BugTracker settings")
    @Test()
    public void createTestApplication() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
    }
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"regression"},
            dataProvider = "bugTrackerTools",
            dependsOnMethods = {"createTestApplication"},
            dataProviderClass = FodUiTestDataProviders.class)
    @Description("Search and default sorting for Domain/Project and Component/Workspace dropdown for FoD bugtracker settings")
    public void searchAndSortDomainComponentBugTrackerIntegration(String bugTrackerType) {
        AllureReportUtil.info("Inside searchAndSortProjectWorkSpaceBugTrackerIntegration() for: " +bugTrackerType);
        LogInActions.tamUserLogin(defaultTenantDTO);
        new TenantTopNavbar()
                .openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openBugTrackerTab().enableBugTracker(true);

        String userId, userPasswd, bugTrackerURL;

        switch (bugTrackerType) {
            case "Atlassian JIRA":
                userId = FodConfig.JIRA_USER_NM;
                userPasswd = FodConfig.JIRA_PASSWD;
                bugTrackerURL = FodConfig.JIRA_URL;
                break;
            case "Bugzilla":
                userId = FodConfig.BUGZILLA_USER_NM;
                userPasswd = FodConfig.BUGZILLA_PASSWD;
                bugTrackerURL = FodConfig.BUGZILLA_URL;
                break;
            case "Azure DevOps":
                userId = FodConfig.AZURE_USER_NM;
                userPasswd = FodConfig.AZURE_ACCESS_TOKEN;
                bugTrackerURL = FodConfig.AZURE_URL;
                break;
            case "ValueEdge/ALM Octane":
                userId = FodConfig.OCTANE_CLIENT_ID;
                userPasswd = FodConfig.OCTANE_CLIENT_SECRET;
                bugTrackerURL = FodConfig.OCTANE_BASE_URI.split("/api")[0];
                break;
            default:
                throw new IllegalStateException("Unexpected Bug tracker type: " + bugTrackerType);
        }

        selectBugTrackerTypeValidateDomainAndComponent(bugTrackerType, bugTrackerURL, userId, userPasswd);
    }

    private void selectBugTrackerTypeValidateDomainAndComponent(String bugTrackerType, String bugTrackerURL, String userNm, String passwd) {
        AllureReportUtil.info("Inside selectBugTrackerTypeValidateDomainAndComponent() for: " +bugTrackerType);
        var bugTrackerPage = new BugTrackerPage();
        switch (bugTrackerType) {
            case "Atlassian JIRA":
                bugTrackerPage.setBugTracker(bugTrackerType)
                        .setBugtrackerUrl(bugTrackerURL).setBugtrackerLogin(userNm)
                        .setBugtrackerPassword(passwd).pressAuthenticate()
                        .verifySortedDomains(bugTrackerType)
                        .verifyInputDomainSuggestionsForJIRA("FODQA")
                        .selectValueFromSuggestions(" FODQA ")
                        .verifySortedComponent(bugTrackerType);
                break;
            case "Bugzilla":
                bugTrackerPage.setBugTracker(bugTrackerType)
                        .setBugtrackerUrl(bugTrackerURL).setBugtrackerLogin(userNm)
                        .setBugtrackerPassword(passwd).pressAuthenticate()
                        .verifySortedDomains(bugTrackerType)
                        .setBugtrackerToolDomain("TestProduct")
                        .verifySortedComponent(bugTrackerType);
                break;
            case "Azure DevOps":
                bugTrackerPage.setBugTracker(bugTrackerType)
                        .setBugtrackerUrl(bugTrackerURL).setBugtrackerLogin(userNm)
                        .setBugtrackerPassword(passwd).pressAuthenticate()
                        .verifySortedDomains(bugTrackerType);
                break;

            case "ValueEdge/ALM Octane":
                bugTrackerPage.setBugTracker(bugTrackerType)
                        .setBugtrackerUrl(bugTrackerURL).setBugtrackerLogin(userNm)
                        .setBugtrackerPassword(passwd).pressAuthenticate()
                        .verifySortedDomains(bugTrackerType)
                        .setBugtrackerToolDomain("ESP_646895359")
                        .verifySortedComponent(bugTrackerType);
                break;
            default:
                throw new IllegalStateException("Unexpected Bug tracker type: " + bugTrackerType + " provided for validation.");
        }
    }

}
