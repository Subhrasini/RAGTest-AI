package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.CollectionCondition;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.custom_types.dashboard_tiles.TileData;
import com.fortify.fod.common.custom_types.dashboard_tiles.TileFilter;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.exceptions.FodUnexpectedConditionsException;
import com.fortify.fod.ui.pages.Page404;
import com.fortify.fod.ui.pages.tenant.dashboard.MyDashboardPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class DefaultDashboardsTest extends FodBaseTest {

    String tamUserName;
    TenantUserDTO secLead;
    TenantUserDTO developer;
    TenantUserDTO leadDeveloper;
    TenantUserDTO reviewer;
    TenantUserDTO executive;
    TenantUserDTO appLead;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Test(description = "Create all necessary users", groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        tamUserName = tenantDTO.getAssignedUser();

        secLead = TenantUserDTO.createDefaultInstance();
        secLead.setUserName(tenantDTO.getUserName());

        developer = TenantUserDTO.createDefaultInstance();
        developer.setRole(FodCustomTypes.TenantUserRole.Developer);
        developer.setUserName(developer.getUserName() + "-DEVELOPER");
        developer.setTenant(tenantDTO.getTenantCode());

        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);
        leadDeveloper.setUserName(leadDeveloper.getUserName() + "-LEADDEV");
        leadDeveloper.setTenant(tenantDTO.getTenantCode());

        reviewer = TenantUserDTO.createDefaultInstance();
        reviewer.setRole(FodCustomTypes.TenantUserRole.Reviewer);
        reviewer.setUserName(reviewer.getUserName() + "-REVIEWER");
        reviewer.setTenant(tenantDTO.getTenantCode());

        executive = TenantUserDTO.createDefaultInstance();
        executive.setRole(FodCustomTypes.TenantUserRole.Executive);
        executive.setUserName(executive.getUserName() + "-EXEC");
        executive.setTenant(tenantDTO.getTenantCode());

        appLead = TenantUserDTO.createDefaultInstance();
        appLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        appLead.setUserName(appLead.getUserName() + "-APPLEAD");
        appLead.setTenant(tenantDTO.getTenantCode());

        TenantUserActions.createTenantUsers(tenantDTO, developer, leadDeveloper, reviewer, executive, appLead);
    }

    @Parameters({"User Name"})
    @MaxRetryCount(5)
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            dataProvider = "tenantUserRolesStrings", dataProviderClass = FodUiTestDataProviders.class)
    public void defaultDashboardsTest(String role) {
        setTestCaseName("Default Dashboards for " + role);
        var dashboard = getTileByUserRole(role);

        switch (role) {
            case "TAM": {
                checkDashboardsForRole(dashboard, tamUserName, true);
                break;
            }
            case "Security Lead": {
                checkDashboardsForRole(dashboard, secLead.getUserName(), false);
                break;
            }
            case "Reviewer": {
                checkDashboardsForRole(dashboard, reviewer.getUserName(), false);
                break;
            }
            case "Executive": {
                checkDashboardsForRole(dashboard, executive.getUserName(), false);
                break;
            }
            case "Developer": {
                checkDashboardsForRole(dashboard, developer.getUserName(), false);
                break;
            }
            case "Lead Developer": {
                checkDashboardsForRole(dashboard, leadDeveloper.getUserName(), false);
                break;
            }
            case "Application Lead": {
                checkDashboardsForRole(dashboard, appLead.getUserName(), false);
                break;
            }
        }
    }

    private void checkDashboardsForRole(TileData[][] dashboard, String username, boolean tamLogin) {

        MyDashboardPage page;

        log.info("Verifying dashboard for {}", username);
        if (tamLogin) {
            page = LogInActions.tamUserLogin(
                    username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode()).tenantTopNavbar.openDashboard();
        } else {
            page = LogInActions.tenantUserLogIn(
                    username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode()).tenantTopNavbar.openDashboard();
        }

        new Page404().waitTill404Disappear();

        if (page.rows.size() == 0) {
            Supplier sup = () -> {
                page.spinner.waitTillLoading(2, true);
                new Page404().waitTill404Disappear();

                if ($(byText("We can't process your request.")).exists()) {
                    Supplier<Boolean> page404 = () -> {
                        refresh();
                        return $(byText("We can't process your request.")).exists();
                    };
                    WaitUtil.waitFor(WaitUtil.Operator.Equals,
                            false, page404, Duration.ofMinutes(2), true);
                }
                page.clickActions();
                sleep(1000);
                page.clickEdit();
                return page.resetToDefaultBtn.isEnabled() && page.resetToDefaultBtn.isDisplayed();
            };

            WaitUtil.waitFor(WaitUtil.Operator.Equals, true, sup, Duration.ofMinutes(1), true);
            page.pressResetToDefault().pressYes();
            page.tenantTopNavbar.openDashboard();
        }

        page.rows.should(CollectionCondition.sizeGreaterThan(0));

        for (int i = 0; i < page.rows.size(); i++) {
            var rowCells = page.getCellsByRow(i);
            for (int j = 0; j < rowCells.size(); j++) {
                log.debug(dashboard[i][j].getTileName());
                assertThat(rowCells.get(j).getName()).as("Check if tile name {} equals expected")
                        .isEqualToIgnoringCase(dashboard[i][j].getTileName());
            }
        }

        page.clickActions().clickEdit();

        for (int i = 0; i < page.rows.size(); i++) {
            var rowCells = page.getCellsByRow(i);
            for (int j = 0; j < rowCells.size(); j++) {
                var expectedTile = dashboard[i][j];
                var actualTile = rowCells.get(j);
                log.debug(expectedTile.getTileName());
                assertThat(actualTile.getSelectedTileType()).as("Check if 'Tile type' equals expected")
                        .isEqualToIgnoringCase(expectedTile.getTileType());

                if (expectedTile.getType() != null) {
                    assertThat(actualTile.getSelectedType()).as("Check if dropdown 'Type' equals expected")
                            .isEqualToIgnoringCase(expectedTile.getType());
                }

                if (expectedTile.getDataType() != null) {
                    assertThat(actualTile.getDataType()).as("Check if dropdown 'Data type' equals expected")
                            .isEqualToIgnoringCase(expectedTile.getDataType());
                }

                if (expectedTile.getResolution() != null) {
                    assertThat(actualTile.getResolution()).as("Check if dropdown 'Resolution' equals expected")
                            .isEqualToIgnoringCase(expectedTile.getResolution());
                }

                if (expectedTile.getGroupBy() != null) {
                    assertThat(actualTile.getGroupBy()).as("Check if dropdown 'Group By' equals expected")
                            .isEqualToIgnoringCase(expectedTile.getGroupBy());
                }

                if (expectedTile.getEventType() != null) {
                    assertThat(actualTile.getEvent()).as("Check if dropdown 'Event' equals expected")
                            .isEqualToIgnoringCase(expectedTile.getEventType());
                }

                if (expectedTile.getTileFilters() != null && expectedTile.getTileFilters().length > 0) {
                    actualTile.expandFilters();
                    for (var filter : List.of(expectedTile.getTileFilters())) {
                        var checkbox = actualTile.getCheckbox(filter.filterCategory, filter.filterName);
                        assertThat(checkbox.checked()).as("Check if filter checkbox is checked").isEqualTo(true);
                    }
                }
            }
        }
    }

    public TileData[][] getTileByUserRole(String role) {
        String last30Days = "Last 30 Days";
        String gauge = "Gauge";
        String listGrid = "List Grid";
        String summary = "Summary";
        String trendingChart = "Trending Chart";

        String scans = "Scans";
        String apps = "Applications";
        String releases = "Releases";

        String portfolio = "Portfolio";
        String entConsumption = "Entitlement Consumption";
        String mostPrevalentIssues = "Most Prevalent Issues";
        String pausedScans = "Paused Scans";
        String cancelledScans = "Canceled Scans";
        String completedScans = "Completed Scans";
        String myIssues = "My Issues";
        String issueAssignments = "Issue Assignment";
        String auditorStatus = "Auditor Status";
        String developerStatus = "Developer Status";

        String isPassed = "Is Passed";
        String appType = "Application Type";
        String scanType = "Scan Type";

        String isCompleted = "Is Completed";

        String sdlc = "SDLC Status";
        String issueStatus = "Issue Status";
        String isSuppressed = "Is Suppressed";

        var SDLCProd = new TileFilter(sdlc, "Production");
        var SDLCQATest = new TileFilter(sdlc, "QA/Test");
        var SDLCDev = new TileFilter(sdlc, "Development");


        var issueStatusNew = new TileFilter(issueStatus, "New");
        var issueStatusExisting = new TileFilter(issueStatus, "Existing");
        var issueStatusReopen = new TileFilter(issueStatus, "Reopen");
        var isSuppressedNo = new TileFilter(isSuppressed, "No");

        var standardFilterList = new TileFilter[]{
                SDLCProd,
                SDLCQATest,
                SDLCDev,
                issueStatusNew,
                issueStatusExisting,
                issueStatusReopen,
                isSuppressedNo};

        var portfolioTile = new TileData("Portfolio Summary", summary,
                portfolio, null);

        var entitlementConsumptionTile = new TileData(entConsumption,
                gauge, entConsumption,
                null);

        var scansCompletedTrendTile = new TileData("Scans Completed Trend",
                trendingChart, scans,
                last30Days,
                scanType,
                isCompleted, null);

        var appPortfolioTrendTile = new TileData("Application Portfolio Trend",
                trendingChart,
                apps,
                last30Days,
                appType,
                null, null);

        var secPolicyTile = new TileData("Security Policy Compliance of Production Releases",
                trendingChart,
                releases,
                last30Days,
                isPassed,
                null,
                new TileFilter[]{SDLCProd});

        var completedScansTile = new TileData(completedScans,
                listGrid, completedScans,
                null);

        var cancelledScansTile = new TileData(cancelledScans,
                listGrid, cancelledScans,
                null);

        var pausedScansTile = new TileData(pausedScans,
                listGrid, pausedScans,
                null);

        var mostPrevalentIssuesTile = new TileData(mostPrevalentIssues,
                listGrid,
                mostPrevalentIssues, null);

        var issueAssignmentTile = new TileData(issueAssignments,
                gauge,
                issueAssignments, standardFilterList);

        var devStatusTile = new TileData(developerStatus,
                gauge,
                developerStatus, standardFilterList);

        var auditorStatusTile = new TileData(auditorStatus,
                gauge,
                auditorStatus,
                new TileFilter[]
                        {
                                SDLCProd,
                                SDLCQATest,
                                SDLCDev,
                                issueStatusNew,
                                issueStatusExisting,
                                issueStatusReopen
                        });

        var myIssuesTile = new TileData(myIssues,
                listGrid, myIssues, null);

        // Create tile sets (dashboards) consisting of different tiles

        var tamSecLeadDashboard = new TileData[][]{
                {portfolioTile, entitlementConsumptionTile},
                {scansCompletedTrendTile, appPortfolioTrendTile},
                {secPolicyTile},
                {completedScansTile, mostPrevalentIssuesTile}
        };

        var execAndReviewerDashboard = new TileData[][]{
                {portfolioTile, entitlementConsumptionTile},
                {scansCompletedTrendTile, appPortfolioTrendTile},
                {secPolicyTile},
                {mostPrevalentIssuesTile}
        };

        var leadDevAppLeadDashboard = new TileData[][]{
                {issueAssignmentTile, devStatusTile, auditorStatusTile},
                {myIssuesTile, mostPrevalentIssuesTile},
                {pausedScansTile, cancelledScansTile}
        };

        switch (role) {
            case "Application Lead":
            case "Lead Developer":
            case "Developer":
                return leadDevAppLeadDashboard;
            case "Security Lead":
            case "TAM":
                return tamSecLeadDashboard;
            case "Executive":
            case "Reviewer":
                return execAndReviewerDashboard;

            default:
                throw new FodUnexpectedConditionsException("No Such Role as: " + role);
        }
    }
}
