package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AttributeDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.CustomAttributesActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

@Owner("svpillai@opentext.com")
@Slf4j
@FodBacklogItem("1634029")
public class PicklistAttributesSearchingTest extends FodBaseTest {
    ApplicationDTO appDto;
    AttributeDTO applicationAttribute, releaseAttribute, issueAttribute, microserviceAttribute;
    String microserviceName = "Microservice 1";
    String[] appPickListValues = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};
    String[] issuePickListValues = {"I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I11"};
    String[] releasePickListValues = {"R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9", "R10", "R11"};
    String[] microservicePickListValues = {"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "M10", "M11"};

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create picklist custom attributes with minimum 10 values")
    @Test(groups = {"regression"})
    public void createCustomAttributes() {
        applicationAttribute = AttributeDTO.createDefaultInstance();
        applicationAttribute.setAttributeType(FodCustomTypes.AttributeType.Application);
        applicationAttribute.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        applicationAttribute.setPickListValues(appPickListValues);
        releaseAttribute = AttributeDTO.createDefaultInstance();
        releaseAttribute.setAttributeType(FodCustomTypes.AttributeType.Release);
        releaseAttribute.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        releaseAttribute.setPickListValues(releasePickListValues);
        issueAttribute = AttributeDTO.createDefaultInstance();
        issueAttribute.setAttributeType(FodCustomTypes.AttributeType.Issue);
        issueAttribute.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        issueAttribute.setPickListValues(issuePickListValues);
        microserviceAttribute = AttributeDTO.createDefaultInstance();
        microserviceAttribute.setAttributeType(FodCustomTypes.AttributeType.Microservice);
        microserviceAttribute.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        microserviceAttribute.setPickListValues(microservicePickListValues);

        appDto = ApplicationDTO.createDefaultInstance();
        appDto.setMicroservicesEnabled(true);
        appDto.setApplicationMicroservices(new String[]{microserviceName});
        appDto.setMicroserviceToChoose(microserviceName);

        CustomAttributesActions.createCustomAttribute(issueAttribute, defaultTenantDTO, true);
        CustomAttributesActions.createCustomAttribute(releaseAttribute, defaultTenantDTO, false);
        CustomAttributesActions.createCustomAttribute(applicationAttribute, defaultTenantDTO, false);
        CustomAttributesActions.createCustomAttribute(microserviceAttribute, defaultTenantDTO, false);
        ApplicationActions.createApplication(appDto, defaultTenantDTO, false);
        StaticScanActions.importScanTenant(appDto, "payloads/fod/static.java.fpr")
                .getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify search bar is present in Application and Microservice Attributes Page for picklist attributes")
    @Test(groups = {"regression"}, dependsOnMethods = {"createCustomAttributes"})
    public void searchBarInAppAndMicroserviceAttributesPage() {
        var softAssert = new SoftAssertions();
        var settingsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openApplications()
                .openDetailsFor(appDto.getApplicationName()).openSettings();
        softAssert.assertThat(settingsPage.openAppAttributesTab()
                        .openDropdownByAttributeName(applicationAttribute.getAttributeName())
                        .searchBox.isDisplayed())
                .as("Search bar should be appeared in the dropdown of application attributes page")
                .isTrue();
        settingsPage.closeDropdown();
        softAssert.assertThat(settingsPage.openMicroservicesTab().editMicroservice(microserviceName)
                        .openDropdownByAttributeName(microserviceAttribute.getAttributeName())
                        .searchBox.isDisplayed())
                .as("Search bar should be appeared in the dropdown of microservice attributes page")
                .isTrue();
        softAssert.assertAll();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify search bar is present in Release and Issues Attributes Page for picklist attributes")
    @Test(groups = {"regression"}, dependsOnMethods = {"createCustomAttributes"})
    public void searchBarInReleaseAndIssuesAttributesPage() {
        var softAssert = new SoftAssertions();
        var releaseSettingsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApplications().openYourReleases()
                .openDetailsForRelease(appDto).openReleaseSettings();
        softAssert.assertThat(releaseSettingsPage.openReleaseAttributesTab()
                        .openDropdownByAttributeName(releaseAttribute.getAttributeName())
                        .searchBox.isDisplayed())
                .as("Search bar should be appeared in dropdown of Release Attributes Page")
                .isTrue();
        releaseSettingsPage.closeDropdown();
        softAssert.assertThat(releaseSettingsPage.openIssues().pressAssignAttributes()
                        .openDropdownByAttributeName(issueAttribute.getAttributeName())
                        .searchBox.isDisplayed())
                .as("Search bar should be appeared in dropdown of issues page")
                .isTrue();
        softAssert.assertAll();
    }
}
