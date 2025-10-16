package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.codeborne.pdftest.assertj.Assertions;
import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureAttachmentsUtil;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("762002")
@Slf4j
public class MicroserviceAttributesTest extends FodBaseTest {
    ApplicationDTO webAppWithMicro, webAppWithoutMicro;
    HashMap<String, String> microAttributesMap;
    TenantDTO tenantDTO;
    TenantUserDTO appLead;
    AttributeDTO microAttributeDto1, microAttributeDto2;
    String microAttributeValue = "Version1";
    String fileName="payloads/fod/WebGoat5.0.zip";
    String microColumnName = "Microservice";
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create microservice attributes and applications for the test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        AllureReportUtil.info("Creating Tenant and Entitlement with enabled 'Microservices option for web applications'");
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Enable Microservices option for web applications"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        microAttributeDto1 = AttributeDTO.createDefaultInstance();
        microAttributeDto1.setAttributeType(FodCustomTypes.AttributeType.Microservice);
        microAttributeDto1.setRequired(true);
        microAttributeDto2 = AttributeDTO.createDefaultInstance();
        microAttributeDto2.setAttributeType(FodCustomTypes.AttributeType.Microservice);

        webAppWithMicro = ApplicationDTO.createDefaultInstance();
        microAttributesMap = new HashMap<>() {{
            put(microAttributeDto1.getAttributeName(), microAttributeValue);
        }};
        webAppWithMicro.setMicroservicesEnabled(true);
        webAppWithMicro.setApplicationMicroservices(new String[]{"Microservice 0", "Microservice 1"});
        webAppWithMicro.setMicroserviceToChoose("Microservice 0");
        webAppWithMicro.setMicroserviceAttributesMap(microAttributesMap);
        webAppWithoutMicro = ApplicationDTO.createDefaultInstance();
        appLead = TenantUserDTO.createDefaultInstance();
        appLead.setTenant(tenantDTO.getTenantCode());
        appLead.setUserName(appLead.getUserName() + "-NoManageAccess");
        appLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        CustomAttributesActions.createCustomAttribute(microAttributeDto1, tenantDTO, true);
        CustomAttributesActions.createCustomAttribute(microAttributeDto2, tenantDTO, false);
        ApplicationActions.createApplication(webAppWithMicro, tenantDTO, false);
        ApplicationActions.createApplication(webAppWithoutMicro, tenantDTO, false);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(fileName);
        staticScanDTO.setAssessmentType("Static Assessment");
        StaticScanActions.createStaticScan(staticScanDTO, webAppWithMicro, FodCustomTypes.SetupScanPageStatus.Completed);
        StaticScanActions.createStaticScan(staticScanDTO, webAppWithoutMicro, FodCustomTypes.SetupScanPageStatus.Completed);
        TenantUserActions.createTenantUsers(tenantDTO, appLead);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration()
                .openUsers().findWithSearchBox(appLead.getUserName())
                .pressAssignApplicationsByUser(appLead.getUserName())
                .selectAssignAllCheckbox()
                .pressSave();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify new microservice attributes type are present in settings page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroserviceAttributesInSettingsPage() {
        String attributeName = "MicroserviceAttributeTest-" + UniqueRunTag.generate();
        AllureReportUtil.info("New Microservice Attribute type should be present in Add Attribute Popup");
        var attributeSettingsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openAdministration().openSettings().openAttributesTab();
        var attributePopup = attributeSettingsPage.addAttribute();
        assertThat(attributePopup.getAllAttributeTypes())
                .as("New drop down with label Attribute Type should be present with Application and microservice options")
                .contains("Application", "Microservice");
        attributePopup.setName(attributeName).setAttributeType(FodCustomTypes.AttributeType.Microservice);
        assertThat(attributePopup.getAllDataTypes())
                .as("Data Type should be to set to Text  if Attribute type is set to Microservice")
                .contains("Picklist", "Text", "Boolean", "Date", "User");
        attributePopup.setDataType(FodCustomTypes.AttributeDataType.Text).pressSave();

        AllureReportUtil.info("New microservice attribute should be added in Attributes Page");
        assertThat(attributeSettingsPage.getAttributeTable().getAllColumnValues(0))
                .as("Verify the newly created attribute should show in the attributes list")
                .contains(attributeName);
        assertThat(attributeSettingsPage.getAttributeByName(attributeName).getAttributeType())
                .as("Verify Attribute Type column  lists Microservice as a type")
                .contains("Microservice");
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify microservice  attributes in Reports and Data exports")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroserviceAttributesInReportsAndDataExports() {
        LogInActions.tamUserLogin(tenantDTO);
        var dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Applications);
        var fileGeneratedFromReportsPage = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(fileGeneratedFromReportsPage).getColumnHeaders();
        assertThat(dataFromCsv)
                .as("Validate microservice attribute names are present in columns")
                .contains(microAttributeDto1.getAttributeName());

        var releaseName = new TenantTopNavbar().openApplications().openYourReleases().findWithSearchBox(webAppWithMicro.getApplicationName())
                .getAllReleases().get(0).getFullReleaseName();
        webAppWithMicro.setReleaseName(releaseName);
        var report = ReportDTO.createInstance(webAppWithMicro, "Static Summary");
        var reportsPage = ReportActions.createReport(report);
        var file = reportsPage.getReportByName(report.getReportName())
                .downloadReport("pdf");
        PDF reportFile = new PDF(file);
        AllureAttachmentsUtil.attachFile(file, report.getReportName(), "pdf", "application/pdf");
        Assertions.assertThat(reportFile)
                .as("Microservice attribute name should be present in Reports")
                .containsText(microAttributeDto1.getAttributeName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify microservice  attributes in application creation wizard")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroserviceAttributesInCreateApplicationWizard() {
        AllureReportUtil.info("Verify if Microservice type attribute is set as Required, any microservice creation should not " +
                "happen without setting value to that attribute while creating an application");
        var appDTO = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenantDTO);
        var createApplicationPopup = new TenantTopNavbar().openApplications().pressAddNewApplicationBtn();
        createApplicationPopup.setApplicationName(appDTO.getApplicationName())
                .chooseBusinessCriticality(FodCustomTypes.BusinessCriticality.High)
                .chooseApplicationType(FodCustomTypes.AppType.Web)
                .enableMicroservices(true).pressNextButton()
                .addMicroservices("Microservice 1").pressNextButton();
        assertThat(createApplicationPopup.getMicroserviceAttributeNames())
                .as("Microservice Attributes Page should list only Microservice Type Attributes")
                .contains(microAttributeDto1.getAttributeName()).hasSizeGreaterThan(0);
        createApplicationPopup.pressNextButton()
                .setReleaseName(appDTO.getReleaseName())
                .chooseSdlcStatus(appDTO.getSdlcStatus())
                .chooseMicroservice("Microservice 1")
                .chooseOwner(appDTO.getOwner())
                .pressNextButton();

        assertThat(createApplicationPopup.microAttributeSummaryContainer.exists())
                .as("Application summary page is showing the Microservice Attributes as the group")
                .isTrue();
        createApplicationPopup.pressSaveButton();
        assertThat(createApplicationPopup.errorBlock.exists())
                .as("Error Block should be displayed if required value is not provided ")
                .isTrue();
        assertThat(createApplicationPopup.getErrorMessages())
                .as("Error message should be displayed if required value is not provided ")
                .contains("The " + microAttributeDto1.getAttributeName() + " field is required.");
        createApplicationPopup.close();

        AllureReportUtil.info("Verify new Tab for microservice attributes on Application Settings page is only visible when" +
                "Application is of type Microservice");
        assertThat(new TenantTopNavbar().openApplications().openDetailsFor(webAppWithMicro.getApplicationName())
                .openSettings().openMicroservicesTab().tabs.getActiveTab())
                .as("New Tab for Microservice Attributes is visible when application is of type microservice")
                .isEqualTo("Microservices");
        assertThat(new TenantTopNavbar().openApplications().openDetailsFor(webAppWithoutMicro.getApplicationName())
                .openSettings().tabs.tabExists("Microservices"))
                .as("Microservice Attributes tab is not visible for Non microservice application")
                .isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify edit , delete functionalities in attributes page")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyMicroserviceAttributesInCreateApplicationWizard"})
    public void verifyEditAndDeleteMicroserviceAttributes() {
        AllureReportUtil.info("Verify edit  microservice attribute ,Attribute name and Attribute Type should be disabled");

        LogInActions.tamUserLogin(tenantDTO);
        var attributeSettingsPage = new TenantTopNavbar().openAdministration().openSettings().openAttributesTab();
        var editAttributePopup = attributeSettingsPage.getAttributeByName(microAttributeDto1.getAttributeName()).edit();
        assertThat(editAttributePopup.attributeNameField.shouldBe(Condition.visible, Duration.ofMinutes(1)).isEnabled())
                .as("Attribute name in edit attribute popup should be disabled")
                .isFalse();
        assertThat(editAttributePopup.attributeTypeDropdown.isEnabled())
                .as("Attribute Type in edit attribute popup should be disabled")
                .isFalse();
        editAttributePopup.setEditableOnlyBySL(true).pressSave();

        AllureReportUtil.info("Verify delete microservice attribute functionality");
        attributeSettingsPage.deleteAttribute(microAttributeDto2.getAttributeName());
        assertThat(attributeSettingsPage.getAttributeTable().getAllColumnValues(0))
                .as("Verify the newly created attribute should show in the attributes list")
                .doesNotContain(microAttributeDto2.getAttributeName());
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify Microservice Attribute with checked 'Edit only by sec leads', user with no manage " +
                "access cannot edit the attribute values.");
        assertThat(LogInActions.tenantUserLogIn(appLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(webAppWithMicro.getApplicationName()).openSettings()
                .openMicroservicesTab().editMicroservice("Microservice 0")
                .getAttributeByAttributeDTO(microAttributeDto1).$("input").isEnabled())
                .as("The input field should be disabled,user cannot edit attribute value")
                .isFalse();
    }

    @MaxRetryCount(3)
    @FodBacklogItem("738007")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Microservice column is added in Issues Data Export")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroserviceColumnInIssueDataExport() {
        AllureReportUtil.info("Verify Microservice column is adding to Issue Data Export if microservice check-box is checked");
        LogInActions.tamUserLogin(tenantDTO);
        var dataExportDTO1 = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        dataExportDTO1.setColumnsToSelectAndUnSelect(new HashMap<>() {{
            put(microColumnName, true);
        }});
        var fileGeneratedFromReportsPage1 = DataExportActions.createDataExportAndDownload(dataExportDTO1);
        var dataFromCsv1 = new CSVHelper(fileGeneratedFromReportsPage1).getColumnHeaders();
        assertThat(dataFromCsv1)
                .as("Validate csv file should have a column for microservice")
                .contains(microColumnName);

        AllureReportUtil.info("Verify Microservice column is not present in Issue Data Export if microservice check-box is unchecked");
        var dataExportDTO2 = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        dataExportDTO2.setColumnsToSelectAndUnSelect(new HashMap<>() {{
            put(microColumnName, false);
        }});
        var fileGeneratedFromReportsPage2 = DataExportActions.createDataExportAndDownload(dataExportDTO2);
        var dataFromCsv2 = new CSVHelper(fileGeneratedFromReportsPage2).getColumnHeaders();
        assertThat(dataFromCsv2)
                .as("Validate csv file should not have a column for microservice")
                .doesNotContain(microColumnName);
    }
}
