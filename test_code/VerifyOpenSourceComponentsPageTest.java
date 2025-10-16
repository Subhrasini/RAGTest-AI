package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.common.admin.cells.AdminIssueCell;
import com.fortify.fod.ui.pages.common.tenant.cells.TenantIssueCell;
import com.fortify.fod.ui.pages.tenant.applications.OpenSourceComponentCell;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.your_releases.YourReleasesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.page;
import static com.fortify.fod.ui.pages.common.common.cells.issue_details_cells.DependenciesCell.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("282021")
@FodBacklogItem("725031")
public class VerifyOpenSourceComponentsPageTest extends FodBaseTest {
    ApplicationDTO applicationDTO, debrickedApplicationDTO;
    StaticScanDTO staticScanOpenSourceDTO;
    ReleaseDTO openSourceReleaseDTO, openSourceRelease2DTO;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Description("TAM should be able to import scan that validates no open source components page")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);

        applicationDTO = ApplicationDTO.createDefaultInstance();
        debrickedApplicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        ApplicationActions.createApplication(debrickedApplicationDTO, tenantDTO, false);
        StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/chat_application_via_lan.fpr");
    }

    @MaxRetryCount(1)
    @Description("Admin should create Sonatype Entitlements, AUTO-TAM should create open source static scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"noOpenSourceEntitlementsApplicationTest"})
    public void prepareTestDataOpenSourceSonatype() {
        var sonaTypeEntitlement = EntitlementDTO.createDefaultInstance();
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        sonaTypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        EntitlementsActions.createEntitlements(sonaTypeEntitlement);
        BrowserUtil.clearCookiesLogOff();

        openSourceReleaseDTO = ReleaseDTO.createDefaultInstance();
        staticScanOpenSourceDTO = StaticScanDTO.createDefaultInstance();
        staticScanOpenSourceDTO.setLanguageLevel("10");
        staticScanOpenSourceDTO.setOpenSourceComponent(true);
        staticScanOpenSourceDTO.setFileToUpload("payloads/fod/Sonatype vulns with yellow banner_CUT.zip");
        openSourceReleaseDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .createNewRelease(openSourceReleaseDTO);
        StaticScanActions.createStaticScan(staticScanOpenSourceDTO, applicationDTO, openSourceReleaseDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), openSourceReleaseDTO.getReleaseName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(1)
    @Description("Admin should create Debricked Entitlements, AUTO-TAM should create open source static scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"noOpenSourceEntitlementsApplicationTest", "prepareTestDataOpenSourceSonatype"})
    public void prepareTestDataOpenSourceDebricked() {

        var entitlementsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        entitlementsPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getAll().get(0)
                .pressEdit()
                .disableEntitlement();

        BrowserUtil.clearCookiesLogOff();
        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        EntitlementsActions.createEntitlements(debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(debrickedApplicationDTO.getApplicationName(), debrickedApplicationDTO.getReleaseName())
                .openScans()
                .pressImportOpenSourceScan()
                .uploadFile("payloads/fod/yarn_cyclonedx.json")
                .pressImportButton()
                .openOpenSourceComponents();
        BrowserUtil.clearCookiesLogOff();

        openSourceRelease2DTO = ReleaseDTO.createDefaultInstance();
        staticScanOpenSourceDTO = StaticScanDTO.createDefaultInstance();
        staticScanOpenSourceDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScanOpenSourceDTO.setOpenSourceComponent(true);
        staticScanOpenSourceDTO.setFileToUpload("payloads/fod/NodeGoat-main.zip");
        openSourceRelease2DTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openDetailsFor(debrickedApplicationDTO.getApplicationName())
                .createNewRelease(openSourceRelease2DTO);
        StaticScanActions.createStaticScan(staticScanOpenSourceDTO, debrickedApplicationDTO, openSourceRelease2DTO, FodCustomTypes.SetupScanPageStatus.Completed);

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(debrickedApplicationDTO, openSourceRelease2DTO)
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(3)
    @Description("AUTO-TAM should see that there is no Open Source Component link on Release Details page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void noOpenSourceEntitlementsReleaseTest() {
        SoftAssertions softAssert = new SoftAssertions();
        var releaseDetailsPage = LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());
        softAssert.assertThat(releaseDetailsPage.sideNavTabExists(TenantSideNavTabs.ReleaseDetails.OpenSourceComponents))
                .as("There should not be a link to Open Source Components page from Release Details")
                .isTrue();
        softAssert.assertThat(releaseDetailsPage.getTabByName("Sonatype").isDisplayed())
                .as("There should not be a Sonatype tab visible")
                .isFalse();
        softAssert.assertThat(releaseDetailsPage.getTabByName("Debricked").isDisplayed())
                .as("There should not be a Debricked tab visible")
                .isFalse();
        softAssert.assertAll();
    }

    @MaxRetryCount(3)
    @Description("AUTO-TAM should validate that Open Source Component link on Your Applications page leads to page with empty table")
    @Test(groups = {"regression"}, dependsOnMethods = {"noOpenSourceEntitlementsReleaseTest"})
    public void noOpenSourceEntitlementsApplicationTest() {
        SoftAssertions softAssert = new SoftAssertions();
        var yourApplicationsPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications();
        softAssert.assertThat(yourApplicationsPage.sideNavTabExists(TenantSideNavTabs.Applications.OpenSourceComponents))
                .as("Link to Open Source Components page should be visible")
                .isTrue();
        var checkOpenSourcePage = yourApplicationsPage.openOpenSourceComponents();
        softAssert.assertThat(checkOpenSourcePage.componentsIdentified.exists())
                .as("Components Identified section should not be visible")
                .isFalse();
        softAssert.assertThat(checkOpenSourcePage.securityIssues.exists())
                .as("Security Issues section should not be visible")
                .isFalse();
        softAssert.assertAll();
    }

    @MaxRetryCount(3)
    @Description("AUTO-TAM should validate navigation to Open Source Components page from Release Details page and listed page elements exist and don't exist")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceSonatype", "prepareTestDataOpenSourceDebricked"},
            dataProvider = "entitlementType", dataProviderClass = VerifyOpenSourceComponentsPageTest.class)
    public void openSourceEntitlementsReleaseTest(FodCustomTypes.EntitlementType entitlementType) {
        SoftAssertions softAssert = new SoftAssertions();

        var releaseDetailsPage = choosePath(entitlementType);
        String openSourceType;
        if (entitlementType == FodCustomTypes.EntitlementType.SonatypeEntitlement) {
            openSourceType = "sonatype";
        } else {
            openSourceType = "debricked";
        }

        softAssert.assertThat(releaseDetailsPage.sideNavTabExists(TenantSideNavTabs.ReleaseDetails.OpenSourceComponents)).isTrue();
        softAssert.assertThat(releaseDetailsPage.getTabByName(openSourceType).isDisplayed()).isFalse();
        var checkOpenSourcePage = releaseDetailsPage.openOpenSourceComponents();
        var openSourceTable = checkOpenSourcePage.getTable();
        softAssert.assertThat(checkOpenSourcePage.getNumberComponentsIdentifiedCount())
                .as("Components Identified section should exist")
                .isGreaterThanOrEqualTo(0);
        softAssert.assertThat(checkOpenSourcePage.getNumberSecurityIssuesCount())
                .as("Security Issues section should exist")
                .isGreaterThanOrEqualTo(0);
        softAssert.assertThat(openSourceTable.rows.size())
                .as("Table should have a size greater than 0")
                .isPositive();
        softAssert.assertThat(openSourceTable
                        .getAllColumnValues(openSourceTable.getColumnIndex("Scan Tool"))
                        .stream().map(String::toLowerCase).collect(Collectors.toList()))
                .as(openSourceType + " should be displayed under Scan Tool column")
                .contains(openSourceType.toLowerCase());
        softAssert.assertThat(checkOpenSourcePage.getNumberComponentsIdentifiedCount())
                .as("Components Identified section should exist")
                .isGreaterThanOrEqualTo(0);
        softAssert.assertThat(checkOpenSourcePage.getNumberSecurityIssuesCount())
                .as("Security Issues section should exist")
                .isGreaterThanOrEqualTo(0);
        softAssert.assertThat(openSourceTable.rows.size())
                .as("Table should have a size greater than 0")
                .isPositive();
        softAssert.assertAll();
    }

    @MaxRetryCount(3)
    @Description("AUTO-TAM should validate navigation from Your Applications to Open Source Components page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceSonatype", "prepareTestDataOpenSourceDebricked"})
    public void openSourceEntitlementsApplicationsTest() {
        SoftAssertions softAssert = new SoftAssertions();
        var yourApplicationsPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications();
        softAssert.assertThat(yourApplicationsPage.sideNavTabExists(TenantSideNavTabs.Applications.OpenSourceComponents)).isTrue();
        var checkOpenSourcePage = yourApplicationsPage.openOpenSourceComponents();
        WaitUtil.waitForTrue(() -> checkOpenSourcePage.componentsIdentified.exists(), Duration.ofMinutes(1), true);
        softAssert.assertThat(checkOpenSourcePage.componentsIdentified.exists())
                .as("Components Identified section not exist")
                .isFalse();
        softAssert.assertThat(checkOpenSourcePage.securityIssues.exists())
                .as("Security Issues section should not exist")
                .isFalse();
        softAssert.assertThat(checkOpenSourcePage.getTable().rows.size())
                .as("Table should have a size greater than 0")
                .isPositive();

        checkOpenSourcePage.filters.openFilterContainer();
        List<String> filtersName = checkOpenSourcePage.filters.getAllFilters();
        if (filtersName.contains("Scan Tool")) {
            checkOpenSourcePage.filters.expandAllFilters();
            checkOpenSourcePage.filters.setFilterByName("Scan Tool").clickFilterOptionByName("Debricked");
            var table = checkOpenSourcePage.getTable();
            var tableItems = table.getAllColumnValues(table.getColumnIndex("Scan Tool"));
            softAssert.assertThat(tableItems)
                    .as("Table should contain Debricked type scans but not Sonatype")
                    .contains("Debricked").doesNotContain("sonatype");
            checkOpenSourcePage.appliedFilters.clearAll();

            checkOpenSourcePage.filters.openFilterContainer();
            checkOpenSourcePage.filters.expandAllFilters();
            checkOpenSourcePage.filters.setFilterByName("Scan Tool").clickFilterOptionByName("sonatype");
            softAssert.assertThat(checkOpenSourcePage.getTable().getAllColumnValues(checkOpenSourcePage.getTable().getColumnIndex("Scan Tool")))
                    .as("Table should contain Sonatype scan data but nothing related to Debricked type scan")
                    .contains("sonatype").doesNotContain("Debricked");

        }
        softAssert.assertAll();
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("763008")
    @MaxRetryCount(3)
    @Description("AUTO-TAM should validate navigation from Your Applications to Open Source Components page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceDebricked"})
    public void updatedAndPublishedDayVulnerabilityTabTest() {
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .getReleaseByAppAndReleaseNames(debrickedApplicationDTO.getApplicationName(), debrickedApplicationDTO.getReleaseName())
                .openDetailsForRelease(debrickedApplicationDTO.getApplicationName(), debrickedApplicationDTO.getReleaseName())
                .openScans()
                .openIssues();
        var issuesPage = page(ReleaseIssuesPage.class);
        var issue = issuesPage.getIssues().get(0);
        var vulnerabilityCell = issue.openDetails().openVulnerability();
        String vulnerabilityTabOutput = vulnerabilityCell.getVisibleSection().text();

        String patternPublishedDate1 = ".*?(Published Date.*?\\d{4}.\\d{2}.\\d{2})";
        String patternPublishedDate2 = ".*?(Published Date.*?\\d{2}.\\d{2}.\\d{4})";

        Pattern regexPatternPublishedDate1 = Pattern.compile(patternPublishedDate1);
        Pattern regexPatternPublishedDate2 = Pattern.compile(patternPublishedDate2);

        Matcher matchPublicDate1 = regexPatternPublishedDate1.matcher(vulnerabilityTabOutput);
        Matcher matchPublicDate2 = regexPatternPublishedDate2.matcher(vulnerabilityTabOutput);

        assertThat(matchPublicDate1.find() || matchPublicDate2.find())
                .as("Ex: pattern of Published Date:dddd/dd/dd (or) dd/dd/dddd should exist")
                .isTrue();

        String patternUpdated1 = ".*?(Updated.*?\\d{4}.\\d{2}.\\d{2})";
        String patternUpdated2 = ".*?(Updated.*?\\d{2}.\\d{2}.\\d{4})";

        Pattern regexPatternUpdated1 = Pattern.compile(patternUpdated1);
        Pattern regexPatternUpdated2 = Pattern.compile(patternUpdated2);

        Matcher matchUpdated1 = regexPatternUpdated1.matcher(vulnerabilityTabOutput);
        Matcher matchUpdated2 = regexPatternUpdated2.matcher(vulnerabilityTabOutput);

        assertThat(matchUpdated1.find() || matchUpdated2.find())
                .as("Ex: pattern of Updated:dddd/dd/dd (or) dd/dd/dddd should exist")
                .isTrue();
    }


    @Owner("tmagill@opentext.com")
    @FodBacklogItem("800056")
    @Severity(SeverityLevel.CRITICAL)
    @MaxRetryCount(1)
    @Description("AUTO-TAM should check Issue Page and validate there is only 1 event in the History tab per issue")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceSonatype", "prepareTestDataOpenSourceDebricked"}
            , dataProvider = "entitlementType", dataProviderClass = VerifyOpenSourceComponentsPageTest.class)
    public void verifyIssueHistoryLogTest(FodCustomTypes.EntitlementType entitlementType) {
        SoftAssertions softAssert = new SoftAssertions();
        var releaseDetailsPage = choosePath(entitlementType);
        var listOfHighIssues = releaseDetailsPage
                .openIssues()
                .groupBy("Status")
                .selectIssuesGroup("New")
                .getCriticalIssues();
        listOfHighIssues
                .forEach(issue -> {
                    var count = issue.openDetails().openHistory().getAllEvents();
                    softAssert.assertThat(count.size())
                            .as("There should only be 1 event in the History at this time")
                            .isEqualTo(1);
                });
        softAssert.assertAll();
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("800056")
    @MaxRetryCount(1)
    @Description("Checks for issue dependecies relative to Debricked and Debricked(Imported). Sonatype should not have dependencies")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceSonatype", "prepareTestDataOpenSourceDebricked"},
            dataProvider = "dependencyScanType")
    public void verifyDependencyTenantTest(String scanType) {
        SoftAssertions softAssert = new SoftAssertions();
        var detailsReleasePage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        switch (scanType) {
            case "Debricked(Imported)" -> {
                var listOfTenantIssues = getListOfIssuesTenant(scanType, debrickedApplicationDTO.getApplicationName(),
                        debrickedApplicationDTO.getReleaseName(), detailsReleasePage);
                listOfTenantIssues
                        .forEach(issue -> {
                            AllureReportUtil.info("Dependency Tab exists");
                            assertThat(issue.openDetails().getAllTabsNames().toString())
                                    .as("If dependency tab does not exist, immediate failure")
                                    .contains("Dependencies");
                            var dependencies = issue.openDetails().openDependencies().listOfDependencies;
                            if (dependencies.size() == 0) {
                                softAssert.assertThat(noDependency.isDisplayed())
                                        .as("If there is no dependency , message should be visible to the fact")
                                        .isTrue();
                            } else {
                                dependencies.forEach(dependency -> {
                                    AllureReportUtil.info("Checking for chevron down facing with %s"
                                            .formatted(dependency.$(".tree-item ").getText()));
                                    softAssert.assertThat(dependencies.size())
                                            .as("Asserts the existence of dependencies for issue")
                                            .isGreaterThan(0);
                                    if (canExpandDependency.exists()) {
                                        canExpandDependency.click();
                                    }
                                    softAssert.assertThat(collapseDependency.isDisplayed())
                                            .as("If the dependency is expanded, controls should exist to collapse it")
                                            .isTrue();
                                    collapseDependency.click();
                                    AllureReportUtil.info("Check for chevron right facing");
                                    softAssert.assertThat(canExpandDependency.isDisplayed())
                                            .as("If dependency is collapsed, controls should exist to expand it")
                                            .isTrue();
                                });
                            }
                        });
            }
            case "Debricked" -> {
                var listOfTenantIssues = getListOfIssuesTenant(scanType, debrickedApplicationDTO.getApplicationName(),
                        openSourceRelease2DTO.getReleaseName(), detailsReleasePage);
                listOfTenantIssues
                        .forEach(issue -> {
                            AllureReportUtil.info("Dependency Tab exists");
                            assertThat(issue.openDetails().getAllTabsNames().toString())
                                    .as("If dependency tab does not exist, immediate failure")
                                    .contains("Dependencies");
                            var dependencies = issue.openDetails().openDependencies().listOfDependencies;
                            if (dependencies.size() == 0) {
                                softAssert.assertThat(noDependency.isDisplayed())
                                        .as("If there is no dependency , message should be visible to the fact")
                                        .isTrue();
                            } else {
                                dependencies.forEach(dependency -> {
                                    AllureReportUtil.info("Checking for chevron down facing");
                                    softAssert.assertThat(dependencies.size())
                                            .as("Asserts the existence of dependencies for issue")
                                            .isGreaterThan(0);
                                    softAssert.assertThat(collapseDependency.isDisplayed())
                                            .as("If the dependency is expanded, controls should exist to collapse it")
                                            .isTrue();
                                    collapseDependency.click();
                                    AllureReportUtil.info("Check for chevron right facing");
                                    softAssert.assertThat(canExpandDependency.isDisplayed())
                                            .as("If dependency is collapsed, controls should exist to expand it")
                                            .isTrue();
                                });
                            }
                        });
            }
            case "Sonatype" -> {
                var listOfTenantIssues = getListOfIssuesTenant(scanType, applicationDTO.getApplicationName(),
                        openSourceReleaseDTO.getReleaseName(), detailsReleasePage);
                listOfTenantIssues
                        .forEach(issue -> {
                            AllureReportUtil.info("Dependency Tab does not exist");
                            softAssert.assertThat(issue.openDetails().getAllTabsNames().contains("Dependencies"))
                                    .as("There should not be a Dependencies tab associated with Sonatype issues")
                                    .isFalse();
                        });
            }
        }
        softAssert.assertAll();
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("800056")
    @MaxRetryCount(1)
    @Description("Checks dependencies of issues on Admin portal.  Debricked should have dependencies, Sonatype should not")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceSonatype", "prepareTestDataOpenSourceDebricked"},
            dataProvider = "dependencyScanType")
    public void verifyDependencyAdminTest(String scanType) {
        SoftAssertions softAssert = new SoftAssertions();
        var staticScanPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        if (scanType.equals("Debricked")) {
            var listOfAdminIssues = getListOfIssuesAdmin(scanType, debrickedApplicationDTO.getApplicationName(), staticScanPage);
            listOfAdminIssues
                    .forEach(issue -> {
                        AllureReportUtil.info("Dependency Tab exists");
                        assertThat(issue.openDetails().getAllTabsNames().toString())
                                .as("If dependency tab does not exist, immediate failure")
                                .contains("Dependencies");
                        var dependencies = issue.openDetails().openDependencies().listOfDependencies;
                        if (dependencies.size() == 0) {
                            softAssert.assertThat(noDependency.isDisplayed())
                                    .as("If there is no dependency , message should be visible to the fact")
                                    .isTrue();
                        } else {
                            dependencies.forEach(dependency -> {

                                AllureReportUtil.info("Checking for chevron down facing");
                                softAssert.assertThat(dependencies.size())
                                        .as("Asserts the existence of dependencies for issue")
                                        .isGreaterThan(0);
                                softAssert.assertThat(collapseDependency.isDisplayed())
                                        .as("If the dependency is expanded, controls should exist to collapse it")
                                        .isTrue();
                                collapseDependency.click();
                                AllureReportUtil.info("Checking for chevron right facing");
                                softAssert.assertThat(canExpandDependency.isDisplayed())
                                        .as("If dependency is collapsed, controls should exist to expand it")
                                        .isTrue();
                            });
                        }
                    });
        } else if (scanType.equals("Sonatype")) {
            var listOfAdminIssues = getListOfIssuesAdmin(scanType, applicationDTO.getApplicationName(), staticScanPage);
            listOfAdminIssues
                    .forEach(issue -> {
                        AllureReportUtil.info("Dependency Tab does not exist");
                        softAssert.assertThat(issue.openDetails().getAllTabsNames().contains("Dependencies"))
                                .as("There should not be a Dependencies tab associated with Sonatype issues")
                                .isFalse();
                    });
        }
        softAssert.assertAll();
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("790007")
    @MaxRetryCount(1)
    @Description("Verifies CVSS Vector information for Debricked Open Source Scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceDebricked"}, dataProvider = "vectorTestType")
    public void verifyCVSSVector(String scanName, String vectorName) {
        var releasesPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openYourReleases();
        if (scanName.equals("Debricked")) {
            checkTheVector(releasesPage, debrickedApplicationDTO.getApplicationName(), openSourceRelease2DTO.getReleaseName(), vectorName);
        } else {
            checkTheVector(releasesPage, debrickedApplicationDTO.getApplicationName(), debrickedApplicationDTO.getReleaseName(), vectorName);
        }
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("780066")
    @MaxRetryCount(1)
    @Description("Checks license link in License column")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceDebricked"})
    public void verifyLicenseLinkReleaseDetailTest() {
        var listOfComponents = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(debrickedApplicationDTO.getApplicationName(), debrickedApplicationDTO.getReleaseName())
                .openOpenSourceComponents()
                .getAllComponentsOnPage();
        checkLicensePage(listOfComponents);

    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("780066")
    @MaxRetryCount(1)
    @Description("Checks license link in License column")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestDataOpenSourceDebricked"})
    public void verifyLicenseLinkApplicationTest() {
        var listOfComponents = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications()
                .openOpenSourceComponents()
                .getAllComponentsOnPage();
        checkLicensePage(listOfComponents);
    }

    public void checkLicensePage(List<OpenSourceComponentCell> openSourceComponentCells) {
        for (var component : openSourceComponentCells) {
            if (component.getCellText(OpenSourceComponentCell.Column.LICENSE).equals("MIT")) {
                var componentName = component.getCellText(OpenSourceComponentCell.Column.COMPONENT);
                var licenseLink = component.getLicenseLinkText();
                AllureReportUtil.info("Validating that " + componentName + " is in the string(link)");
                assertThat(licenseLink)
                        .as("Checking that " + componentName + " is in string")
                        .contains(componentName);
                AllureReportUtil.info("Navigating to Git Hub and license page for " + componentName);
                BrowserUtil.openNewTab();
                Selenide.switchTo().window(1).navigate().to(licenseLink);
                var pageText = Selenide.$x("//h3[contains(text(),'MIT License')]");
                assertThat(pageText.getText())
                        .as("Should find data indicating that this is an MIT license")
                        .isEqualTo("MIT License");
                break;
            }
        }
    }


    public void checkTheVector(YourReleasesPage yourReleasesPage, String appName, String releaseName, String vectorName) {
        var issueVulnerability = yourReleasesPage.openDetailsForRelease(appName, releaseName)
                .openIssues()
                .groupBy("Scan Type")
                .getHighIssues()
                .get(0)
                .openDetails()
                .openVulnerability();
        assertThat(issueVulnerability.getCVSSVector())
                .as("For " + releaseName + " CVSS Vector information should equal " + vectorName)
                .isEqualTo(vectorName);

    }

    List<AdminIssueCell> getListOfIssuesAdmin(String scanType, String appName, ScanJobsPage scanJobsPage) {
        List<AdminIssueCell> listOfIssues = null;
        var issuesPage = scanJobsPage.findScanByAppName(appName)
                .openDetails()
                .openIssues();
        issuesPage.filters.expandAllFilters();
        issuesPage.filters.setFilterByName("Category").clickFilterOptionByName("Open Source");
        if (scanType.equals("Debricked")) {
            listOfIssues = issuesPage.groupBy("Scan Type").getCriticalIssues();
        } else if (scanType.equals("Sonatype")) {
            listOfIssues = issuesPage.groupBy("Scan Type").getHighIssues();
        }
        return listOfIssues;
    }

    List<TenantIssueCell> getListOfIssuesTenant(String scanType, String appName, String releaseName, YourReleasesPage yourReleasesPage) {
        List<TenantIssueCell> listOfIssues = null;
        var issuesPage = yourReleasesPage.openDetailsForRelease(appName, releaseName)
                .openIssues();
        if (scanType.equals("Debricked(Imported)")) {
            listOfIssues = issuesPage.groupBy("Scan Type").getHighIssues();
        } else {
            issuesPage.filters.expandAllFilters();
            issuesPage.filters.setFilterByName("Category").clickFilterOptionByName("Open Source");
            if (scanType.equals("Debricked")) {
                listOfIssues = issuesPage.groupBy("Scan Type").getCriticalIssues();
            } else if (scanType.equals("Sonatype")) {
                listOfIssues = issuesPage.groupBy("Scan Type").getHighIssues();
            }
        }
        return listOfIssues;
    }

    ReleaseDetailsPage choosePath(FodCustomTypes.EntitlementType entitlementType) {
        if (entitlementType == FodCustomTypes.EntitlementType.SonatypeEntitlement) {
            return LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                    .openDetailsForRelease(applicationDTO.getApplicationName(), openSourceReleaseDTO.getReleaseName());
        } else {
            return LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                    .openDetailsForRelease(debrickedApplicationDTO.getApplicationName(), openSourceRelease2DTO.getReleaseName());
        }
    }

    @DataProvider(name = "entitlementType", parallel = true)
    public static Object[][] entitlementType() {
        return new Object[][]{
                {FodCustomTypes.EntitlementType.SonatypeEntitlement},
                {FodCustomTypes.EntitlementType.DebrickedEntitlement}
        };
    }


    @DataProvider(name = "dependencyScanType", parallel = true)
    public static Object[][] dependencyScanType() {
        return new Object[][]{
                {"Debricked"},
                {"Debricked(Imported)"},
                {"Sonatype"}
        };
    }

    @DataProvider(name = "vectorTestType", parallel = true)
    public static Object[][] vectorTestType() {
        return new Object[][]{
                {"Debricked", "CVSS Vector: CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"},
                {"Debricked(Imported)", "CVSS Vector: CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"}
        };
    }
}
