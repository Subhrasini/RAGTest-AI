package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Panel;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AssessmentTypesDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.AssessmentTypesActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class PremiumScanSettingTest extends FodBaseTest {

    private AssessmentTypesDTO webServicesAssessment, websiteAssessment, mobilePlusAssessment;
    private TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant / application / dynamic scan for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {

        log.info("<<< Creating default Tenant >>>");
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);

        log.info("<<< Creating 'AUTO-Dynamic Plus Web Services' Assessment Type >>>");
        webServicesAssessment = AssessmentTypesDTO.createDefaultInstance();
        webServicesAssessment.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        webServicesAssessment.setAssessmentCategory("Dynamic Plus APIs");
        webServicesAssessment.setWorkflow("Dynamic Premium");
        webServicesAssessment.setSlaDays("1");
        webServicesAssessment.setRemediationWorkflow("Dynamic Premium");
        webServicesAssessment.setSlaDaysRemediation("1");
        webServicesAssessment.setSingleScanUnits("1");
        webServicesAssessment.setSubscriptionUnits("1");
        webServicesAssessment.setNewTenantDefault(false);
        webServicesAssessment.setTenantToAssign(tenantDTO.getTenantCode());
        webServicesAssessment.setAllowSingleScanForTenant(true);
        webServicesAssessment.setAllowSubscriptionForTenant(true);
        AssessmentTypesActions.createAssessmentType(webServicesAssessment, false);

        log.info("<<< Creating 'Dynamic Plus Website' Assessment Type >>>");
        websiteAssessment = AssessmentTypesDTO.createDefaultInstance();
        websiteAssessment.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        websiteAssessment.setAssessmentCategory("Dynamic Plus Website");
        websiteAssessment.setWorkflow("Dynamic Premium");
        websiteAssessment.setSlaDays("1");
        websiteAssessment.setRemediationWorkflow("Dynamic Premium");
        websiteAssessment.setSlaDaysRemediation("1");
        websiteAssessment.setSingleScanUnits("1");
        websiteAssessment.setSubscriptionUnits("1");
        websiteAssessment.setNewTenantDefault(false);
        websiteAssessment.setTenantToAssign(tenantDTO.getTenantCode());
        websiteAssessment.setAllowSingleScanForTenant(true);
        websiteAssessment.setAllowSubscriptionForTenant(true);
        AssessmentTypesActions.createAssessmentType(websiteAssessment, false);

        log.info("<<< Creating 'Mobile Plus' Assessment Type >>>");
        mobilePlusAssessment = AssessmentTypesDTO.createDefaultInstance();
        mobilePlusAssessment.setAnalysisType(FodCustomTypes.AnalysisType.Mobile);
        mobilePlusAssessment.setAssessmentCategory("Mobile Plus");
        mobilePlusAssessment.setWorkflow("Mobile Premium");
        mobilePlusAssessment.setSlaDays("1");
        mobilePlusAssessment.setRemediationWorkflow("Mobile Premium");
        mobilePlusAssessment.setSlaDaysRemediation("1");
        mobilePlusAssessment.setSingleScanUnits("1");
        mobilePlusAssessment.setSubscriptionUnits("1");
        mobilePlusAssessment.setNewTenantDefault(false);
        mobilePlusAssessment.setTenantToAssign(tenantDTO.getTenantCode());
        mobilePlusAssessment.setAllowSingleScanForTenant(true);
        mobilePlusAssessment.setAllowSubscriptionForTenant(true);
        AssessmentTypesActions.createAssessmentType(mobilePlusAssessment, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Dynamic Scan Setting for selected Assessment Type")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyDynamicScanSetupPage() {

        log.info("<<< Creating default Web Application >>>");
        ApplicationDTO webApplication = ApplicationDTO.createDefaultInstance();
        webApplication.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Low);
        ApplicationActions.createApplication(webApplication, tenantDTO, true);

        log.info("<<< Verify Dynamic Scan Setup Page with 'AUTO-Dynamic Plus Web Services' Assessment Type >>>");
        var dynamicScanSetupPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(webApplication).pressStartDynamicScan();
        dynamicScanSetupPage.setAssessmentType(webServicesAssessment.getAssessmentTypeName());

        var apiPanel = dynamicScanSetupPage.getApiPanel().expand()
                .enableApi(true)
                .setApiType(FodCustomTypes.DynamicScanApiType.REST);

        assertThat(apiPanel.getWebServiceUsernameInput().getValue()).isEmpty();
        assertThat(apiPanel.getWebServiceApiPasswordInput().getValue()).isEmpty();
        assertThat(apiPanel.getWebServiceApiKeyInput().getValue()).isEmpty();
        assertThat(apiPanel.getWebServiceApiPasswordInput().getValue()).isEmpty();

        var additionalDetailsPanel = dynamicScanSetupPage.getAdditionalDetailsPanel().expand();

        assertThat(additionalDetailsPanel.getRequestPreassessmentConferenceCallCheckbox().checked()).isFalse();

        log.info("<<< Verify Dynamic Scan Setup Page with 'Dynamic Plus Website' Assessment Type >>>");
        dynamicScanSetupPage.setAssessmentType(websiteAssessment.getAssessmentTypeName());
        assertThat(additionalDetailsPanel.getRequestPreassessmentConferenceCallCheckbox().checked()).isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Mobile Scan Setting for selected Assessment Type")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void verifyMobileScanSetupPage() {

        log.info("<<< Creating default Mobile Application >>>");
        ApplicationDTO mobileApplication = ApplicationDTO.createDefaultMobileInstance();
        mobileApplication.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Low);
        ApplicationActions.createApplication(mobileApplication, tenantDTO, true);

        log.info("<<< Verify Mobile Scan Setup Page with 'Mobile Plus' Assessment Type >>>");
        var mobileScanSetupPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(mobileApplication).pressStartMobileScan();
        mobileScanSetupPage.setAssessmentType(mobilePlusAssessment.getAssessmentTypeName());
        assertThat(mobileScanSetupPage.getPlatformSection().text().trim()).contains("Application Platform");
        assertThat(mobileScanSetupPage.getTabletCheckbox().checked()).isTrue();
        assertThat(mobileScanSetupPage.getPhoneCheckbox().checked()).isFalse();
        assertThat(mobileScanSetupPage.getPrimaryUsername().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getPrimaryPassword().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getSecondaryUsername().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getSecondaryPassword().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getOtherUsername().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getOtherPassword().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getMultiFactorAuthenticationCheckbox().checked()).isFalse();
        assertThat(mobileScanSetupPage.getAccessToWebServicesCheckbox().checked()).isTrue();
        assertThat(mobileScanSetupPage.getEnvironmentAvailabilitySection().getText())
                .contains("Environment Availability");
        assertThat(mobileScanSetupPage.getUploadAdditionalDocsContainer().getText().trim()).isEqualTo("Upload");
        assertThat(mobileScanSetupPage.getAdditionalNotes().input.getValue()).isEmpty();
        assertThat(mobileScanSetupPage.getUploadedFilesSection().getText()).contains("Uploaded Files");
    }
}