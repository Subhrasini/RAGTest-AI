package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationCell;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.pages.tenant.applications.your_releases.YourReleasesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class YourReleasesAndYourApplicationsManagedTest extends FodBaseTest {

    TenantDTO tenantDTO;
    AttributeDTO picklist;
    HashMap<AttributeDTO, String> attributesMap;
    ApplicationDTO dynamicApp, staticApp, mobileApp, appForReleases;
    DynamicScanDTO dynamicScanDTO;
    MobileScanDTO mobileScanDTO;
    StaticScanDTO staticScanDTO;
    ReleaseDTO qaTest, development, productionCopyStateAndRetired;
    String groupName, microserviceName;
    HashMap<String, Integer> scenariosToValidate = new HashMap<>() {{
        put("append if no applications were assigned before", 0);
        put("overwrite if no applications were assigned before", 0);
        put("append if some applications were assigned before", 1);
        put("overwrite if some applications were assigned before", 2);
    }};
    List<String> scenarios = new ArrayList<>() {{
        add("append if no applications were assigned before");
        add("overwrite if no applications were assigned before");
        add("append if some applications were assigned before");
        add("overwrite if some applications were assigned before");
    }};

    public void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Enable Microservices option for web applications"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        groupName = "TenantGroup:" + UniqueRunTag.generate();

        appForReleases = ApplicationDTO.createDefaultInstance();
        dynamicApp = ApplicationDTO.createDefaultInstance();
        staticApp = ApplicationDTO.createDefaultInstance();
        staticApp.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Low);
        staticApp.setSdlcStatus(FodCustomTypes.Sdlc.Production);
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        mobileApp.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);

        picklist = AttributeDTO.createDefaultInstance();
        picklist.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        picklist.setRequired(false);
        picklist.setPickListValues(new String[]{"A", "B", "C"});
        attributesMap = new HashMap<>() {{
            put(picklist, "B");
        }};
        appForReleases.setAttributesMap(attributesMap);

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        qaTest = ReleaseDTO.createDefaultInstance();
        qaTest.setSdlcStatus(FodCustomTypes.Sdlc.QaTest);

        development = ReleaseDTO.createDefaultInstance();
        development.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        productionCopyStateAndRetired = ReleaseDTO.createDefaultInstance();
        productionCopyStateAndRetired.setSdlcStatus(FodCustomTypes.Sdlc.Production);
        productionCopyStateAndRetired.setCopyState(true);
        productionCopyStateAndRetired.setRetireSelectedRelease(true);
        productionCopyStateAndRetired.setCopyFromReleaseName(appForReleases.getReleaseName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare tenant/applications/completed scans for test execution")
    @Test(groups = {"regression"}, priority = 1)
    public void testDataPreparation() {
        init();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        CustomAttributesActions.createCustomAttribute(picklist, tenantDTO, true);
        TenantGroupActions.createTenantGroup(groupName, "", true);

        ApplicationActions.createApplication(appForReleases, tenantDTO, false);
        StaticScanActions.importScanTenant(appForReleases, "payloads/fod/static.java.fpr");
        ReleaseActions.createReleases(appForReleases, productionCopyStateAndRetired, qaTest, development);

        ApplicationActions.createApplication(dynamicApp, tenantDTO, false);
        ApplicationActions.createApplication(mobileApp, tenantDTO, false);
        ApplicationActions.createApplication(staticApp, tenantDTO, false);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.SetupScanPageStatus.Scheduled);

        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(dynamicApp.getReleaseName(), "payloads/fod/dynamic.zero.fpr",
                false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, false);

        MobileScanActions.importMobileScanAdmin(mobileApp.getApplicationName(),
                FodCustomTypes.ImportFprScanType.Mobile, "payloads/fod/mobile_IOS.fpr",
                false, false, false);
        MobileScanActions.completeMobileScan(mobileApp, false);

        StaticScanActions.completeStaticScan(staticApp, false);
    }

    @MaxRetryCount(3)
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate your releases page functionality (filtering, sorting, paging, assign attributes/groups)")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            priority = 1)
    public void yourReleasesPageManagedTest() {

        AllureReportUtil.info("Verify all filters are present in Your Releases view");
        var expectedFilters = new ArrayList<String>() {{
            add("Application Type");
            add("Business Criticality");
            add("Dynamic Scan Status");
            add(picklist.getAttributeName());
            add("Mobile Scan Status");
            add("Pass/Fail");
            add("Scan Type");
            add("SDLC Status");
            add("Star Rating");
            add("Application Created Date");
        }};
        var yourReleasesPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases();
        var columnCount = yourReleasesPage.getActiveTable().getColumnsCount();
        yourReleasesPage.filters.expandSideFiltersDrawer().collapseAllFilters();
        assertThat(yourReleasesPage.filters.getAllFilters())
                .as("Filters should contain values")
                .hasSameElementsAs(expectedFilters);

        AllureReportUtil.info("Verify Add/Remove Column functionality");
        var gridPopup = yourReleasesPage.filters.openGridSettings();
        gridPopup.setGridColumnCheckbox("SDLC Status", true);
        gridPopup.setGridColumnCheckbox("Policy Compliance", true);
        gridPopup.pressSave();
        assertThat(yourReleasesPage.getActiveTable().getColumnHeaders())
                .as("Column header should contain a new column")
                .contains("SDLC Status", "Policy Compliance");
        assertThat(yourReleasesPage.getActiveTable().getColumnsCount())
                .as("Column count should increase")
                .isGreaterThanOrEqualTo(columnCount);

        columnCount = yourReleasesPage.getActiveTable().getColumnsCount();
        gridPopup = yourReleasesPage.filters.openGridSettings();
        gridPopup.setGridColumnCheckbox("Policy Compliance", false);
        gridPopup.pressSave();
        assertThat(yourReleasesPage.getActiveTable().getColumnHeaders())
                .as("Column header should contain a new column")
                .doesNotContain("Policy Compliance");
        assertThat(yourReleasesPage.getActiveTable().getColumnsCount())
                .as("Column count should increase")
                .isEqualTo(columnCount - 1);

        AllureReportUtil.info("Validate Paging");
        new PagingActions().validatePaging();
        AllureReportUtil.info("Validate Pagination");
        new PagingActions().validatePagination();
        yourReleasesPage.paging.setRecordsPerPage_100();

        AllureReportUtil.info("Validate Ordering");
        var ordering = new Ordering(yourReleasesPage.getActiveTable());

        ordering.verifyOrderForColumn("Application");
        ordering.verifyOrderForColumn("Release");
        ordering.verifyOrderForColumn("# Issues");
        ordering.verifyOrderForColumn("Static");
        ordering.verifyOrderForColumn("Dynamic");
        ordering.verifyOrderForColumn("Mobile");
        ordering.verifyOrderForColumn("Last Completed");

        AllureReportUtil.info("Validate Filtering");
        yourReleasesPage.filters.expandSideFiltersDrawer();
        yourReleasesPage.openSDLCTab("Development");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getReleaseName().equals(development.getReleaseName()))
                .findFirst())
                .as("Filtering by development. " + development.getReleaseName() + " should present!")
                .isPresent();

        yourReleasesPage.openSDLCTab("QA/Test");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getReleaseName().equals(qaTest.getReleaseName()))
                .findFirst())
                .as("Filtering by QA/Test. " + qaTest.getReleaseName() + " should present!")
                .isPresent();

        yourReleasesPage.openSDLCTab("Production");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getReleaseName().equals(staticApp.getReleaseName()))
                .findFirst())
                .as("Filtering by Production. " + staticApp.getReleaseName() + " should present!")
                .isPresent();

        yourReleasesPage.openSDLCTab("Retired");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getReleaseName().equals(appForReleases.getReleaseName()))
                .findFirst())
                .as("Filtering by Retired. " + appForReleases.getReleaseName() + " should present!")
                .isPresent();

        yourReleasesPage.openSDLCTab("All");

        yourReleasesPage.filters.setFilterByName("Application Type").expand()
                .clickFilterOptionByName("Mobile");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getApplicationName().equals(mobileApp.getApplicationName()))
                .findFirst())
                .as("Filtering by Application Type/Mobile. " + mobileApp.getReleaseName()
                        + " should present!")
                .isPresent();
        yourReleasesPage.appliedFilters.clearAll();

        yourReleasesPage.filters.setFilterByName("Business Criticality").expand()
                .clickFilterOptionByName("Low");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getApplicationName().equals(staticApp.getApplicationName()))
                .findFirst())
                .as("Filtering by Business Criticality/Low. " + staticApp.getReleaseName()
                        + " should present!")
                .isPresent();
        yourReleasesPage.appliedFilters.clearAll();

        yourReleasesPage.filters.setFilterByName("Pass/Fail").expand()
                .clickFilterOptionByName("Failed");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getApplicationName().equals(dynamicApp.getApplicationName()))
                .findFirst())
                .as("Filtering by Pass/Fail/Failed. " + dynamicApp.getReleaseName()
                        + " should present!")
                .isPresent();
        yourReleasesPage.appliedFilters.clearAll();

        yourReleasesPage.filters.setFilterByName("SDLC Status").expand()
                .clickFilterOptionByName("Development");
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getApplicationName().equals(dynamicApp.getApplicationName()))
                .findFirst())
                .as("Filtering by SDLC Status/Development. " + dynamicApp.getReleaseName()
                        + " should present!")
                .isPresent();
        yourReleasesPage.appliedFilters.clearAll();
    }


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate your applications page functionality (filtering, sorting, paging)")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            priority = 2)
    public void yourApplicationsPageManagedTest() {
        var yourApplicationsPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourApplications();

        AllureReportUtil.info("Validate Paging");
        new PagingActions().validatePaging();
        AllureReportUtil.info("Validate Pagination");
        new PagingActions().validatePagination();
        new Paging().setRecordsPerPage_100();

        AllureReportUtil.info("Validate Filter Sort");
        yourApplicationsPage.filters.expandSideFiltersDrawer();

        var value = yourApplicationsPage.filters.setFilterByName("Sort").expand()
                .setFilterDropdownValue("Most Recent Change")
                .getFilterDropdownValue();

        assertThat(value).isEqualTo("Most Recent Change");
        assertThat(url()).contains("sort=changes");

        value = yourApplicationsPage.filters.setFilterByName("Sort").expand()
                .setFilterDropdownValue("Production Risk")
                .getFilterDropdownValue();

        assertThat(value).isEqualTo("Production Risk");
        assertThat(url()).contains("sort=risk");

        value = yourApplicationsPage.filters.setFilterByName("Sort").expand()
                .setFilterDropdownValue("Application Name (A to Z)")
                .getFilterDropdownValue();

        assertThat(value).isEqualTo("Application Name (A to Z)");
        assertThat(url()).contains("sortdir=Asc");

        value = yourApplicationsPage.filters.setFilterByName("Sort").expand()
                .setFilterDropdownValue("Application Name (Z to A)")
                .getFilterDropdownValue();

        assertThat(value).isEqualTo("Application Name (Z to A)");
        assertThat(url()).contains("sortdir=Desc");

        AllureReportUtil.info("Validate Filtering");
        yourApplicationsPage.filters.setFilterByName("Application type").expand().clickFilterOptionByName("Web");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(mobileApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Application type").expand().clickFilterOptionByName("Mobile");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(dynamicApp.getApplicationName(), staticApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Business Criticality").expand().clickFilterOptionByName("High");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(staticApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Business Criticality").expand().clickFilterOptionByName("Medium");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(staticApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Business Criticality").expand().clickFilterOptionByName("Low");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .contains(staticApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Scan Type").expand().clickFilterOptionByName("Dynamic");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(mobileApp.getApplicationName(), staticApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Scan Type").expand().clickFilterOptionByName("Static");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(dynamicApp.getApplicationName(), mobileApp.getApplicationName());

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Scan Type").expand().clickFilterOptionByName("Mobile");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .doesNotContain(dynamicApp.getApplicationName(), staticApp.getApplicationName());
    }

    @MaxRetryCount(3)
    @Owner("kbadia@opentext.com")
    @FodBacklogItem("728018")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate append and overwrite functionalities for All tab")
    @Test(groups = {"regression"}, dataProvider = "verifyApplicationPageTabs",
            dependsOnMethods = {"testDataPreparation", "yourReleasesPageManagedTest"})
    public void appendAndOverwriteScenarioForTabsTest(String tabName) {
        String newGroupName = "Brand New Group " + UniqueRunTag.generate();
        for (var scenario : scenarios) {
            appendAndOverwriteScenarioTest(scenario, scenariosToValidate.get(scenario),
                    scenariosToValidate.get(scenario) + 1, tabName, newGroupName);
        }
    }

    @MaxRetryCount(3)
    @Owner("kbadia@opentext.com")
    @FodBacklogItem("728018")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Your Releases page updated functionalities")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void releasesPageUpdatesTest() {
        var yourReleasesPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases();
        assertThat(yourReleasesPage.selectAllBtn.isDisplayed())
                .as("Select All Button is available in Your Releases Page")
                .isTrue();
        assertThat(yourReleasesPage.checkBox.exists())
                .as("Check box is exists in Your Releases Page")
                .isTrue();
        yourReleasesPage.getTable().getRow(0)
                .find(".form-group.control.checkbox").click();
        groupAndAttributeButtonTest(false);
        refresh();
        yourReleasesPage.selectAllBtn.click();
        groupAndAttributeButtonTest(false);
    }

    @MaxRetryCount(3)
    @Owner("kbadia@opentext.com")
    @FodBacklogItem("728018")
    @Severity(SeverityLevel.NORMAL)
    @Parameters({"Tab"})
    @Description("Validate Your Applications Page updated functionalities")
    @Test(groups = {"regression"}, dataProvider = "verifyApplicationPageTabs",

            dependsOnMethods = {"testDataPreparation", "appendAndOverwriteScenarioForTabsTest"})
    public void applicationPageUpdatesTest(String tabName) {
        var yourApplicationsPage = LogInActions
                .tamUserLogin(tenantDTO).openYourApplications();
        assertThat(yourApplicationsPage.tabs.tabExists("All"))
                .as("All tab should be available " +
                        "under Your Applications Page")
                .isTrue();
        assertThat(yourApplicationsPage.tabs.tabExists("Microservice"))
                .as("Microservice tab should be available " +
                        "under Your Applications Page")
                .isTrue();
        assertThat(yourApplicationsPage.tabs.tabExists("Non-Microservice"))
                .as("Non-Microservice tab should be available " +
                        "under Your Applications Page")
                .isTrue();
        switch (tabName) {
            case "All":
                yourApplicationsPage.openAllTab();
                break;
            case "Microservice":
                yourApplicationsPage.openMicroserviceTab();
                break;
            case "Non-Microservice":
                yourApplicationsPage.openNonMicroserviceTab();
                break;
            default:
                throw new IllegalStateException("Unexpected tab: " + tabName);
        }
        assertThat(yourApplicationsPage.selectAllBtn.isDisplayed())
                .as("Select All Button is " +
                        "available in Your Applications Page")
                .isTrue();
        assertThat(yourApplicationsPage.checkBox.exists())
                .as("Check box is exists in Your Applications Page")
                .isTrue();
        yourApplicationsPage.getTable()
                .getRow(0).find(".form-group.control.checkbox").click();
        groupAndAttributeButtonTest(true);
        refresh();
        yourApplicationsPage.selectAllBtn.click();
        groupAndAttributeButtonTest(true);
    }

    @MaxRetryCount(3)
    @Owner("kbadia@opentext.com")
    @FodBacklogItem("728018")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate all and non-microservice tab searching " +
            "should find applications even by Application attributes values")
    @Test(groups = {"regression"},
            dependsOnMethods = {"testDataPreparation", "applicationPageUpdatesTest"})
    public void searchAppThroughAttributeTest() {
        var textName = "Text" + UniqueRunTag.generate();
        var text = AttributeDTO.createDefaultInstance();
        var picklist = AttributeDTO.createDefaultInstance();
        text.setAttributeDataType(FodCustomTypes.AttributeDataType.Text);
        picklist.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        picklist.setPickListValues(new String[]{"XX", "QQ"});
        CustomAttributesActions.createCustomAttribute(text, tenantDTO, true);
        CustomAttributesActions.createCustomAttribute(picklist);
        HashMap<AttributeDTO, String> attribute = new HashMap<>() {{
            put(text, textName);
            put(picklist, "QQ");
        }};
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        applicationDTO.setAttributesMap(attribute);
        ApplicationActions.createApplication(applicationDTO, tenantDTO, false);
        var yourApplicationsPage = new TenantTopNavbar().openApplications().openAllTab();
        List<String> searchItems = new ArrayList<>() {{
            add(textName);
            add("QQ");
        }};
        for (var item : searchItems) {
            yourApplicationsPage.findWithSearchBox(item);
            assertThat(yourApplicationsPage.getTable().getRow(0).getText())
                    .as("Application should be available in " +
                            "Your Applications Page under All tab")
                    .contains(applicationDTO.getApplicationName());
            yourApplicationsPage.appliedFilters.clearAll();
            yourApplicationsPage.openNonMicroserviceTab().findWithSearchBox(item);
            assertThat(yourApplicationsPage.getTable().getRow(0).getText())
                    .as("Application should be available in " +
                            "Your Applications Page under Non-Microservice tab")
                    .contains(applicationDTO.getApplicationName());
            yourApplicationsPage.appliedFilters.clearAll();
        }
        var attributesPage = new TenantTopNavbar().openAdministration()
                .openSettings()
                .openAttributesTab();
        attributesPage.deleteAttribute(text.getAttributeName());
        attributesPage.deleteAttribute(picklist.getAttributeName());
    }

    private void groupAndAttributeButtonTest(boolean isApplicationsPage) {
        if (isApplicationsPage) {
            var yourApplicationsPage = page(YourApplicationsPage.class);
            assertThat(yourApplicationsPage.editAttributesBtn.isDisplayed())
                    .as("Edit Attribute Button is available in Your Applications Page")
                    .isTrue();
            assertThat(yourApplicationsPage.editGroupsBtn.isDisplayed())
                    .as("Edit Group Button is available in Your Applications Page")
                    .isTrue();
            var yourApplicationEditAttributesPopup = yourApplicationsPage
                    .pressEditAttributes();
            assertThat(yourApplicationEditAttributesPopup.businessCritically.isDisplayed())
                    .as("business criticality is available in " +
                            "Your Applications Attributes popup")
                    .isTrue();
            assertThat(yourApplicationEditAttributesPopup.sdlcDropdown.isDisplayed())
                    .as("SDLC Dropdown is not available in Your Applications Page")
                    .isFalse();
            assertThat(yourApplicationEditAttributesPopup.ownerDropdown.isDisplayed())
                    .as("Owner Dropdown is not available in Your Applications Page")
                    .isFalse();
        } else {
            var yourReleasesPage = page(YourReleasesPage.class);
            assertThat(yourReleasesPage.editAttributesBtn.isDisplayed())
                    .as("Edit Attribute Button is available in Your Releases Page")
                    .isTrue();
            assertThat(yourReleasesPage.editGroupsBtn.isDisplayed())
                    .as("Edit Group Button is not available in Your Releases Page")
                    .isFalse();
            var yourReleaseEditAttributesPopup = yourReleasesPage
                    .pressEditAttributes();
            assertThat(yourReleaseEditAttributesPopup.ownerDropdown.isDisplayed())
                    .as("Owner Dropdown is available in Your Releases Page")
                    .isTrue();
            assertThat(yourReleaseEditAttributesPopup.sdlcDropdown.isDisplayed())
                    .as("SDLC Dropdown is available in Your Releases Page")
                    .isTrue();
        }
    }

    private void appendAndOverwriteScenarioTest(String scenario, int existingAssignedAppCount, int currentAssignedAppCount, String tabName, String newGroupName) {
        ApplicationDTO applicationDTO;
        if (tabName.equals("Microservice")) {
            microserviceName = "microservice" + UniqueRunTag.generate();
            applicationDTO = ApplicationDTO.createDefaultInstance();
            applicationDTO.setMicroserviceToChoose(microserviceName);
            applicationDTO.setMicroservicesEnabled(true);
            applicationDTO.setApplicationMicroservices(new String[]{microserviceName});
            ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        } else {
            applicationDTO = ApplicationActions.createApplication(tenantDTO);
        }
        var groupsPage = new TenantTopNavbar()
                .openAdministration()
                .openUserManagement()
                .openGroups();
        if (scenario.equals("append if no applications were assigned before")) {
            groupsPage.addGroup(newGroupName, "", true);
        }
        assertThat(groupsPage.getGroupByName(newGroupName).getAssignedApplicationsCount())
                .as("Group should be created with zero application")
                .isEqualTo(existingAssignedAppCount);
        var yourApplicationsPage = new TenantTopNavbar().openApplications();
        switch (tabName) {
            case "All":
                yourApplicationsPage.openAllTab();
                break;
            case "Microservice":
                yourApplicationsPage.openMicroserviceTab();
                break;
            case "Non-Microservice":
                yourApplicationsPage.openNonMicroserviceTab();
                break;
            default:
                throw new IllegalStateException("Unexpected tab: " + tabName);
        }
        var editGroupPopups = yourApplicationsPage
                .getAppByName(applicationDTO.getApplicationName())
                .setCheckbox(true).pressEditGroups();
        assertThat(editGroupPopups.getAssignedItemCount())
                .as("Check that number of items near the 'Edit Group' " +
                        "is the same as number of applications that were selected")
                .isEqualTo(1);
        if (scenario.equals("append if no applications were assigned before") ||
                scenario.equals("append if some applications were assigned before")) {
            editGroupPopups.setGroupToAssign(newGroupName).pressAppendAndSaveBtn();
        } else {
            editGroupPopups.setGroupToAssign(newGroupName).pressOverwriteAndSave();
        }
        var groupCell = new TenantTopNavbar().openAdministration()
                .openUserManagement().openGroups().getGroupByName(newGroupName);
        assertThat(groupCell.getAssignedApplicationsCount())
                .as("Check that number of assigned applications is the " +
                        "same as number of applications that were selected")
                .isEqualTo(currentAssignedAppCount);
        var assignApplicationCell = groupCell
                .pressAssignApplication().getAllSelectedApplications().stream()
                .filter(app -> app.getName().equals(applicationDTO.getApplicationName()))
                .findFirst().orElse(null);
        assertThat(assignApplicationCell)
                .as("Check that the 'selected' tab contains only " +
                        "names of applications that were selected").isNotNull();
        if (scenario.equals("append if no applications were assigned before")) {
            assignApplicationCell.clickAssign(false).pressSave();
            assertThat(groupsPage.getGroupByName(newGroupName).getAssignedApplicationsCount())
                    .as("Group should be created with zero application")
                    .isZero();
        } else {
            assignApplicationCell.pressSave();
        }
        BrowserUtil.clearCookiesLogOff();
    }

    @DataProvider(name = "verifyApplicationPageTabs", parallel = true)
    public Object[][] verifyApplicationPageTabs() {
        return new Object[][]{
                {"All"},
                {"Microservice"},
                {"Non-Microservice"}
        };
    }
}