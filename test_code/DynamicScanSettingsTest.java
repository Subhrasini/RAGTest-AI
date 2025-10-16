package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.tenant.applications.release.dynamic_scan_setup.DynamicScanSetupPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("723011")
@Slf4j
public class DynamicScanSettingsTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifying Dynamic scan settings disable feature without " +
            "AllowedToAlterDynamicScanSettingsTenantIdList site setting configured for Tenant")
    @Parameters({"Assessment"})
    @Test(groups = {"regression"}, priority = 1, dataProvider = "dynamicScanSettingsData",
            dataProviderClass = DynamicScanSettingsTest.class)
    public void validateWithoutAllowedToAlterDynamicScanSettingsTest(String assessment) {
        AllureReportUtil.info("Without AllowedToAlterDynamicScanSettingsTenantIdList " +
                "site setting configured for Tenant having assessment" + assessment);
        LogInActions.adminLogIn().adminTopNavbar
                .openTenants().openTenantByName(defaultTenantDTO.getTenantName())
                .openAssessmentTypes()
                .selectAllowSingleScan(assessment)
                .selectAllowSubscription(assessment);
        SiteSettingsActions
                .setValueInSettings("AllowedToAlterDynamicScanSettingsTenantIdList", "", false);
        BrowserUtil.clearCookiesLogOff();
        verifyDynamicScanSetupPage(assessment, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Adding tenant id to the AllowedToAlterDynamicScanSettingsTenantIdList key")
    @Test(groups = {"regression"},
            dependsOnMethods = {"validateWithoutAllowedToAlterDynamicScanSettingsTest"})
    public void addTenantIdToAlterDynamicScanSettingsTest() {
        Integer tenantId = Integer.parseInt(new FodSQLUtil().getTenantIdByName(defaultTenantDTO.getTenantName()));
        SiteSettingsActions
                .setValueInSettings(
                        "AllowedToAlterDynamicScanSettingsTenantIdList",
                        tenantId.toString(),
                        true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifying Dynamic scan settings disable feature " +
            "with AllowedToAlterDynamicScanSettingsTenantIdList site setting configured for Tenant")
    @Parameters({"Assessment"})
    @Test(groups = {"regression"},
            dataProvider = "dynamicScanSettingsData",
            dependsOnMethods = {"addTenantIdToAlterDynamicScanSettingsTest"},
            dataProviderClass = DynamicScanSettingsTest.class)
    public void validateWithAllowedToAlterDynamicScanSettingsTest(String assessment) {
        AllureReportUtil.info("With AllowedToAlterDynamicScanSettingsTenantIdList " +
                "site setting configured for Tenant having assessment" + assessment);
        verifyDynamicScanSetupPage(assessment, true);
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("724023")
    @Description("Verify Dynamic Webservice for Storing API definition files at " +
            "Release level and not at Application level")
    @Test(groups = {"regression"},
            dependsOnMethods = {"validateWithAllowedToAlterDynamicScanSettingsTest"})
    public void verifyDynamicWebserviceTest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        List<String> fileName = new ArrayList<>() {{
            add("postman.json");
            add("example-6.json");
        }};
        for (var filesToValidate : fileName) {
            verifyDynamicWebservice(applicationDTO, filesToValidate);
        }
    }


    public void verifyDynamicScanSetupPage(String assessment, boolean isAllowedToAlterDynamicScanSettings) {
        var softAssertions = new SoftAssertions();
        var application = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(application, defaultTenantDTO, true);
        DynamicScanDTO dynamicScan;
        if (assessment.equals("Dynamic APIs")) {
            dynamicScan = DynamicScanDTO.createDefaultDynamicAPIsInstance();
        } else {
            dynamicScan = DynamicScanDTO.createDefaultInstance();
        }
        dynamicScan.setAssessmentType(assessment);
        DynamicScanActions.createDynamicScan(dynamicScan, application);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(application, true);
        BrowserUtil.clearCookiesLogOff();
        var dynamicScanSetupPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(application.getApplicationName(), application.getReleaseName())
                .openDynamicScanSetup();

        if (isAllowedToAlterDynamicScanSettings && assessment.equals("Dynamic APIs")) {
            var apiPanel = dynamicScanSetupPage.getApiPanel().expand();

            softAssertions.assertThat(apiPanel.getApiTypeDropdown().isDisabled())
                    .as("API type section NOT disabled").isFalse();
            softAssertions.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                    .as("Additional instruction section is enabled").isTrue();
        } else if (!isAllowedToAlterDynamicScanSettings && assessment.equals("Dynamic APIs")) {
            var apiPanel = dynamicScanSetupPage.getApiPanel().expand();

            softAssertions.assertThat(apiPanel.getApiTypeDropdown().isDisabled())
                    .as("API type section is disabled").isTrue();
            softAssertions.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                    .as("Additional instruction section is NOT enabled").isFalse();

        } else if (isAllowedToAlterDynamicScanSettings) {
            var authPanel = dynamicScanSetupPage.getAuthenticationPanel().expand();

            softAssertions.assertThat(dynamicScanSetupPage.getDynamicSiteUrlInput().isEnabled())
                    .as("Dynamic Site URL field is NOT greyed out and editable").isTrue();
            softAssertions.assertThat(dynamicScanSetupPage.getScopePanel().isDisabled())
                    .as("Scope section is NOT greyed out and editable").isFalse();
            softAssertions.assertThat(authPanel.getFormsAuthRequiredCheckbox().isDisabled())
                    .as("Forms Auth NOT disabled").isFalse();
            softAssertions.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                    .as("Network Auth NOT disabled").isFalse();
            softAssertions.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                    .as("Additional Auth NOT disabled").isFalse();
            if (assessment.equals("Dynamic+ API Assessment")) {
                softAssertions.assertThat(dynamicScanSetupPage.getApiPanel().expand().getApiCheckbox().isDisabled())
                        .as("API checkbox is not disabled").isFalse();
            }
        } else {
            var authPanel = dynamicScanSetupPage.getAuthenticationPanel().expand();

            softAssertions.assertThat(dynamicScanSetupPage.getDynamicSiteUrlInput().isEnabled())
                    .as("Dynamic Site URL field is greyed out and not editable").isFalse();
            softAssertions.assertThat(dynamicScanSetupPage.getScopePanel().isDisabled())
                    .as("Scope section is greyed out and not editable").isTrue();
            softAssertions.assertThat(authPanel.getFormsAuthRequiredCheckbox().isDisabled())
                    .as("Forms Auth is disabled").isFalse();
            softAssertions.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                    .as("Network Auth is disabled").isFalse();
            softAssertions.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                    .as("Additional Auth is disabled").isFalse();
            if (assessment.equals("Dynamic+ API Assessment")) {
                softAssertions.assertThat(dynamicScanSetupPage.getApiPanel().expand().getApiCheckbox().isDisabled())
                        .as("API checkbox is disabled").isTrue();
            }
        }
        softAssertions.assertAll();
    }

    @SneakyThrows
    public void verifyDynamicWebservice(ApplicationDTO dynamicApp, String fileName) {
        DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultDynamicAPIsInstance();
        if (fileName.equals("postman.json")) {
            dynamicScanDTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.POSTMAN);
            dynamicScanDTO.setWebServiceDocument("payloads/fod/postman.json");
            var secondReleaseDynamicApp = ReleaseDTO.createDefaultInstance();
            ReleaseActions.createRelease(dynamicApp, secondReleaseDynamicApp);
            DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                    secondReleaseDynamicApp.getReleaseName(),
                    FodCustomTypes.SetupScanPageStatus.Scheduled);
        } else {
            LogInActions.tamUserLogin(defaultTenantDTO);
            DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp,
                    FodCustomTypes.SetupScanPageStatus.Scheduled);
        }
        assertThat(page(DynamicScanSetupPage.class).getApiPanel().getTable()
                .rows.get(0).getAttribute("innerHTML"))
                .as("The Json file would be listed under the Name column")
                .contains(fileName);
        BrowserUtil.clearCookiesLogOff();
        var dynamicScanFilesPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName()).openFiles().openManifestsTab();
        assertThat(dynamicScanFilesPage
                .getTable()
                .getCellByTextAndIndex(fileName,
                        dynamicScanFilesPage.getTable().getColumnIndex("File Name"))
                .getText())
                .as("File should be displayed in the table of manifests tab")
                .contains(fileName);
        assertThat(dynamicScanFilesPage.getTable()
                .getCellByTextAndIndex("Download", 3).download().getName())
                .as("File should be downloaded with correct name")
                .isEqualTo(fileName);
        dynamicScanFilesPage.openOverview().publishScan();
        BrowserUtil.clearCookiesLogOff();
    }

    @AfterClass
    public void setDefaultConcurrentSetting() {
        setupDriver("setDefaultAllowedToAlterDynamicScanSettingsTenantIdList");
        SiteSettingsActions
                .setValueInSettings("AllowedToAlterDynamicScanSettingsTenantIdList", "", true);
        attachTestArtifacts();
    }

    @DataProvider(name = "dynamicScanSettingsData", parallel = true)
    public Object[][] dynamicScanSettingsData() {
        return new Object[][]{
                {"Dynamic+ Website Assessment"},
                {"Dynamic Website Assessment"},
                {"Dynamic+ API Assessment"},
                {"Dynamic APIs"}
        };
    }
}
