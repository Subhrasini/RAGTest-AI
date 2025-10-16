package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import dev.failsafe.internal.util.Assert;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("1243003")
@Slf4j
public class CustomReleaseAttributesTest extends FodBaseTest {
    TenantDTO tenantDTO;
    TenantUserDTO appLead;
    ApplicationDTO webAppWithRelease, webAppWithRelease1;
    StaticScanDTO staticScanDTO;
    HashMap<AttributeDTO, String> releaseAttributesMap,releaseAttributesMap1;
    AttributeDTO releaseAttribute1, releaseAttribute2, releaseAttribute3;
    String[] releasePickListValues = {"R1", "R2", "R3"};

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create release attributes and applications for the test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        appLead = TenantUserDTO.createDefaultInstance();
        appLead.setTenant(tenantDTO.getTenantCode());
        appLead.setUserName(appLead.getUserName() + "-NoManageAccess");
        appLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        releaseAttribute1 = AttributeDTO.createDefaultInstance();
        releaseAttribute1.setAttributeType(FodCustomTypes.AttributeType.Release);
        releaseAttribute2 = AttributeDTO.createDefaultInstance();
        releaseAttribute2.setAttributeType(FodCustomTypes.AttributeType.Release);
        releaseAttribute2.setRequired(true);
        releaseAttribute3 = AttributeDTO.createDefaultInstance();
        releaseAttribute3.setAttributeType(FodCustomTypes.AttributeType.Release);
        releaseAttribute3.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        releaseAttribute3.setPickListValues(releasePickListValues);

        releaseAttributesMap = new HashMap<>() {{
            put(releaseAttribute3, releasePickListValues[0]);
        }};
        webAppWithRelease = ApplicationDTO.createDefaultInstance();
        webAppWithRelease.setReleaseAttributesMap(releaseAttributesMap);

        releaseAttributesMap1 = new HashMap<>() {{
            put(releaseAttribute3, releasePickListValues[1]);
        }};
        webAppWithRelease1 = ApplicationDTO.createDefaultInstance();
        webAppWithRelease1.setReleaseAttributesMap(releaseAttributesMap1);

        staticScanDTO=StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/WebGoat5.0.zip");

        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        CustomAttributesActions.createCustomAttribute(releaseAttribute3, tenantDTO, true);
        ApplicationActions.createApplication(webAppWithRelease, tenantDTO, false);
        ApplicationActions.createApplication(webAppWithRelease1, tenantDTO, false);
        StaticScanActions.importScanTenant(webAppWithRelease, "payloads/fod/static.java.fpr");
        StaticScanActions.importScanTenant(webAppWithRelease1, "payloads/fod/static.java.fpr");
        TenantUserActions.createTenantUsers(tenantDTO, appLead);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration()
                .openUsers().findWithSearchBox(appLead.getUserName())
                .pressAssignApplicationsByUser(appLead.getUserName())
                .selectAssignAllCheckbox()
                .pressSave();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify creation of release attributes in attributes page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void attributesPageValidation() {
        var attributesPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration()
                .openSettings().openAttributesTab();
        var attributePopup = attributesPage.addAttribute();
        attributePopup.setName(releaseAttribute1.getAttributeName())
                .setAttributeType(releaseAttribute1.getAttributeType());
        assertThat(attributePopup.getAllDataTypes())
                .as("Verify 5 Data Types are visible for Release Attribute Type")
                .contains("Picklist", "Text", "Boolean", "Date", "User");
        attributePopup.setDataType(FodCustomTypes.AttributeDataType.Text).pressSave();

        AllureReportUtil.info("New release attribute should be added in Attributes Page");
        assertThat(attributesPage.getAttributeTable().getAllColumnValues(0))
                .as("Verify the newly created release attribute should show in the attributes list")
                .contains(releaseAttribute1.getAttributeName());
        assertThat(attributesPage.getAttributeByName(releaseAttribute1.getAttributeName()).getAttributeType())
                .as("Verify Attribute Type column would have Release for any Release Attribute types added")
                .contains("Release");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify release  attributes in application creation wizard")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyReleaseAttributesInCreateApplicationWizard() {
        AllureReportUtil.info("Verify if Release attribute is set as Required,application creation should not happen " +
                "without setting the release attribute value");
        var appDTO = ApplicationDTO.createDefaultInstance();
        CustomAttributesActions.createCustomAttribute(releaseAttribute2, tenantDTO, true);
        var createApplicationPopup = new TenantTopNavbar().openApplications().pressAddNewApplicationBtn();
        createApplicationPopup.setApplicationName(appDTO.getApplicationName())
                .chooseBusinessCriticality(FodCustomTypes.BusinessCriticality.High)
                .chooseApplicationType(FodCustomTypes.AppType.Web)
                .pressNextButton()
                .setReleaseName(appDTO.getReleaseName())
                .chooseSdlcStatus(appDTO.getSdlcStatus())
                .chooseOwner(appDTO.getOwner())
                .pressNextButton();
        assertThat(createApplicationPopup.getAllSteps().contains("Release Attributes"))
                .as("Release Attributes page would appear when creating a new application")
                .isTrue();
        createApplicationPopup.pressNextButton();
        if (createApplicationPopup.getAllSteps().contains("User Groups")) {
            createApplicationPopup.pressNextButton();
        }
        assertThat(createApplicationPopup.releaseAttributeSummaryContainer.exists())
                .as("Application creation page should show the Release Attributes section")
                .isTrue();
        createApplicationPopup.pressSaveButton();

        assertThat(createApplicationPopup.errorBlock.exists())
                .as("Error Block should be displayed if required value is not provided ")
                .isTrue();
        assertThat(createApplicationPopup.getErrorMessages())
                .as("Error message should be displayed if required value is not provided ")
                .contains("The " + releaseAttribute2.getAttributeName() + " field is required.");
        createApplicationPopup.close();
        CustomAttributesActions.deleteCustomAttribute(releaseAttribute2, tenantDTO, false);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify edit , delete functionality of release attributes")
    @Test(groups = {"regression"}, dependsOnMethods = {"attributesPageValidation"})
    public void editAndDeleteReleaseAttributes() {
        LogInActions.tamUserLogin(tenantDTO);
        var attributesPage = new TenantTopNavbar().openAdministration().openSettings().openAttributesTab();
        attributesPage.getAttributeByName(releaseAttribute3.getAttributeName()).edit().setEditableOnlyBySL(true).pressSave();

        AllureReportUtil.info("Verify delete release attribute functionality");
        attributesPage.deleteAttribute(releaseAttribute1.getAttributeName());
        Selenide.refresh();
        assertThat(attributesPage.getAttributeTable().getAllColumnValues(0))
                .as("Verify the release attribute should not show in the attributes list")
                .doesNotContain(releaseAttribute1.getAttributeName());
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify Release Attribute with checked 'Edit only by sec leads' cannot edit the attribute value.");
        var attribute =  LogInActions.tenantUserLogIn(appLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(webAppWithRelease.getApplicationName(), webAppWithRelease.getReleaseName())
                .openReleaseSettings().openReleaseAttributesTab();
        String selectedValueStatus  = attribute.getAttributeByDto(releaseAttribute3)
                .$("#fod-select-0-filtered-select-uxa-select").getAttribute("class");
        assertThat(selectedValueStatus.contains("uxa-disabled"))
                .as("The dropdown field should be disabled,user cannot edit attribute value").isTrue();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify  release attributes in Data exports")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyReleaseAttributesInDataExports() {
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        AllureReportUtil.info("Verify columns and associated values would be included on the Data Exports for any Release Attributes");
        LogInActions.tamUserLogin(tenantDTO);
        var dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.ApplicationReleases);
        var fileGeneratedFromReportsPage = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv1 = new CSVHelper(fileGeneratedFromReportsPage).getColumnHeaders();
        assertThat(dataFromCsv1)
                .as("Validate csv file should have a column for release attribute")
                .contains(releaseAttribute3.getAttributeName());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify  release attributes in filters of your release page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyReleaseAttributesFilterInYourReleasePage() {
        var yourReleasesPage =  LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        yourReleasesPage.filters.expandSideFiltersDrawer().collapseAllFilters();
        assertThat(yourReleasesPage.filters.getAllFilters())
                .as("Filters should contain release attribute  values if type is picklist")
                .contains(releaseAttribute3.getAttributeName());
        yourReleasesPage.filters.setFilterByName(releaseAttribute3.getAttributeName()).expand()
                .clickFilterOptionByName(releasePickListValues[0]);
        assertThat(yourReleasesPage.getAllReleases()
                .stream().filter(x -> x.getReleaseName().equals(webAppWithRelease.getReleaseName()))
                .findFirst())
                .as("Filtering by release attribute " + webAppWithRelease.getReleaseName()
                        + " should present!")
                .isPresent();
    }

    @AfterClass
    public void cleanupAttribute() {
        AllureReportUtil.info("Custom attributes cleanup in case of failure");
        Optional.ofNullable(releaseAttribute3)
                .ifPresent(attr -> CustomAttributesActions.deleteCustomAttribute(attr, tenantDTO, true));
    }
}
