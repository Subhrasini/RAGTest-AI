package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.statics.scan.StaticScanIssuesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.FodBacklogItems;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;


@Owner("vdubovyk@opentext.com")
@FodBacklogItems({@FodBacklogItem("76398"), @FodBacklogItem("762043"), @FodBacklogItem("762006")})
@Slf4j
public class OWASPClassificationAndReportsTest extends FodBaseTest {

    ApplicationDTO mainApplicationDTO = ApplicationDTO.createDefaultInstance();
    StaticScanDTO mainStaticScanDTO = StaticScanDTO.createDefaultInstance();
    EntitlementDTO fortifyEntitlement, sonatypeEntitlement, debrickedEntitlement;
    TenantDTO debrickedTenantDTO, sonatypeTenantDTO;

    private void init() {
        fortifyEntitlement = EntitlementDTO.createDefaultInstance();

        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);

        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);

        sonatypeTenantDTO = TenantDTO.createDefaultInstance();
        sonatypeTenantDTO.setEntitlementDTO(sonatypeEntitlement);

        debrickedTenantDTO = TenantDTO.createDefaultInstance();
        debrickedTenantDTO.setEntitlementDTO(debrickedEntitlement);
    }

    @MaxRetryCount(3)
    @Description("Prepare test data for OWASP tests")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        init();

        log.info("Creating tenants with Fortify, Sonatype and Debricked entitlements.");
        TenantActions.createTenants(true, sonatypeTenantDTO, debrickedTenantDTO);
        EntitlementsActions.createEntitlements(sonatypeTenantDTO, false, fortifyEntitlement);
        EntitlementsActions.createEntitlements(debrickedTenantDTO, false, fortifyEntitlement);
        BrowserUtil.clearCookiesLogOff();

        log.info("Creating main application, and initiating a static scan for default tenant.");
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        ApplicationActions.createApplication(mainApplicationDTO);
        StaticScanActions.createStaticScan(mainStaticScanDTO, mainApplicationDTO);
    }

    @MaxRetryCount(3)
    @Description("Reports and exports test")
    @Test(dataProvider = "owaspVersions", groups = {"regression"},
            dependsOnMethods = "prepareTestData")
    public void reportsAndExportsTest(String owaspVersion) {
        var owaspModule = owaspVersion.equals("OWASP 2021") ? "OWASP Top 10 2021" : owaspVersion;
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());

        var reportsPage = new TenantTopNavbar().openReports();

        log.info("Check Issues data export for " + owaspVersion);
        var dataExportWizardPopup =
                reportsPage
                        .openDataExport()
                        .pressCreateExportBtn()
                        .setExportTemplate(FodCustomTypes.DataExportTemplate.Issues)
                        .pressNextButton()
                        .setDateFrom(1)
                        .setDateTo(30)
                        .pressNextButton();

        assertThat(
                dataExportWizardPopup
                        .dataExportsColumns
                        .getText())
                .as("Data exports columns should contain correct OWASP version value")
                .contains(owaspVersion);

        dataExportWizardPopup.pressCloseButton();

        log.info("Check Report Modules");
        assertThat(
                reportsPage
                        .openTemplates()
                        .pressNewTemplateButton()
                        .setName(owaspModule)
                        .pressNextButton()
                        .setScanType("Static")
                        .pressNextButton()
                        .availableModules
                        .getText())
                .as("Report Modules should contain '" + owaspModule + "' and does not 'OWASP 2013' module")
                .contains(owaspModule)
                .doesNotContain("OWASP 2013 Top 10");
    }

    @MaxRetryCount(3)
    @Description("Issues page tests")
    @Test(dataProvider = "owaspVersions", groups = {"regression"},
            dependsOnMethods = "prepareTestData")
    public void issuesPageTest(String owaspVersion) {

        var filtersSettingsPopup =
                LogInActions
                        .tamUserLogin(defaultTenantDTO)
                        .tenantTopNavbar
                        .openApplications()
                        .openDetailsFor(mainApplicationDTO.getApplicationName())
                        .openIssues()
                        .filters
                        .openSettings();

        log.info("Check Filters options");
        assertThat(
                filtersSettingsPopup
                        .getVisibleOptions())
                .as("Filtering options should contain correct OWASP version option")
                .contains(owaspVersion);

        filtersSettingsPopup.openGroupsTab();

        log.info("Check Grouping options");
        assertThat(
                filtersSettingsPopup
                        .getVisibleOptions())
                .as("Grouping options should contain correct OWASP version option")
                .contains(owaspVersion);
    }

    @MaxRetryCount(3)
    @Description("Dashboard and Trending tab test")
    @Test(dataProvider = "owaspVersions", groups = {"regression"},
            dependsOnMethods = "prepareTestData")
    public void dashboardAndTrendingTest(String owaspVersion) {
        var tenantTopNavbar = LogInActions
                .tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar;

        log.info("Verify grouping options on the Dashboard page");
        var dashboardPage = tenantTopNavbar
                .openDashboard()
                .clickActions()
                .clickEdit()
                .getCellsByRow(0)
                .get(0)
                .setTileType("Trending Chart")
                .setDataType("Issues");

        assertThat(
                dashboardPage
                        .getGroupByOptions())
                .as("Grouping options on Trending chart should contain correct OWASP version option")
                .contains(owaspVersion);

        dashboardPage.clickCancel();

        log.info("Verify grouping options on the Release Overview page");

        assertThat(
                tenantTopNavbar
                        .openApplications()
                        .openYourReleases()
                        .openDetailsForRelease(mainApplicationDTO)
                        .getGroupByOptions())
                .as("Grouping options on Trending chart should contain correct OWASP version option")
                .contains(owaspVersion);
    }

    @MaxRetryCount(3)
    @Description("Policy Compliance test")
    @Test(dataProvider = "owaspVersions", groups = {"regression"},
            dependsOnMethods = "prepareTestData")
    public void policyComplianceTest(String owaspVersion) {
        assertThat(
                LogInActions
                        .tamUserLogin(defaultTenantDTO)
                        .tenantTopNavbar
                        .openAdministration()
                        .openPolicyManagement()
                        .openPolicies()
                        .pressAddPolicy()
                        .getComplianceRequirementDropdown()
                        .getOptionsTextValues())
                .as("Policy Compliance should contain correct OWASP version option")
                .contains(owaspVersion);
    }

    @FodBacklogItem("762006")
    @MaxRetryCount(3)
    @Description("This test verifies the report for open-source component analysis.")
    @Test(dataProvider = "tenantsAndEntitlements", groups = {"regression"},
            dependsOnMethods = "prepareTestData")
    public void testOpenSourceComponentAnalysis(TenantDTO tenant, String payload) {
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenant.getTenantCode());
        var a = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(a);

        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload("payloads/fod/" + payload);
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON);
        staticScanDTO.setLanguageLevel("3");
        StaticScanActions.createStaticScan(staticScanDTO, a)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.Completed)
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        var sections = page(StaticScanIssuesPage.class)
                .openIssues()
                .getAllIssues()
                .get(0)
                .openDetails()
                .openVulnerability();

        assertThat(sections.getSections())
                .as("There should be 'Standards and Best Practices' section")
                .contains("Standards and Best Practices");

        assertThat(sections.getStandardsAndBestPracticesHeaders())
                .as("There should be 'OWASP 2021' and 'PCI 4.0' category")
                .contains("OWASP 2021", "PCI 4.0");
    }

    @DataProvider(name = "owaspVersions", parallel = true)
    public static Object[][] owaspVersions() {
        return new Object[][]{
                {"OWASP 2021"},
                {"OWASP ASVS 4.0"}
        };
    }

    @DataProvider(name = "tenantsAndEntitlements")
    public Object[][] tenantsAndEntitlements() {
        return new Object[][]{
                {sonatypeTenantDTO, "vuln_python with requirements (OS).zip"},
                {debrickedTenantDTO, "vuln_python_with_pipfilelock(debricked).zip"}

        };
    }
}