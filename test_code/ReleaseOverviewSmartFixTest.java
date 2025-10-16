package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.your_releases.YourReleasesPage;
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
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.WebDriverRunner.driver;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ReleaseOverviewSmartFixTest extends FodBaseTest {

    public String fileToUpload = "payloads/fod/static.java.fpr";
    public String errorNodeTitle = "InCall ResourceLeakExample.java:22";
    public String helpPopupTitle = "Smart Fix Basics";
    public ArrayList<String> categories = new ArrayList<>(Arrays
            .asList(
                    "Poor Error Handling: Return Inside Finally - 1",
                    "Unreleased Resource: Sockets - 3"));
    public ArrayList<String> smartFixTextNotes = new ArrayList<>(Arrays
            .asList(
                    "Drag anywhere to move around.",
                    "Scroll to zoom in to specific issues, or zoom out to get a birds eye view of the diagram.",
                    "Click on a box to highlight the shared paths flowing in and out of it.",
                    "Drill into an issue by clicking the first box in the trace.",
                    "Double click to zoom into an area",
                    "Heatmap highlights nodes with the most shared issue flows in red (>50%), orange (>30%) and yellow (>10%).",
                    "Select a node and click \"Prune\" to see the subtree of issues."));

    public ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to create application")
    @Test(groups = {"regression"})
    public void createApplication() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to upload .fpr scan from tenant site.")
    @Test(groups = {"regression"},
            dependsOnMethods = {"createApplication"})
    public void importScanTest() {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases();
        StaticScanActions.importScanTenant(applicationDTO, fileToUpload);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to use smart fix section.")
    @Test(groups = {"regression"},
            dependsOnMethods = {"importScanTest"})
    public void releaseOverviewSmartFixTest() {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases();
        var yourReleasesPage = page(YourReleasesPage.class);
        yourReleasesPage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());

        driver().getWebDriver().manage().window().maximize();

        var releaseDetailsPage = page(ReleaseDetailsPage.class);
        var smartFix = releaseDetailsPage.openSmartFixTab();

        var categoriesList = smartFix.getCategoriesOptions.texts();

        assertThat(categoriesList)
                .as("Validate that present categories is equal to expected.")
                .isEqualTo(categories);

        smartFix.selectCategory(categories.get(1));

        var nodes = smartFix.getAllNodes();
        long issueCount = nodes.stream().filter(node -> node.nodeTitle.contains("Issue")).count();

        var selectedIssueCount = smartFix.getSelectedIssueCount();
        assertThat(selectedIssueCount)
                .as("Validate that selected issues count by default = 0")
                .isEqualTo(0);
        assertThat(issueCount).as("Validate that issue count = 3")
                .isEqualTo(3);

        nodes.get(0).selectNode(true);
        selectedIssueCount = smartFix.getSelectedIssueCount();

        assertThat(nodes.get(0).nodeSelected()).as("Node should be selected").isTrue();
        assertThat(selectedIssueCount).as("Selected issue count should be: 1").isEqualTo(1);

        nodes = smartFix.getAllNodes();
        var secondRowNode = nodes.stream()
                .filter(x -> x.nodeTitle.contains(errorNodeTitle)).findAny().orElse(null);

        assertThat(secondRowNode).as("Node shouldn't be null").isNotNull();
        secondRowNode.selectNode(true);

        selectedIssueCount = smartFix.getSelectedIssueCount();
        var traces = smartFix.getAllTraces();
        var traceOne = traces.stream()
                .filter(x -> x.traceId.equals("link-0")).findAny().orElse(null);
        var traceTwo = traces.stream()
                .filter(x -> x.traceId.equals("link-1")).findAny().orElse(null);
        assertThat(traceOne).as("Trace 1 shouldn't be null").isNotNull();
        assertThat(traceTwo).as("Trace 2 shouldn't be null").isNotNull();

        var tracesHighlighted = traceOne.traceHighlighted && traceTwo.traceHighlighted;
        assertThat(selectedIssueCount).as("Selected issue count should be 2").isEqualTo(2);
        assertThat(tracesHighlighted).as("Trace should be highlighted").isTrue();

        smartFix.pressPruneButton();
        nodes = smartFix.getAllNodes();
        issueCount = nodes.stream().filter(node -> node.nodeTitle.contains("Issue")).count();
        selectedIssueCount = smartFix.getSelectedIssueCount();
        var redColoredNodesCount = nodes.stream()
                .filter(node -> node.nodeColor.equals("rgb(229, 0, 76)")).count();

        assertThat(issueCount).as("Issue count should be 2").isEqualTo(2);
        assertThat(selectedIssueCount).as("Selected issue count should be 0").isEqualTo(0);
        assertThat(redColoredNodesCount).as("Red colored nodes should be 2").isEqualTo(2);

        smartFix.pressToggleHeatMapButton();
        nodes = smartFix.getAllNodes();
        redColoredNodesCount = nodes.stream().filter(node -> node.nodeColor.equals("rgb(229, 0, 76)")).count();
        assertThat(redColoredNodesCount)
                .as("Red colored nodes should be 0 after press on [Toggle Heat Map Button]")
                .isEqualTo(0);

        smartFix.pressToggleHeatMapButton();
        nodes = smartFix.getAllNodes();
        redColoredNodesCount = nodes.stream().filter(node -> node.nodeColor.equals("rgb(229, 0, 76)")).count();
        assertThat(redColoredNodesCount)
                .as("Red colored nodes should be 2 after press on [Toggle Heat Map]")
                .isEqualTo(2);

        smartFix.pressResetButton();
        nodes = smartFix.getAllNodes();
        issueCount = nodes.stream().filter(node -> node.nodeTitle.contains("Issue")).count();

        assertThat(issueCount)
                .as("Issue count should be 3 after press on [Reset]")
                .isEqualTo(3);

        var defaultSize = smartFix.nodesBlockElement.getSize();
        BrowserUtil.zoomElement(smartFix.nodesBlockElement, -500);

        var zoomIn = smartFix.nodesBlockElement.getSize();

        assertThat(defaultSize.getHeight()).as("Size should be changed after scrolling wheel")
                .isLessThan(zoomIn.getHeight());
        assertThat(defaultSize.getHeight()).as("Size should be changed after scrolling wheel")
                .isLessThan(zoomIn.getWidth());

        smartFix.pressZoomToFitButton();
        var zoomToFitSize = smartFix.nodesBlockElement.getSize();

        assertThat(defaultSize)
                .as("Size should be restored after press on [Zoom To Fit]")
                .isEqualTo(zoomToFitSize);

        BrowserUtil.zoomElement(smartFix.nodesBlockElement, 1000);
        var zoomOut = smartFix.nodesBlockElement.getSize();

        assertThat(defaultSize.getHeight()).as("Size should be changed after scrolling wheel")
                .isGreaterThan(zoomOut.getHeight());
        assertThat(defaultSize.getWidth()).as("Size should be changed after scrolling wheel")
                .isGreaterThan(zoomOut.getWidth());

        smartFix.pressZoomToFitButton();
        zoomToFitSize = smartFix.nodesBlockElement.getSize();

        assertThat(zoomToFitSize).as("Size should be restored after press on [Zoom To Fit]")
                .isEqualTo(defaultSize);

        var defaultWindowSize = smartFix.getGetSmartFixWindowElement().getSize();
        smartFix.pressFullScreenButton();
        var fullScreenWindowSize = smartFix.getGetSmartFixWindowElement().getSize();

        assertThat(defaultWindowSize.getWidth())
                .as("Size should be changed after press on [Full Screen]")
                .isLessThan(fullScreenWindowSize.getWidth());
        assertThat(defaultWindowSize.getHeight())
                .as("Size should be changed after press on [Full Screen]")
                .isLessThan(fullScreenWindowSize.getHeight());

        smartFix.pressCloseButton();
        assertThat(defaultWindowSize)
                .as("Size should be restored after press on [Close]")
                .isEqualTo(smartFix.getGetSmartFixWindowElement().getSize());

        var helpPopup = smartFix.pressHelpButton();

        assertThat(helpPopup.popupElement.isDisplayed())
                .as("Validate that help popup is opened").isTrue();
        assertThat(helpPopup.getTitle())
                .as("Validate that popup title is equal: " + helpPopupTitle)
                .isEqualTo(helpPopupTitle);
        assertThat(helpPopup.getNotesList())
                .as("Validate smart fix notes")
                .isEqualTo(smartFixTextNotes);

        helpPopup.pressClose();
    }
}


