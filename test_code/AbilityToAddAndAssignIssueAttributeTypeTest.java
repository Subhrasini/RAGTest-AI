package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.FiltersSettingsPopup;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AttributeDTO;
import com.fortify.fod.common.entities.DataExportDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.common.tenant.popups.AssignAttributesPopup;
import com.fortify.fod.ui.pages.tenant.administration.settings.AttributesPage;
import com.fortify.fod.ui.pages.tenant.administration.settings.SettingsPage;
import com.fortify.fod.ui.pages.tenant.administration.settings.popups.AttributeDefinitionPopup;
import com.fortify.fod.ui.pages.tenant.applications.application.issues.ApplicationIssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DataExportActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;


@Owner("jlal@opentext.com")
@FodBacklogItem("1650023")
@Slf4j

public class AbilityToAddAndAssignIssueAttributeTypeTest extends FodBaseTest {
    private ApplicationDTO applicationDTO;
    private StaticScanDTO staticScanDTO;
    private AttributeDefinitionPopup attributeDefinitionPopup;
    private AssignAttributesPopup assignAttributesPopup;
    private AttributeDTO issueAttributeDTO;
    private ApplicationIssuesPage applicationIssuesPage;
    private ReleaseIssuesPage releaseIssuesPage;
    private SettingsPage settingsPage;
    private AttributesPage attributesPage;
    SoftAssert softAssert;
    final List<String> issueAttributeDataTypeList = Arrays.asList("(Choose One)", "Picklist", "Text", "Boolean", "Date", "User");
    final String testPicklistValue = "issuePickListTestValue";
    private String app;
    List<String> attributeListAdded;
    Map<String, String> attributeNameByDataTypeMap;
    private String releaseName;
    ReleaseDetailsPage releaseDetailsPage;
    protected final String fileName = "payloads/fod/10JavaDefects_Small(OS).zip";

    @Test(groups = {"regression"})
    public void testDataPrepTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        app = applicationDTO.getApplicationName();
        releaseName = applicationDTO.getReleaseName();
        AllureReportUtil.info("Create custom attributes of all 5 attributeListAdded types and save");
        releaseDetailsPage = ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        settingsPage = releaseDetailsPage.tenantTopNavbar.openAdministration()
                .openSettings();
        attributeNameByDataTypeMap = new HashMap<>();
        for (String data : issueAttributeDataTypeList) {
            if (!(data.equalsIgnoreCase("(Choose One)"))) {
                attributeDefinitionPopup = settingsPage.openAttributesTab().addAttribute();
                issueAttributeDTO = AttributeDTO.createDefaultInstance();
                issueAttributeDTO.setAttributeType(FodCustomTypes.AttributeType.Issue);
                issueAttributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.valueOf(data));
                attributeNameByDataTypeMap.put(data.trim(), issueAttributeDTO.getAttributeName());
                attributeDefinitionPopup.setName(issueAttributeDTO.getAttributeName())
                        .setAttributeType(issueAttributeDTO.getAttributeType())
                        .setDataType(issueAttributeDTO.getAttributeDataType());
                if (data.equalsIgnoreCase("Picklist")) {
                    attributeDefinitionPopup.setPicklistValueAndSave(testPicklistValue);
                }
                attributeDefinitionPopup.pressSave(true);
            }
        }
        attributeListAdded = attributeNameByDataTypeMap.values().stream().collect(Collectors.toList());
        AllureReportUtil.info("Start static scan by uploading fpr to have some issues");
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that 5 types of IssueAttribute is Visbile in add attribute Page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPrepTest"}, priority = 0)
    public void verifyIsIssueAttributeVisibleTest() {
        softAssert = new SoftAssert();
        AllureReportUtil.info("Login and Navigate to Administration -> Settings -> OpenAttribute");
        attributeDefinitionPopup = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration()
                .openSettings().openAttributesTab().addAttribute();
        List<String> allAttributesType = attributeDefinitionPopup.getAllAttributeTypes();
        List<String> allDataTypes = attributeDefinitionPopup.getAllDataTypes();
        assertThat(allAttributesType).as("AttributeType dropdown must contain Issue").contains("Issue");
        assertThat(allDataTypes).as("All 5 DataTypes are present in dropdown for Issue Attribute Type")
                .containsExactlyInAnyOrderElementsOf(issueAttributeDataTypeList);
        for (String data : allDataTypes) {
            if (!(data.equalsIgnoreCase("(Choose One)"))) {
                attributeDefinitionPopup.setAttributeType(FodCustomTypes.AttributeType.Issue).setDataType(FodCustomTypes.AttributeDataType.valueOf(data));
                AllureReportUtil.info("data type set in dropdown is - " + attributeDefinitionPopup.getDataType());
                attributeDefinitionPopup.getDataType();
            }
            softAssert.assertFalse(attributeDefinitionPopup.isRequiredCheckboxElement.isEnabled());
        }
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign custom Attributes of issue type is present in group by and filter on release issue Page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPrepTest"}, priority = 1)
    public void verifyAssignAttributeOnReleaseIssuePageTest() {
        AllureReportUtil.info("Tenant Name - " + defaultTenantDTO.getTenantName() + ", application name - " + app + ", release name - " + releaseName);
        AllureReportUtil.info("Refresh the Release issue page until issue count starts reflecting");
        releaseIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases().openDetailsForRelease(app, releaseName).openIssues();
        try {
            while (releaseIssuesPage.getIssuesCounters().getSum() <= 0) {
                refresh();
            }
        } catch (Exception e) {
            AllureReportUtil.info("error is - " + e.getMessage());
        }
        assignAttributesPopup = releaseIssuesPage.pressAssignAttributes();
        List<String> customeAttributeList = assignAttributesPopup.allAttributeTags.attributes("data-attribute-name");
        Assert.assertTrue(customeAttributeList.containsAll(attributeListAdded));
        AllureReportUtil.info("attributeNameByDataTypeMap -- " + attributeNameByDataTypeMap.get("Picklist"));
        assignAttributesPopup.setPicklistAttributeByLabel(attributeNameByDataTypeMap.get("Picklist"), testPicklistValue);
        assignAttributesPopup.setTextAttributeByLabel(attributeNameByDataTypeMap.get("Text"), "testAnyTextValue");
        assignAttributesPopup.setBooleanAttributeByLabel(attributeNameByDataTypeMap.get("Boolean"), "True");
        assignAttributesPopup.setDateAttributeByLabel(attributeNameByDataTypeMap.get("Date"));
        assignAttributesPopup.setUserAttributeByLabel(attributeNameByDataTypeMap.get("User"));
        releaseIssuesPage.pressSave(true);
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in group by list after assigning");
        List<String> allGroupByOptions = releaseIssuesPage.groupByElement.getOptions().texts();
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("User")));
        FiltersSettingsPopup filtersSettingsPopup = releaseIssuesPage.filters.openSettings().openFiltersTab();
        List<String> allFilterOptions = filtersSettingsPopup.getVisibleOptions();
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in filters list in settings after assigning");
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("User")));
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in groups list in settings after assigning");
        List<String> allGroupsOptionsInPopup = filtersSettingsPopup.openGroupsTab().getVisibleOptions();
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("User")));
        filtersSettingsPopup.pressClose();
        releaseIssuesPage.clickAll();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign custom Attributes of issue type is present in group by and filter on application issue Page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPrepTest"}, priority = 2)
    public void verifyAssignAttributeOnApplicationIssuePageTest() {
        AllureReportUtil.info("Tenant Name - " + defaultTenantDTO.getTenantName() + ", application name - " + app + ", release name - " + releaseName);
        AllureReportUtil.info("Refresh the application issue page until issue count starts reflecting");
        applicationIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openDetailsFor(app).openIssues();
        try {
            while (applicationIssuesPage.getIssuesCounters().getSum() <= 0) {
                refresh();
            }
        } catch (Exception e) {
            AllureReportUtil.info("error is - " + e.getMessage());
        }
        assignAttributesPopup = applicationIssuesPage.pressAssignAttributes();
        List<String> customeAttributeList = assignAttributesPopup.allAttributeTags.attributes("data-attribute-name");
        Assert.assertTrue(customeAttributeList.containsAll(attributeListAdded));
        AllureReportUtil.info("attributeNameByDataTypeMap -- " + attributeNameByDataTypeMap.get("Picklist"));
        assignAttributesPopup.setPicklistAttributeByLabel(attributeNameByDataTypeMap.get("Picklist"), testPicklistValue);
        assignAttributesPopup.setTextAttributeByLabel(attributeNameByDataTypeMap.get("Text"), "testAnyTextValue");
        assignAttributesPopup.setBooleanAttributeByLabel(attributeNameByDataTypeMap.get("Boolean"), "True");
        assignAttributesPopup.setDateAttributeByLabel(attributeNameByDataTypeMap.get("Date"));
        assignAttributesPopup.setUserAttributeByLabel(attributeNameByDataTypeMap.get("User"));
        applicationIssuesPage.pressSave(true);
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in group by list after assigning");
        List<String> allGroupByOptions = applicationIssuesPage.groupByElement.getOptions().texts();
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allGroupByOptions.contains(attributeNameByDataTypeMap.get("User")));
        FiltersSettingsPopup filtersSettingsPopup = applicationIssuesPage.filters.openSettings().openFiltersTab();
        List<String> allFilterOptions = filtersSettingsPopup.getVisibleOptions();
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in filters list in settings after assigning");
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allFilterOptions.contains(attributeNameByDataTypeMap.get("User")));
        AllureReportUtil.info("Verify that picklist, Boolean, User attributes are visible in groups list in settings after assigning");
        List<String> allGroupsOptionsInPopup = filtersSettingsPopup.openGroupsTab().getVisibleOptions();
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("Picklist")));
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("Boolean")));
        Assert.assertTrue(allGroupsOptionsInPopup.contains(attributeNameByDataTypeMap.get("User")));
        filtersSettingsPopup.pressClose();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign custom Attributes of issue type is present on release issue Page when Issue is opened in new tab")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPrepTest"}, priority = 4)
    public void verifyAssignAttributeInNewTab() {
        AllureReportUtil.info("Tenant Name - " + defaultTenantDTO.getTenantName() + ", application name - " + app + ", release name - " + releaseName);
        AllureReportUtil.info("Refresh the application issue page until issue count starts reflecting");
        releaseIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases().openDetailsForRelease(app, releaseName).openIssues();
        try {
            while (releaseIssuesPage.getIssuesCounters().getSum() <= 0) {
                refresh();
            }
        } catch (Exception e) {
            AllureReportUtil.info("error is - " + e.getMessage());
        }
        AllureReportUtil.info("Open IssueId in new tab and verify assign attribute");
        releaseIssuesPage.issuesCheckBox.get(0).parent().click();
        releaseIssuesPage = releaseIssuesPage.openAndSwitchToIssueInNewTab();
        releaseIssuesPage.pressAssignAttributes();
        List<String> customeAttributeList = assignAttributesPopup.allAttributeTags.attributes("data-attribute-name");
        Assert.assertTrue(customeAttributeList.containsAll(attributeListAdded));
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify all customs attributes of Issue type created is present in data exported reports of Issue template")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPrepTest"}, priority = 5)
    public void verifyCustomIssueAttributesInDataExportReportTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        var issuesDto = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(issuesDto);
        var dataFromCsv2 = new CSVHelper(generatedFromReportsPage).getColumnHeaders();
        var columnHeaderList = Arrays.stream(dataFromCsv2).toList();
        assertThat(columnHeaderList).as("Attributes should be  present in dataExport's column")
                .containsAll(attributeListAdded);
    }

    @Description("Delete all attributes created as part of this test class at end")
    @AfterClass
    public void attributeCleanUpTest() {
        AllureReportUtil.info("Custom attributes cleanup");
        attributesPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration()
                .openSettings()
                .openAttributesTab();
        for (String attributeToDelete : attributeListAdded) {
            attributesPage.deleteAttribute(attributeToDelete);
        }
    }
}
