package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.RadioButton;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("549001")
@Slf4j
public class DynamicScanCopyStateScanCancelScanTest extends FodBaseTest {
    DynamicScanDTO dynamicScanWADTO, dynamicScanPlusWADTO, dynamicScanWSADTO, dynamicScanPlusWSADTO;
    TenantDTO tenantDTO;
    ApplicationDTO webAppDTO, secondWebAppDTO, thirdWebAppDTO, fourthWebAppDTO;
    AssessmentTypesDTO assessmentTypesDTO;

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
    public String dwsaWebServiceDocument = "payloads/fod/example-2.json";
    public String dwsaWebServiceApiKey = "sparky";
    public String dwsaWebServiceAdditionalInstructions = "Saturn is nice this time of the year.";

    //Dynamic PLUS Web Service Assessment
    public String dpwsaWebServiceDocument = "payloads/fod/example-2.json";
    public String dpwsaAdditionalAuthInstructions = "Login from Stark tower";
    public String dpwsaExcludedUrlContains = "bread";
    public String dpwsaNetworkUserName = "stark";
    public String dpwsaNetworkPassword = "ironman";
    public String dpwsaAdditionalNotes = "Assemble the Avengers if necessary";


    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        tenantDTO = TenantDTO.createDefaultInstance();
        webAppDTO = ApplicationDTO.createDefaultInstance();
        secondWebAppDTO = ApplicationDTO.createDefaultInstance();
        thirdWebAppDTO = ApplicationDTO.createDefaultInstance();
        fourthWebAppDTO = ApplicationDTO.createDefaultInstance();

        tenantDTO.setOptionsToEnable(new String[]{"Disable automatic dynamic scanning"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(webAppDTO, tenantDTO, true);
        ApplicationActions.createApplication(secondWebAppDTO, tenantDTO, false);
        ApplicationActions.createApplication(thirdWebAppDTO, tenantDTO, false);
        ApplicationActions.createApplication(fourthWebAppDTO, tenantDTO, false);
        BrowserUtil.clearCookiesLogOff();

        assessmentTypesDTO = AssessmentTypesDTO.createDefaultInstance();
        assessmentTypesDTO.setAssessmentTypeName("Dynamic APIs Assessment" + UniqueRunTag.generate());
        assessmentTypesDTO.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        assessmentTypesDTO.setAssessmentCategory("Dynamic APIs");
        assessmentTypesDTO.setWorkflow("Dynamic Custom");
        assessmentTypesDTO.setRemediationWorkflow("Dynamic Custom");
        assessmentTypesDTO.setTenantToAssign(tenantDTO.getTenantCode());

        var topNav = AssessmentTypesActions.createAssessmentType(assessmentTypesDTO, true)
                .adminTopNavbar;

        topNav.openTenants()
                .findWithSearchBox(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openAssessmentTypes()
                .selectAllowSingleScan(assessmentTypesDTO.getAssessmentTypeName())
                .selectAllowSubscription(assessmentTypesDTO.getAssessmentTypeName())
                .selectAllowSingleScan("Dynamic Plus APIs")
                .selectAllowSubscription("Dynamic Plus APIs")
                .selectAllowSingleScan("Dynamic+ Website Assessment")
                .selectAllowSubscription("Dynamic+ Website Assessment");
    }

    @MaxRetryCount(1)
    @Description("Admin and TAM-User should be able to create and complete Dynamic Scans for various assessment types")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})

    public void prepareScans() {

        dynamicScanWADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanWADTO.setAssessmentType("Dynamic Website Assessment");
        dynamicScanWADTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanWADTO.setAdditionalAuthenticationInstructions(dwaAdditionalAuthInstructions);
        dynamicScanWADTO.setExcludeUrl(dwaExcludedUrlContains);
        dynamicScanWADTO.setAdditionalNotes(dwaAdditionalNotes);
        dynamicScanWADTO.setAdditionalDocumentationFile(dwaAdditionalDocumentationFile);
        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanWADTO, webAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(webAppDTO.getReleaseName(), "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(webAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

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
        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanPlusWADTO, secondWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(secondWebAppDTO.getReleaseName(),
                "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(secondWebAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

        dynamicScanWSADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanWSADTO.setAssessmentType(assessmentTypesDTO.getAssessmentTypeName());
        dynamicScanWSADTO.setDynamicSiteUrl(null);
        dynamicScanWSADTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.OPEN_API);
        dynamicScanWSADTO.setWebServiceDocument(dwsaWebServiceDocument);
        dynamicScanWSADTO.setWebServiceAPIKey(dwsaWebServiceApiKey);
        dynamicScanWSADTO.setWebServiceAdditionalInstructions(dwsaWebServiceAdditionalInstructions);
        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanWSADTO, thirdWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(thirdWebAppDTO.getReleaseName(), "payloads/fod/DynSuper1.fpr",
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(thirdWebAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

        dynamicScanPlusWSADTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanPlusWSADTO.setAssessmentType("Dynamic Plus APIs");
        dynamicScanPlusWSADTO.setWebServicesRequired(true);
        dynamicScanPlusWSADTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.REST);
        dynamicScanPlusWSADTO.setWebServiceDocument(dpwsaWebServiceDocument);
        dynamicScanPlusWSADTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanPlusWSADTO.setAdditionalAuthenticationInstructions(dpwsaAdditionalAuthInstructions);
        dynamicScanPlusWSADTO.setExcludeUrl(dpwsaExcludedUrlContains);
        dynamicScanPlusWSADTO.setNetworkAuthenticationRequired(true);
        dynamicScanPlusWSADTO.setNetworkUsername(dpwsaNetworkUserName);
        dynamicScanPlusWSADTO.setNetworkPassword(dpwsaNetworkPassword);
        dynamicScanPlusWSADTO.setAdditionalNotes(dpwsaAdditionalNotes);

        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(dynamicScanPlusWSADTO, fourthWebAppDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(fourthWebAppDTO.getReleaseName(),
                "payloads/fod/DynSuper1.fpr",
                false, false, true, false, true);
        DynamicScanActions.completeDynamicScanAdmin(fourthWebAppDTO, false);
        BrowserUtil.clearCookiesLogOff();

    }

    @MaxRetryCount(3)
    @Description("TAM-User should be able to check previous scan setup pages")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareScans"})
    public void verifyPostScanFieldsAvailabilityTest() {

        //Validating Dynamic Website Assessment scan and its copy
        var soft = new SoftAssertions();
        var checkDWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(webAppDTO.getApplicationName(), webAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDWsAWrites.getDynamicSiteUrlInput().isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        var authPanel = checkDWsAWrites.getAuthenticationPanel().expand();
        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be enabled")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be enabled after scan")
                .isTrue();
        soft.assertThat(checkDWsAWrites.getScopePanel().expand().getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();

        var additionalDetailsPanel = checkDWsAWrites.getAdditionalDetailsPanel().expand();

        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic+ Website Assessment scan and its copy
        var checkDPlusWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), secondWebAppDTO.getReleaseName())
                .pressStartDynamicScan();

        soft.assertThat(checkDPlusWsAWrites.getDynamicSiteUrlInput().isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        authPanel = checkDWsAWrites.getAuthenticationPanel().expand();

        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be enabled")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be DISABLED after scan")
                .isTrue();
        soft.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                .as("Network Authentication Required checkbox should be Enabled after scan")
                .isFalse();
        soft.assertThat(authPanel.getNetworkUsernameInput().isEnabled())
                .as("Network User Name field should be writeable")
                .isTrue();
        soft.assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should disabled and still be writeable only if edit icon is clicked")
                .isFalse();
        soft.assertAll();

        var scopePanel = checkDPlusWsAWrites.getScopePanel().expand();

        soft.assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();
        soft.assertThat(scopePanel.getIncludeUrlsInput().isEnabled())
                .as("Included URLs field should not be writeable after scan")
                .isFalse();

        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic APIs Assessment scan and its copy
        var checkDWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(thirdWebAppDTO.getApplicationName(), thirdWebAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDWSAWrites.getDynamicSiteUrlInput().isDisplayed())
                .as("For Dynamic APIs assessment, URL field should not be visible")
                .isFalse();

        var apiPanel = checkDWSAWrites.getApiPanel().expand();

        soft.assertThat(apiPanel.getApiTypeDropdown().isDisabled())
                .as("Drop down should be DISABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiKeyInput().isEnabled())
                .as("Web Service API Key should be writeable")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Web Services Additional Instructions field should be DISABLED")
                .isFalse();
        apiPanel.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE)
                .setIntrospectionFileUploadType(FodCustomTypes.DynamicScanIntrospectionFileUploadType.UPLOAD_NEW_FILE);
        soft.assertThat(new RadioButton(FodCustomTypes.DynamicScanIntrospectionFileUploadType.UPLOAD_NEW_FILE.getTypeValue()).isChecked())
                .as("Upload Button should be Disabled after scan")
                .isFalse();
        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic Plus APIs scan and its copy
        var checkDPlusWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(fourthWebAppDTO.getApplicationName(), fourthWebAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDPlusWSAWrites.getDynamicSiteUrlInput().input.isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        apiPanel = checkDPlusWSAWrites.getApiPanel().expand();
        authPanel = checkDPlusWsAWrites.getAuthenticationPanel().expand();
        scopePanel = checkDPlusWsAWrites.getScopePanel().expand();
        additionalDetailsPanel = checkDPlusWsAWrites.getAdditionalDetailsPanel().expand();

        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instruction checkbox should be enabled after scan")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Instructions text field should be DISABLED after scan")
                .isTrue();
        soft.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                .as("Network Authentication Required checkbox should be Enabled after scan")
                .isFalse();
        soft.assertThat(authPanel.getNetworkUsernameInput().isEnabled())
                .as("Network User Name field should be writeable")
                .isTrue();
        soft.assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should disabled and still be writeable only if edit icon is clicked")
                .isFalse();
        soft.assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();
        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Additional Instructions input under Web Services should be DISABLED after scan")
                .isFalse();
        soft.assertThat(apiPanel.getWebServiceDocumentUploadButton().isEnabled())
                .as("Web Service Upload Document Button should be ENABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getApiCheckbox().isDisabled())
                .as("Web Services Check box should be DISABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiKeyInput().isEnabled())
                .as("Web Services Api Key should be enabled after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiPasswordInput().isEnabled())
                .as("Web Services API Key Password should be ENABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceUsernameInput().isEnabled())
                .as("Web Services API username should be enabled after scan")
                .isTrue();
        soft.assertAll();

    }

    @MaxRetryCount(3)
    @Description("TAM-User should be run scan on existing release and then cancel it ")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyPostScanFieldsAvailabilityTest"})
    public void runAndCancelScans() {


        runScanCheckDynamicScanStatus(webAppDTO.getApplicationName(), webAppDTO.getReleaseName(), "In Progress");
        runScanCheckDynamicScanStatus(secondWebAppDTO.getApplicationName(), secondWebAppDTO.getReleaseName(), "In Progress");
        runScanCheckDynamicScanStatus(thirdWebAppDTO.getApplicationName(), thirdWebAppDTO.getReleaseName(), "In Progress");
        runScanCheckDynamicScanStatus(fourthWebAppDTO.getApplicationName(), fourthWebAppDTO.getReleaseName(), "In Progress");


        DynamicScanActions.cancelDynamicScanAdmin(webAppDTO, true, false);
        DynamicScanActions.cancelDynamicScanAdmin(secondWebAppDTO, false, false);
        DynamicScanActions.cancelDynamicScanAdmin(thirdWebAppDTO, false, false);
        DynamicScanActions.cancelDynamicScanAdmin(fourthWebAppDTO, false, false);

    }

    @MaxRetryCount(3)
    @Description("TAM-User should be able to check scan setup pages after scan is cancelled")
    @Test(groups = {"regression"}, dependsOnMethods = {"runAndCancelScans"})
    public void verifyCancelledScanDataTests() {
        //Validating Dynamic Website Assessment scan and its copy
        var soft = new SoftAssertions();
        var checkDWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(webAppDTO.getApplicationName(), webAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDWsAWrites.getDynamicSiteUrlInput().input.isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        var authPanel = checkDWsAWrites.getAuthenticationPanel().expand();
        var scopePanel = checkDWsAWrites.getScopePanel().expand();
        var additionalDetailsPanel = checkDWsAWrites.getAdditionalDetailsPanel().expand();

        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be Enabled")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be DISABLED after scan")
                .isTrue();
        soft.assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();
        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic+ Website Assessment scan and its copy
        var checkDPlusWsAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(secondWebAppDTO.getApplicationName(), secondWebAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDPlusWsAWrites.getDynamicSiteUrlInput().input.isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        authPanel = checkDWsAWrites.getAuthenticationPanel().expand();
        scopePanel = checkDWsAWrites.getScopePanel().expand();
        additionalDetailsPanel = checkDWsAWrites.getAdditionalDetailsPanel().expand();

        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instructions checkbox should be Enable")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Input field should be DISABLED after scan")
                .isTrue();
        soft.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                .as("Network Authentication Required checkbox should be Enabled after scan")
                .isFalse();
        soft.assertThat(authPanel.getNetworkUsernameInput().isEnabled())
                .as("Network User Name field should be writeable")
                .isTrue();
        soft.assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should disabled and still be writeable only if edit icon is clicked")
                .isFalse();
        soft.assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();
        soft.assertThat(scopePanel.getIncludeUrlsInput().isEnabled())
                .as("Included URLs field should not be writeable after scan")
                .isFalse();
        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic APIs Assessment scan and its copy
        var checkDWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(thirdWebAppDTO.getApplicationName(), thirdWebAppDTO.getReleaseName())
                .pressStartDynamicScan();
        soft.assertThat(checkDWSAWrites.getDynamicSiteUrlInput().input.isDisplayed())
                .as("For Dynamic APIs assessment, URL field should not be visible")
                .isFalse();

        var apiPanel = checkDWsAWrites.getApiPanel().expand();

        soft.assertThat(apiPanel.getApiTypeDropdown().isDisabled())
                .as("Drop down should be DISABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiKeyInput().isEnabled())
                .as("Web Service API Key should be writeable")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Web Services Additional Instructions field should be DISABLED")
                .isFalse();
        apiPanel.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.FILE)
                .setIntrospectionFileUploadType(FodCustomTypes.DynamicScanIntrospectionFileUploadType.UPLOAD_NEW_FILE);
        soft.assertThat(new RadioButton(FodCustomTypes.DynamicScanIntrospectionFileUploadType.UPLOAD_NEW_FILE.getTypeValue()).isChecked())
                .as("Upload Button should be Disabled after scan")
                .isFalse();

        soft.assertAll();
        BrowserUtil.clearCookiesLogOff();

        //Validating Dynamic Plus APIs scan
        var checkDPlusWSAWrites = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(fourthWebAppDTO.getApplicationName(), fourthWebAppDTO.getReleaseName())
                .pressStartDynamicScan();
        assertThat(checkDPlusWSAWrites.getDynamicSiteUrlInput().input.isEnabled())
                .as("Dynamic site url field should be DISABLED after scan")
                .isFalse();

        authPanel = checkDWsAWrites.getAuthenticationPanel().expand();
        scopePanel = checkDWsAWrites.getScopePanel().expand();
        additionalDetailsPanel = checkDWsAWrites.getAdditionalDetailsPanel().expand();
        apiPanel = checkDWsAWrites.getApiPanel().expand();

        soft.assertThat(authPanel.getAdditionalAuthInstrCheckbox().isDisabled())
                .as("Additional Authentication Instruction checkbox should be ENABLED after scan")
                .isFalse();
        soft.assertThat(authPanel.getAddAuthInstrInput().isEnabled())
                .as("Additional Authentication Instructions text field should be DISABLED after scan")
                .isTrue();
        soft.assertThat(authPanel.getNetworkAuthCheckbox().isDisabled())
                .as("Network Authentication Required checkbox should be Enabled after scan")
                .isFalse();
        soft.assertThat(authPanel.getNetworkUsernameInput().isEnabled())
                .as("Network User Name field should be writeable")
                .isTrue();
        soft.assertThat(authPanel.getNetworkPasswordInput().isEnabled())
                .as("Network Password Field should disabled and still be writeable only if edit icon is clicked")
                .isFalse();
        soft.assertThat(scopePanel.getExcludeUrlsInput().isEnabled())
                .as("Excluded URls field should not be writable after scan")
                .isFalse();
        soft.assertThat(additionalDetailsPanel.getAdditionalNotes().isEnabled())
                .as("Additional Notes under Additional Details should still be enabled after scan")
                .isTrue();
        soft.assertThat(additionalDetailsPanel.getAdditionalDocumentationUploadButton().isEnabled())
                .as("Upload Button under Additional Documentation should still be enabled after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceAdditionalInstructionsArea().isEnabled())
                .as("Additional Instructions input under Web Services should be DISABLED after scan")
                .isFalse();
        soft.assertThat(apiPanel.getWebServiceDocumentUploadButton().isEnabled())
                .as("Web Service Upload Document Button should be ENABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getApiCheckbox().isDisabled())
                .as("Web Services Check box should be DISABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiKeyInput().isEnabled())
                .as("Web Services Api Key should be enabled after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceApiPasswordInput().isEnabled())
                .as("Web Services API Key Password should be ENABLED after scan")
                .isTrue();
        soft.assertThat(apiPanel.getWebServiceUsernameInput().isEnabled())
                .as("Web Services API username should be enabled after scan")
                .isTrue();

        soft.assertAll();
    }

    public void runScanCheckDynamicScanStatus(String applicationName, String releaseName, String waitStatus) {
        var scanSetupPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(applicationName, releaseName)
                .pressStartDynamicScan();

        if (scanSetupPage.getScanStatusFromIcon()
                .contains(FodCustomTypes.SetupScanPageStatus.InProgress.getTypeValue())) {
            log.info("Scan already in progress");
        } else {
            scanSetupPage
                    .pressStartScanBtn()
                    .pressNextButtonUntilStartAvailable()
                    .pressStartButton();
        }

        BrowserUtil.clearCookiesLogOff();

        var releaseDetailsPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(applicationName, releaseName);
        Supplier sup = () -> releaseDetailsPage.getDynamicScanStatus().contains(waitStatus);
        WaitUtil.waitFor(WaitUtil.Operator.Equals, true, sup, Duration.ofSeconds(120), true);
        assertThat(releaseDetailsPage.getDynamicScanStatus())
                .as("Dynamic scan status is: " + releaseDetailsPage.getDynamicScanStatus()
                        + " it should be " + waitStatus).isEqualTo(waitStatus);
        BrowserUtil.clearCookiesLogOff();

    }

}
