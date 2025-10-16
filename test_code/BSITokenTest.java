package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.static_scan_setup.StaticScanSetupPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONObject;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.LocalTime;
import java.util.Base64;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class BSITokenTest extends FodBaseTest {

    public ApplicationDTO webApp;
    public ApplicationDTO mobApp;
    public String assessmentType = "Static Assessment";
    public FodCustomTypes.TechnologyStack languageForWebApp = FodCustomTypes.TechnologyStack.DotNet;
    public FodCustomTypes.TechnologyStack languageForMobApp = FodCustomTypes.TechnologyStack.JAVA;
    public String languageLevelForWebApp = "3.5";
    public String languageLevelForMobApp = "1.8";
    public boolean openSourceAndThirdPartyForWebApp = true;
    public boolean openSourceAndThirdPartyForMobApp = false;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to create applications")
    @Test(groups = {"regression"})
    public void testDataPreparation() {

        webApp = ApplicationDTO.createDefaultInstance();
        mobApp = ApplicationDTO.createDefaultInstance();
        mobApp.setAppType(FodCustomTypes.AppType.Mobile);

        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);
        ApplicationActions.createApplication(mobApp, defaultTenantDTO, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to configure static scan. BSI Token should be generated and available")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void bsiTokenWithWebAppTest() {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openApplications();
        validateBsiTokenE2E(webApp, languageForWebApp, languageLevelForWebApp, openSourceAndThirdPartyForWebApp);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to configure static scan. BSI Token should be generated and available")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void bsiTokenWithMobileAppTest() {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openApplications();
        validateBsiTokenE2E(mobApp, languageForMobApp, languageLevelForMobApp, openSourceAndThirdPartyForMobApp);
    }

    private void validateBsiTokenE2E(ApplicationDTO applicationDTO, FodCustomTypes.TechnologyStack technologyStack,
                                     String languageLevel, boolean include) {

        var staticScanSetupPage = page(YourApplicationsPage.class).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .pressStartStaticScan()
                .chooseAssessmentType(assessmentType)
                .chooseEntitlement("Subscription");
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(technologyStack)
                .chooseLanguageLevel(languageLevel);
        staticScanSetupPage
                .chooseAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanSetupPage
                .enableOpenSourceScans(include)
                .pressSaveBtn();

        var startStaticScanPage = page(StaticScanSetupPage.class);
        /*need to refresh the page to get correct attributes values in the UI*/

        String token;
        String auditPreferencesIDUI;
        String releaseIDUI;
        String assessmentTypeIDUI;

        var endTime = LocalTime.now().plusMinutes(1);
        do {
            refresh();
            sleep(5000);
            token = startStaticScanPage.getBsiToken();
            auditPreferencesIDUI = startStaticScanPage.auditPreferenceDropdown
                    .shouldBe(Condition.visible)
                    .shouldBe(Condition.enabled)
                    .getAttribute("value");
            releaseIDUI = startStaticScanPage.hiddenReleaseId.shouldBe(Condition.exist).getAttribute("value");
            assessmentTypeIDUI = startStaticScanPage.assessmentTypeId.shouldBe(Condition.exist)
                    .getAttribute("value");
            if (!token.isEmpty() && auditPreferencesIDUI != null && releaseIDUI != null && assessmentTypeIDUI != null
                    && !auditPreferencesIDUI.isEmpty()
                    && !releaseIDUI.isEmpty()
                    && !assessmentTypeIDUI.isEmpty()) {
                break;
            }
        } while (LocalTime.now().isBefore(endTime));

        startStaticScanPage.getAdvancedSettingsPanel().expand();
        startStaticScanPage.getDevOpsAndIDEIntegrationPanel().expand();
        startStaticScanPage.getLegacySettingsPanel().expand();

        assertThat(auditPreferencesIDUI).isNotNull().isNotEmpty();
        String auditPreferencesUIText = startStaticScanPage.auditPreferenceDropdown.getSelectedOptionText();

        JSONObject obj = new JSONObject(new String(Base64.getUrlDecoder().decode(token)));
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(obj.get("tenantCode").toString()).as("Tenant Code")
                .isEqualTo(defaultTenantDTO.getTenantCode());
        softAssertions.assertThat(obj.get("releaseId").toString()).as("Release Id")
                .isEqualTo(releaseIDUI);

        /*I've never seen any other value except ANALYSIS_PAYLOAD so it is hardcoded*/
        softAssertions.assertThat(obj.get("payloadType").toString()).as("Payload Type")
                .isEqualTo("ANALYSIS_PAYLOAD");
        softAssertions.assertThat(obj.get("assessmentTypeId").toString()).as("Assessment Type Id")
                .isEqualTo(assessmentTypeIDUI);
        softAssertions.assertThat(obj.get("technologyType").toString()).as("Technology Type")
                .isEqualTo(technologyStack.getTypeValue());
        softAssertions.assertThat(obj.get("technologyVersion").toString()).as("Technology Version")
                .isEqualTo(languageLevel);
        softAssertions.assertThat(obj.get("auditPreference").toString()).as("Audit Preference")
                .isEqualTo(auditPreferencesUIText);
        softAssertions.assertThat(obj.get("auditPreferenceId").toString()).as("Audit Preference Id")
                .isEqualTo(auditPreferencesIDUI);
        softAssertions.assertThat(obj.get("includeOpenSourceAnalysis").toString()).
                as("Include OpenSource Analysis")
                .isEqualTo(Boolean.toString(include));
        softAssertions.assertThat(obj.get("portalUri").toString()).as("Portal Uri")
                .isNotEqualTo("null");
        softAssertions.assertThat(obj.get("apiUri").toString()).as("Api Uri")
                .isNotEqualTo("null");

        /*Express scan preferences is not used a lot of time, so Standard (ID 1) value is hardcoded in the test*/
        softAssertions.assertThat(obj.get("scanPreference").toString()).as("Scan Preference")
                .isEqualTo("Standard");
        softAssertions.assertThat(obj.get("scanPreferenceId").toString()).as("Scan Preference Id")
                .isEqualTo("1");

        softAssertions.assertAll();
    }
}