package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AttributeDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class TenantApplicationAttributeTest extends FodBaseTest {

    String attributeName = null;
    final String[] pickListValues = {"0", "1", "2", "3", "4", "5", "6", "7"};
    final int count = pickListValues.length;
    final String chooseOne = "(Choose One)";
    List<String> booleanReleaseAttributeValues = Arrays.asList("True", "False");
    String attributeTextValue = RandomStringUtils.randomAlphanumeric(521);
    String updatedAttributeTextValue = RandomStringUtils.randomAlphanumeric(1024);
    HashMap<AttributeDTO, String> appAttributesMap, releaseAttributesMap;
    AttributeDTO appAttributeDTO, releaseAttributeDTO, microserviceAttributeDTO, issuesAttributeDTO;
    ApplicationDTO app;
    String microservice = "M1";
    TenantUserDTO securityLead;
    List<String> attributeNames;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a new application attribute that has less than 10 picklist values")
    @Test(groups = {"hf", "regression"})
    public void addAndEditAttributeTest() {
        AttributeDTO attributeDTO = AttributeDTO.createDefaultInstance();
        attributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        attributeDTO.setPickListValues(pickListValues);
        attributeName = attributeDTO.getAttributeName();
        CustomAttributesActions.createCustomAttribute(attributeDTO, defaultTenantDTO, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify picklist values are added to the Policy Assignment")
    @Test(dependsOnMethods = {"addAndEditAttributeTest"}, groups = {"hf", "regression"})
    public void verifyAttributeScopeTest() {

        var policyManagementPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openAdministration().openPolicyManagement();
        var editAssignmentScopePopup = policyManagementPage.pressEditScope();
        editAssignmentScopePopup.setAssignmentType(FodCustomTypes.PolicyScope.ApplicationAttribute)
                .setAttribute(attributeName).pressSave(true);
        var allPolicies = policyManagementPage.getPoliciesAssignmentDropDownLabels();
        for (int i = 0; i < count; i++) {
            assertThat(allPolicies.get(i))
                    .as("Verify picklist values are added to the Policy Assignment")
                    .isEqualTo(String.valueOf(i));
        }
        policyManagementPage.pressSave();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("421015")
    @Description("Users are able to bypass the Application Attributes scope in Policy Management")
    @Test(dependsOnMethods = {"verifyAttributeScopeTest"}, groups = {"hf", "regression"})
    public void verifyErrorMessageTest() {
        var attributeSettingsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openSettings().openAttributesTab();
        var attributeDefinitionPopup = attributeSettingsPage
                .getAttributeByName(attributeName).edit();
        for (int i = count; i < 12; i++) {
            attributeDefinitionPopup.setPicklistValueAndSave(String.valueOf(i));
        }
        attributeDefinitionPopup.pressSave();
        var modal = new ModalDialog(attributeDefinitionPopup.modalElement
                .shouldBe(Condition.visible, Duration.ofSeconds(20)));
        assertThat(attributeDefinitionPopup.modalElement.isDisplayed())
                .as("Verify error message should display when adding more than 10 picklist values for an application attribute")
                .isTrue();
        assertThat(modal.getMessage().trim())
                .as("Verify error message in the modal dialog")
                .contains("The selected attribute may not have more than 10 values");

        modal.pressClose();
        attributeDefinitionPopup.pressCancel();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1647022")
    @Description("Verify that the release boolean attribute is showing-(Choose One) while creating release for an application")
    @Test
    public void verifyReleaseAttributeListHasChooseOneTest() {
        AllureReportUtil.info("set attribute as release having boolean value");
        AttributeDTO attributeDTO = AttributeDTO.createDefaultInstance();
        attributeDTO.setAttributeType(FodCustomTypes.AttributeType.Release);
        attributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.Boolean);
        AllureReportUtil.info("Create release attribute");
        CustomAttributesActions.createCustomAttribute(attributeDTO, defaultTenantDTO, true);
        AllureReportUtil.info("Create a new application (leave Release attribute as Not Set)");
        var app = ApplicationDTO.createDefaultInstance();
        var applicationDetailsPage = ApplicationActions.createApplication(app).tenantTopNavbar.openApplications()
                .openDetailsFor(app.getApplicationName());
        AllureReportUtil.info("Click on Create new release");
        var createReleasePopup = applicationDetailsPage.clickCreateNewRelease();
        assertThat(createReleasePopup.getDesiredAttributeList(attributeDTO.getAttributeName()).getAlloptions())
                .as("Values in boolean release attribute dropdown should be true,false,choose one")
                .containsExactlyInAnyOrderElementsOf(booleanReleaseAttributeValues);
        assertThat(createReleasePopup.getDesiredAttributeList(attributeDTO.getAttributeName()).getDefaultValue()).
                as("Default selected should be (Choose One)").isEqualTo(chooseOne);
    }

    @MaxRetryCount(1)
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1636024")
    @Description("Prepare test data for text size limit test")
    @Test
    public void prepareTestDataForSizeLimitTest() {
        securityLead = TenantUserDTO.createDefaultInstance();
        securityLead.setTenant(defaultTenantDTO.getTenantCode());
        securityLead.setUserName(securityLead.getUserName() + "-SECLEAD");
        securityLead.setRole(FodCustomTypes.TenantUserRole.SecurityLead);
        LogInActions.tamUserLogin(defaultTenantDTO);
        TenantUserActions.createTenantUser(securityLead);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create Application, Release , Microservice and Issues attributes");
        appAttributeDTO = AttributeDTO.createDefaultInstance();
        appAttributeDTO.setEditableOnlyBySecurityLead(true);
        releaseAttributeDTO = AttributeDTO.createDefaultInstance();
        releaseAttributeDTO.setAttributeType(FodCustomTypes.AttributeType.Release);
        releaseAttributeDTO.setEditableOnlyBySecurityLead(true);
        microserviceAttributeDTO = AttributeDTO.createDefaultInstance();
        microserviceAttributeDTO.setAttributeType(FodCustomTypes.AttributeType.Microservice);
        microserviceAttributeDTO.setEditableOnlyBySecurityLead(true);
        issuesAttributeDTO = AttributeDTO.createDefaultInstance();
        issuesAttributeDTO.setAttributeType(FodCustomTypes.AttributeType.Issue);
        issuesAttributeDTO.setEditableOnlyBySecurityLead(true);
        CustomAttributesActions.createCustomAttribute(appAttributeDTO, defaultTenantDTO, true);
        CustomAttributesActions.createCustomAttribute(releaseAttributeDTO, defaultTenantDTO, false);
        CustomAttributesActions.createCustomAttribute(microserviceAttributeDTO, defaultTenantDTO, false);
        CustomAttributesActions.createCustomAttribute(issuesAttributeDTO, defaultTenantDTO, false);

        attributeNames = Arrays.asList(
                appAttributeDTO.getAttributeName(),
                releaseAttributeDTO.getAttributeName(),
                microserviceAttributeDTO.getAttributeName(),
                issuesAttributeDTO.getAttributeName()
        );
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create Application with application and release attributes");
        appAttributesMap = new HashMap<>() {{
            put(appAttributeDTO, attributeTextValue);
        }};
        releaseAttributesMap = new HashMap<>() {{
            put(releaseAttributeDTO, attributeTextValue);
        }};
        app = ApplicationDTO.createDefaultInstance();
        app.setMicroservicesEnabled(true);
        app.setMicroserviceToChoose(microservice);
        app.setApplicationMicroservices(new String[]{microservice});
        app.setAttributesMap(appAttributesMap);
        app.setReleaseAttributesMap(releaseAttributesMap);

        secLeadLogin();
        ApplicationActions.createApplication(app);
        StaticScanActions.importScanTenant(app, "payloads/fod/static.java.fpr");
    }

    @MaxRetryCount(1)
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1636024")
    @Description("Verify text size limit for Application and Microservice Attributes")
    @Test(dependsOnMethods = {"prepareTestDataForSizeLimitTest"})
    public void textSizeLimitForApplicationAndMicroserviceAttributes() {
        secLeadLogin();
        AllureReportUtil.info("Verify character size of application attribute on created application and values after edit");
        var settingsPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(app.getApplicationName())
                .openSettings();
        var attributesPage = settingsPage.openAppAttributesTab();

        var savedAppAttributeValue = attributesPage.getAttributeValue(appAttributeDTO);
        assertAttributeValues(savedAppAttributeValue, attributeTextValue);
        attributesPage.setAttributeValue(appAttributeDTO, updatedAttributeTextValue);
        var updatedAppValue = attributesPage.getAttributeValue(appAttributeDTO);
        assertAttributeValues(updatedAppValue, updatedAttributeTextValue);

        AllureReportUtil.info("Verify character size of microservice attribute");
        var microAttributesPage = settingsPage.openMicroservicesTab();
        microAttributesPage.editMicroservice(microservice).setAttributeName(updatedAttributeTextValue).clickSave();
        Selenide.refresh();
        var microserviceEditPopup = microAttributesPage.editMicroservice(microservice);
        String microUpdatedName = microserviceEditPopup.getAttributeValueByAttributeDTO(microserviceAttributeDTO);
        assertAttributeValues(microUpdatedName, updatedAttributeTextValue);
        microserviceEditPopup.clickClose();
    }

    @MaxRetryCount(1)
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1636024")
    @Description("Verify text size limit for Release and Issue Attributes")
    @Test(dependsOnMethods = {"prepareTestDataForSizeLimitTest"})
    public void textSizeLimitForReleaseAndIssuesAttributes() {
        AllureReportUtil.info("Verify character size of release attribute on created application and values after edit");
        secLeadLogin();
        var releaseAttributesPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(app.getApplicationName(), app.getReleaseName()).openReleaseSettings()
                .openReleaseAttributesTab();
        var savedReleaseAttributeValue = releaseAttributesPage.getAttributeValue(releaseAttributeDTO);
        assertAttributeValues(savedReleaseAttributeValue, attributeTextValue);
        releaseAttributesPage.setAttributeValue(releaseAttributeDTO, updatedAttributeTextValue);
        var updatedReleaseAttribute = releaseAttributesPage.getAttributeValue(releaseAttributeDTO);
        assertAttributeValues(updatedReleaseAttribute, updatedAttributeTextValue);

        AllureReportUtil.info("Verify character size limit of issues attribute");
        var issuesPage = releaseAttributesPage.openIssues();
        var assignAttributesPopup = issuesPage.pressAssignAttributes();
        assignAttributesPopup.setTextAttributeByLabel(issuesAttributeDTO.getAttributeName(), updatedAttributeTextValue)
                .pressSaveButton();
        assignAttributesPopup = issuesPage.pressAssignAttributes();
        String actualValue = assignAttributesPopup.getTextAttributeValueByLabel(issuesAttributeDTO.getAttributeName());
        assertAttributeValues(actualValue, updatedAttributeTextValue);
        assignAttributesPopup.clickClose();
    }

    private void assertAttributeValues(String actualValue, String expectedValue) {
        assertThat(actualValue)
                .as("The attribute value should match the input/updated value")
                .isEqualTo(expectedValue);
        assertThat(actualValue.length())
                .as("The attribute value's length should be less than or equal to 1024 characters")
                .isLessThanOrEqualTo(1024);
    }

    private void secLeadLogin() {
        LogInActions.tenantUserLogIn(securityLead.getUserName(), FodConfig.TAM_PASSWORD, securityLead.getTenant());
    }

    @Description("Delete all attributes")
    @AfterClass
    public void attributeCleanUpTest() {
        AllureReportUtil.info("Custom attributes cleanup");
        var attributesPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration()
                .openSettings()
                .openAttributesTab();
        for (String attributeToDelete : attributeNames) {
            attributesPage.deleteAttribute(attributeToDelete);
        }
    }
}