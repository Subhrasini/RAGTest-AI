package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("549001")
@Slf4j
public class DynamicScanCopyStateTest extends FodBaseTest {
    DynamicScanDTO dynamicScanWADTO, dynamicScanPlusWADTO, dynamicScanWSADTO, dynamicScanPlusWSADTO;
    TenantDTO tenantDTO;
    ApplicationDTO webAppDTO, secondWebAppDTO, thirdWebAppDTO, fourthWebAppDTO;
    AssessmentTypesDTO assessmentTypesDTO;
    public String theDynamicScanURL = "http://zero.webappsecurity.com";

    //Dynamic Website Assessment
    public String dwaAdditionalAuthInstructions = "Login from Mars";
    public String dwaExcludedUrlContains = "bacon";
    public String dwaAdditionalNotes = "Here are additional notes";
    public String dwaAdditionalDocumentationFile = "payloads/fod/_test.txt";

    //Dynamic PLUS Website Assessment
    public String dpwaAdditionalAuthInstructions = "Login from Venus";
    public String dpwaExcludeUrlContains = "beans";
    public String dpwaIncludeUrlContains = "https://chili.com";
    public String dpwaNetworkUserName = "grendel";
    public String dpwaNetworkPasswordName = "beowulf";
    public String dpwaAdditionalNotes = "Here are some additional notes";
    public String dpwaAdditionalDocumentationFile = "payloads/fod/_test.txt";

    //Dynamic Web Service Assessment
    public FodCustomTypes.DynamicScanApiType dwsaWebServiceType = FodCustomTypes.DynamicScanApiType.OPEN_API;
    public String dwsaWebServiceDocument = "payloads/fod/example-2.json";
    public String dwsaWebServiceApiKey = "sparky";
    public String dwsaWebServiceAdditionalInstructions = "Saturn is nice this time of the year.";

    //Dynamic PLUS Web Service Assessment
    public FodCustomTypes.DynamicScanApiType dpwsaWebServiceType = FodCustomTypes.DynamicScanApiType.REST;
    public String dpwsaWebServiceDocument = "payloads/fod/example-2.json";
    public String dpwsaAdditionalAuthInstructions = "Login from Stark tower";
    public String dpwsaExcludedUrlContains = "bread";
    public String dpwsaNetworkUserName = "stark";
    public String dpwsaNetworkPassword = "ironman";
    public String dpwsaAdditionalNotes = "Assemble the Avengers if necessary";

    private void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Disable automatic dynamic scanning"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setAssessments(Arrays.asList("Dynamic Plus APIs", "Dynamic+ Website Assessment"));

        assessmentTypesDTO = AssessmentTypesDTO.createDefaultInstance();
        assessmentTypesDTO.setAssessmentTypeName("Dynamic APIs Assessment" + UniqueRunTag.generate());
        assessmentTypesDTO.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        assessmentTypesDTO.setAssessmentCategory("Dynamic APIs");
        assessmentTypesDTO.setWorkflow("Dynamic Custom");
        assessmentTypesDTO.setRemediationWorkflow("Dynamic Custom");
        assessmentTypesDTO.setTenantToAssign(tenantDTO.getTenantName());
        assessmentTypesDTO.setAllowSingleScanForTenant(true);
        assessmentTypesDTO.setAllowSubscriptionForTenant(true);

        webAppDTO = ApplicationDTO.createDefaultInstance();
        secondWebAppDTO = ApplicationDTO.createDefaultInstance();
        thirdWebAppDTO = ApplicationDTO.createDefaultInstance();
        fourthWebAppDTO = ApplicationDTO.createDefaultInstance();

        dynamicScanWADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanWADTO.setAssessmentType("Dynamic Website Assessment");
        dynamicScanWADTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanWADTO.setAdditionalAuthenticationInstructions(dwaAdditionalAuthInstructions);
        dynamicScanWADTO.setExcludeUrl(dwaExcludedUrlContains);
        dynamicScanWADTO.setAdditionalNotes(dwaAdditionalNotes);
        dynamicScanWADTO.setAdditionalDocumentationFile(dwaAdditionalDocumentationFile);

        dynamicScanPlusWADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanPlusWADTO.setAssessmentType("Dynamic+ Website Assessment");
        dynamicScanPlusWADTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanPlusWADTO.setAdditionalAuthenticationInstructions(dpwaAdditionalAuthInstructions);
        dynamicScanPlusWADTO.setExcludeUrl(dpwaExcludeUrlContains);
        dynamicScanPlusWADTO.setIncludeUrl(dpwaIncludeUrlContains);
        dynamicScanPlusWADTO.setNetworkAuthenticationRequired(true);
        dynamicScanPlusWADTO.setNetworkUsername(dpwaNetworkUserName);
        dynamicScanPlusWADTO.setNetworkPassword(dpwaNetworkPasswordName);
        dynamicScanPlusWADTO.setAdditionalNotes(dpwaAdditionalNotes);
        dynamicScanPlusWADTO.setAdditionalDocumentationFile(dpwaAdditionalDocumentationFile);

        dynamicScanWSADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanWSADTO.setAssessmentType(assessmentTypesDTO.getAssessmentTypeName());
        dynamicScanWSADTO.setDynamicSiteUrl(null);
        dynamicScanWSADTO.setWebServiceType(dwsaWebServiceType);
        dynamicScanWSADTO.setWebServiceDocument(dwsaWebServiceDocument);
        dynamicScanWSADTO.setWebServiceAPIKey(dwsaWebServiceApiKey);
        dynamicScanWSADTO.setWebServiceAdditionalInstructions(dwsaWebServiceAdditionalInstructions);

        dynamicScanPlusWSADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanPlusWSADTO.setAssessmentType("Dynamic Plus APIs");
        dynamicScanPlusWSADTO.setWebServicesRequired(true);
        dynamicScanPlusWSADTO.setWebServiceType(dpwsaWebServiceType);
        dynamicScanPlusWSADTO.setWebServiceDocument(dpwsaWebServiceDocument);
        dynamicScanPlusWSADTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanPlusWSADTO.setAdditionalAuthenticationInstructions(dpwsaAdditionalAuthInstructions);
        dynamicScanPlusWSADTO.setExcludeUrl(dpwsaExcludedUrlContains);
        dynamicScanPlusWSADTO.setNetworkAuthenticationRequired(true);
        dynamicScanPlusWSADTO.setNetworkUsername(dpwsaNetworkUserName);
        dynamicScanPlusWSADTO.setNetworkPassword(dpwsaNetworkPassword);
        dynamicScanPlusWSADTO.setAdditionalNotes(dpwsaAdditionalNotes);
    }

    @MaxRetryCount(2)
    @Description("Admin user should be able to create tenant on admin site")
    @Test(groups = {"regression"}, dataProvider = "applicationAndActionProvider", priority = 1)
    public void dynamicScanCopyStateTest(String dynamicScanAction) {
        prepareTestData(dynamicScanAction);
        verifyCopyStateDataTests(dynamicScanAction);
    }

    public void prepareTestData(String dynamicScanAction) {
        init();

        TenantActions.createTenant(tenantDTO, true, false);

        AssessmentTypesActions.createAssessmentType(assessmentTypesDTO, false);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO.getAssignedUser(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        ApplicationActions.createApplications(webAppDTO, secondWebAppDTO, thirdWebAppDTO, fourthWebAppDTO);

        DynamicScanActions.createDynamicScan(dynamicScanWADTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        DynamicScanActions.createDynamicScan(dynamicScanPlusWADTO, secondWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        DynamicScanActions.createDynamicScan(dynamicScanWSADTO, thirdWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        DynamicScanActions.createDynamicScan(dynamicScanPlusWSADTO, fourthWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(webAppDTO.getReleaseName(), "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.importDynamicScanAdmin(secondWebAppDTO.getReleaseName(),
                "payloads/fod/DynSuper1.fpr",
                false, false, true, false, false);
        DynamicScanActions.importDynamicScanAdmin(thirdWebAppDTO.getReleaseName(), "payloads/fod/DynSuper1.fpr",
                false, false, false, false, false);
        DynamicScanActions.importDynamicScanAdmin(fourthWebAppDTO.getReleaseName(),
                "payloads/fod/DynSuper1.fpr",
                false, false, true, false, false);

        ApplicationDTO[] apps = {webAppDTO, secondWebAppDTO, thirdWebAppDTO, fourthWebAppDTO};
        Arrays.stream(apps).forEach(app -> {
            Runnable action = "cancel".equals(dynamicScanAction) ?
                    () -> DynamicScanActions.cancelDynamicScanAdmin(app, false, false) :
                    () -> DynamicScanActions.completeDynamicScanAdmin(app, false);
            action.run();
        });
        BrowserUtil.clearCookiesLogOff();
    }

    public void verifyCopyStateDataTests(String dynamicScanAction) {
        //Validating Dynamic Website Assessment scan and its copy
        String releaseName = webAppDTO.getReleaseName();
        if (dynamicScanAction.equals("copy")) {
            releaseName = createAndCopyRelease(webAppDTO);
        }

        var checkDWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(webAppDTO.getApplicationName(), releaseName)
                .pressStartDynamicScan();

        assertThat(checkDWsAWrites.getDynamicSiteUrlInput().isEnabled())
                .as("Dynamic site url field should be enabled")
                .isTrue();

        var authPanel = checkDWsAWrites.getAuthenticationPanel().expand();
        var scopePanel = checkDWsAWrites.getScopePanel().expand();
        var additionalDetailsPanel = checkDWsAWrites.getAdditionalDetailsPanel().expand();

        assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be enabled")
                .isFalse();
        assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be enabled")
                .isTrue();
        assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URLs field should be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should be enabled")
                .isTrue();

        if (dynamicScanAction.equals("copy")) {
            assertThat(checkDWsAWrites.getDynamicSiteUrlInput().getValue())
                    .as("URL should match original scan URL")
                    .isEqualTo(theDynamicScanURL);
            assertThat(authPanel.getAddAuthInstrInput().getValue())
                    .as("Text should equal original scan setup text")
                    .isEqualTo(dwaAdditionalAuthInstructions);
            assertThat(scopePanel.getExcludeTable().rows.get(0).getText())
                    .as("Excluded Url text should match original scan setup")
                    .isEqualTo(dwaExcludedUrlContains);
            assertThat(additionalDetailsPanel.getAdditionalNotes().getValue())
                    .as("Additional notes should match original scan setup")
                    .isEqualTo(dwaAdditionalNotes);
        }
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic+ Website Assessment scan and its copy
        releaseName = secondWebAppDTO.getReleaseName();
        if (dynamicScanAction.equals("copy")) {
            releaseName = createAndCopyRelease(secondWebAppDTO);
        }

        var checkDPlusWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), releaseName)
                .pressStartDynamicScan();

        assertThat(checkDPlusWsAWrites.getDynamicSiteUrlInput().isEnabled()).
                as("Dynamic site url field should be enabled").isTrue();

        authPanel = checkDPlusWsAWrites.getAuthenticationPanel().expand();
        scopePanel = checkDPlusWsAWrites.getScopePanel().expand();
        additionalDetailsPanel = checkDPlusWsAWrites.getAdditionalDetailsPanel().expand();

        assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be enabled")
                .isFalse();
        assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be enabled")
                .isTrue();
        assertThat(authPanel.getNetworkAuthCheckbox().
                isDisabled()).as("Network Authentication Required checkbox should be enabled")
                .isFalse();
        assertThat(authPanel.getNetworkUsernameInput().isEnabled())
                .as("Network User Name field should be enabled")
                .isTrue();
        assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should be disabled and writeable if edit icon is clicked")
                .isFalse();
        assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URLs field should be enabled")
                .isTrue();
        assertThat(scopePanel.getIncludeUrlsInput().isEnabled())
                .as("Included URLs field should be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should be enabled")
                .isTrue();

        if (dynamicScanAction.equals("copy")) {
            assertThat(checkDPlusWsAWrites.getDynamicSiteUrlInput().getValue())
                    .as("URL should equal original scan URL")
                    .isEqualTo(theDynamicScanURL);
            assertThat(authPanel.getAddAuthInstrInput().getValue())
                    .as("Additional Auth instructions should match original scan input")
                    .isEqualTo(dpwaAdditionalAuthInstructions);
            assertThat(authPanel.getNetworkUsernameInput().getValue())
                    .as("User name should match original scan input")
                    .isEqualTo(dpwaNetworkUserName);
            assertThat(scopePanel.getExcludeTable().rows.get(0).getText())
                    .as("Excluded Url text should match original scan setup")
                    .isEqualTo(dpwaExcludeUrlContains);
            assertThat(scopePanel.getIncludeTable().rows.get(0).getText())
                    .as("Included Url should match input from original scan")
                    .isEqualTo(dpwaIncludeUrlContains);
            assertThat(additionalDetailsPanel.getAdditionalNotes().getValue())
                    .as("Additional notes should match those in original scan")
                    .isEqualTo(dpwaAdditionalNotes);
        }
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic APIs Assessment scan and its copy
        releaseName = thirdWebAppDTO.getReleaseName();
        if (dynamicScanAction.equals("copy")) {
            releaseName = createAndCopyRelease(thirdWebAppDTO);
        }

        var checkDWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(thirdWebAppDTO.getApplicationName(), releaseName)
                .pressStartDynamicScan();

        assertThat(checkDWSAWrites.getDynamicSiteUrlInput().isDisplayed())
                .as("For Dynamic APIs assessment, URL field should not be visible")
                .isFalse();

        var apiPanel = checkDWSAWrites.getApiPanel().expand();

        assertThat(apiPanel.getApiTypeDropdown().isDisabled())
                .as("Drop down should be enabled after scan")
                .isFalse();
        assertThat(apiPanel.getWebServiceApiKeyInput().isEnabled())
                .as("Web Service API Key should be writeable after scan")
                .isTrue();
        assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Web Services Additional Instructions field should be enabled after scan")
                .isTrue();
        apiPanel.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE)
                .setIntrospectionFileUploadType(FodCustomTypes.DynamicScanIntrospectionFileUploadType.UPLOAD_NEW_FILE);
        assertThat(apiPanel.getWebServiceDocumentUploadButton().isEnabled())
                .as("Upload Button should be enabled after scan")
                .isTrue();

        if (dynamicScanAction.equals("copy")) {
            assertThat(apiPanel.getApiTypeDropdown().getSelectedOption())
                    .as("Web Service type should match original scan")
                    .isEqualTo(dwsaWebServiceType.getTypeValue());
            assertThat(apiPanel.getWebServiceApiKeyInput().getValue())
                    .as("Web Service Api Key should match original scan")
                    .isEqualTo(dwsaWebServiceApiKey);
            assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().getValue())
                    .as("Web Services Additional instructions should match original scan instructions")
                    .isEqualTo(dwsaWebServiceAdditionalInstructions);
        }
        BrowserUtil.clearCookiesLogOff();

        // Validating Dynamic Plus APIs scan and its copy
        releaseName = fourthWebAppDTO.getReleaseName();
        if (dynamicScanAction.equals("copy")) {
            releaseName = createAndCopyRelease(fourthWebAppDTO);
        }

        var checkDPlusWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(fourthWebAppDTO.getApplicationName(), releaseName)
                .pressStartDynamicScan();

        assertThat(checkDPlusWSAWrites.getDynamicSiteUrlInput().isEnabled()).
                as("Dynamic site url field should be enabled")
                .isTrue();

        authPanel = checkDPlusWSAWrites.getAuthenticationPanel().expand();
        scopePanel = checkDPlusWSAWrites.getScopePanel().expand();
        additionalDetailsPanel = checkDPlusWSAWrites.getAdditionalDetailsPanel().expand();
        apiPanel = checkDPlusWSAWrites.getApiPanel().expand();

        assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instruction checkbox should be enabled")
                .isFalse();
        assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Instructions text field should be enabled")
                .isTrue();
        assertThat(authPanel.getNetworkAuthCheckbox().
                isDisabled())
                .as("Network Authentication Required checkbox should be enabled")
                .isFalse();
        assertThat(authPanel.getNetworkUsernameInput().
                isEnabled())
                .as("Network User Name field should be enabled")
                .isTrue();
        assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should be disabled and still be writeable if edit icon is clicked")
                .isFalse();
        assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled")
                .isTrue();
        assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().
                isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled")
                .isTrue();
        assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Additional Instructions input under Web Services should be enabled")
                .isTrue();
        assertThat(apiPanel.getApiCheckbox().isDisabled())
                .as("Web Services Check box should be enabled")
                .isFalse();
        assertThat(apiPanel.getWebServiceApiKeyInput().
                isEnabled()).as("Web Services Api Key should be enabled")
                .isTrue();
        assertThat(apiPanel.getWebServiceApiPasswordInput().isEnabled())
                .as("Web Services API Key Password should be enabled")
                .isTrue();
        assertThat(apiPanel.getWebServiceUsernameInput().
                isEnabled()).as("Web Services API username should be enabled")
                .isTrue();

        if (dynamicScanAction.equals("copy")) {
            assertThat(checkDPlusWSAWrites.getDynamicSiteUrlInput().getValue())
                    .as("Dynamic Scan URL should match original scan url")
                    .isEqualTo(theDynamicScanURL);
            assertThat(authPanel.getAddAuthInstrInput().getValue())
                    .as("Additional Authentication instructions should match original scan")
                    .isEqualTo(dpwsaAdditionalAuthInstructions);
            assertThat(authPanel.getNetworkUsernameInput().getValue())
                    .as("Network User entry should match original scan input")
                    .isEqualTo(dpwsaNetworkUserName);
            assertThat(scopePanel.getExcludeTable().rows.get(0).getText())
                    .as("Excluded Url Field should match original scan entry")
                    .isEqualTo(dpwsaExcludedUrlContains);
            assertThat(additionalDetailsPanel.getAdditionalNotes().getValue())
                    .as("Additional notes entry should match original scan")
                    .isEqualTo(dpwsaAdditionalNotes);
            assertThat(apiPanel.getApiTypeDropdown().getSelectedOption())
                    .as("Web Service Type should match original scan")
                    .isEqualTo(dpwsaWebServiceType.getTypeValue());
        }
        BrowserUtil.clearCookiesLogOff();
    }

    public String createAndCopyRelease(ApplicationDTO application) {
        var releaseName = ReleaseDTO.createDefaultInstance().getReleaseName();
        LogInActions.tamUserLogin(tenantDTO)
                .openYourApplications()
                .openDetailsFor(application.getApplicationName())
                .clickCreateNewRelease()
                .setCopyStateOn(true)
                .setReleaseName(releaseName)
                .setSdlc("Development")
                .clickNext()
                .selectReleaseToCopyFrom(application.getReleaseName())
                .clickSave();

        var waitForRelease = new TenantTopNavbar().openApplications()
                .openYourApplications()
                .openYourReleases()
                .openDetailsForApplication(application.getApplicationName());

        Supplier<Boolean> sup = () -> waitForRelease.isReleaseExists(releaseName);
        WaitUtil.waitFor(WaitUtil.Operator.Equals, true, sup, Duration.ofSeconds(120), true);
        BrowserUtil.clearCookiesLogOff();
        return releaseName;
    }

    @DataProvider(name = "applicationAndActionProvider")
    public Object[][] createApplicationAndActionData() {
        return new Object[][]{
                {"copy"},
                {"cancel"},
        };
    }
}