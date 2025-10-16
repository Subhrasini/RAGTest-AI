package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.CollectionCondition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
public class EditDashboardChartsTest extends FodBaseTest {
    String newDashboardName = "Chart type testing";
    String summary = "Summary";
    String gauge = "Gauge";
    String listGrid = "List Grid";
    String trendingChart = "Trending Chart";
    String secPolicyTile = "Security Policy Compliance of Production Releases";
    String entitlementsDataType = "Entitlements";
    String releasesDataType = "Releases";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("777006")
    @Description("Verify Removal of Group By field for trending charts while selecting Entitlements as Data type")
    @Test(groups = {"regression"})
    public void verifyRemovalOfGroupByFieldInTrendingChart() {
        var dashboardPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openDashboard();
        if (!dashboardPage.getAvailableDashboards().contains(newDashboardName)) {
            dashboardPage.createNewDashboard(newDashboardName).selectDashboard(newDashboardName)
                    .clickActions().clickEdit().pressResetToDefault().pressYes();
            assertThat(dashboardPage.rows).as("Rows should be greater than 0 ").isNotEmpty();
        }

        AllureReportUtil.info("Verify trending chart ,group By field is removed if datatype is entitlements");
        var dashboardCell = dashboardPage.selectDashboard(newDashboardName).clickActions().clickEdit()
                .getCellsByRow(2).get(0);
        assertThat(dashboardCell.getSelectedTileType())
                .as("Tile type should be trending chart")
                .isEqualTo(trendingChart);
        dashboardCell.setDataType(releasesDataType);
        assertThat(dashboardCell.getDataType())
                .as("Data Type should be updated as Releases")
                .isEqualTo(releasesDataType);
        assertThat(dashboardCell.isGroupByVisible())
                .as("If datatype is not entitlements Group By Dropdown should be visible")
                .isTrue();
        dashboardCell.setDataType(entitlementsDataType);
        assertThat(dashboardCell.getDataType())
                .as("Data Type should be updated as entitlements")
                .isEqualTo(entitlementsDataType);
        assertThat(dashboardCell.isGroupByVisible())
                .as("If datatype is entitlements Group By Dropdown should not be visible")
                .isFalse();
        dashboardCell.clickSave();
        AllureReportUtil.info("Verify display of the trending chart tile has not been diminished");
        for (int rows = 0; rows < dashboardPage.rows.size(); rows++) {
            var rowCells = dashboardPage.getCellsByRow(rows);
            for (int columns = 0; columns < rowCells.size(); columns++) {
                if (rowCells.get(columns).getName().equalsIgnoreCase(secPolicyTile)) {
                    assertThat(rowCells)
                            .as("Display of the Security policy tile should be present")
                            .isNotEmpty();
                }
            }
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create dashboard and edit dashboard charts in tenant site")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyRemovalOfGroupByFieldInTrendingChart"})
    public void validateDashboardCharts() {
        var dashboardPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openDashboard();
        AllureReportUtil.info("Validate the  chart types in New Dashboard before editing");
        verifyDashboardTiles(newDashboardName, summary, gauge);

        AllureReportUtil.info("Edit the chart types in New Dashboard");
        var tiles = dashboardPage.selectDashboard(newDashboardName).clickActions().clickEdit().getCellsByRow(0);
        tiles.get(0).setTileType(listGrid);
        BrowserUtil.waitAjaxLoaded();
        tiles.get(1).setTileType(trendingChart);
        BrowserUtil.waitAjaxLoaded();
        dashboardPage.clickSave();

        AllureReportUtil.info("Validate the  chart types in new dashboard After editing");
        verifyDashboardTiles(newDashboardName, listGrid, trendingChart);

        AllureReportUtil.info("Delete the newly created Dashboard after the test");
        dashboardPage.selectDashboard(newDashboardName).clickActions().clickEdit().pressDelete().pressYes();
        assertThat(dashboardPage.getAvailableDashboards())
                .as("Newly created dashboard should be deleted and shouldn't present in the dropdown")
                .doesNotContain(newDashboardName);
    }

    public void verifyDashboardTiles(String dashboardName, String chartType1, String chartType2) {
        var myDashboardPage = new TenantTopNavbar().openDashboard().selectDashboard(dashboardName)
                .clickActions().clickEdit();
        myDashboardPage.rows.should(CollectionCondition.sizeGreaterThan(0));
        for (int rows = 0; rows < 1; rows++) {
            var rowCells = myDashboardPage.getCellsByRow(rows);
            for (int columns = 0; columns < 2; columns++) {
                assertThat(rowCells.get(0).getSelectedTileType())
                        .as("Validate chart type of first row first cell")
                        .isEqualToIgnoringCase(chartType1);
                assertThat(rowCells.get(1).getSelectedTileType())
                        .as("Validate chart type of first row second cell")
                        .isEqualToIgnoringCase(chartType2);
            }
            myDashboardPage.clickSave();
        }
    }
}
