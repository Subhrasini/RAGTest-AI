package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.custom_types.FodCustomTypes.ApplicationType;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.dynamic_scan_setup.DynamicScanSetupPage;
import com.fortify.fod.ui.pages.tenant.applications.release.static_scan_setup.StaticScanSetupPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import static com.fortify.fod.api.utils.FodApiActions.init;
import static org.assertj.core.api.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_OK;

@Slf4j
public class StaticDynamicScanSetUpPageTest extends FodBaseTest {
    Integer appID,appID1;
    Integer releaseID,releaseID1;
    Random rand = new Random();
    FodApiActions apiActions;

    @Owner("yliben@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("552005")
    @Description("Validate static and dynamic setup page assessment types and entitlements UI/API")
    @Test(groups = {"regression"})
    public void staticDynamicSetupPageValidationTest() {
        apiActions = init(new UserPayload(defaultTenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                defaultTenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
        ApplicationPayload appPayload = ApplicationPayload.getInstanceForTenantByName(defaultTenantDTO.getTenantName());
        appPayload.setApplicationType(ApplicationType.WebThickClient.getTypeValue());
        appPayload.setOwnerId(apiActions.getCurrentSL().getUserId());
        appID = apiActions.getApplicationsApiProvider()
                .createApplication(appPayload).jsonPath().getInt("applicationId");
        releaseID = apiActions.getApplicationsApiProvider()
                .getApplicationReleases(appID).jsonPath().getInt("items[0].releaseId");

        AllureReportUtil.info("Static scan setup validation UI and API");
        var startStaticScanPopup = LogInActions.tamUserLogin(defaultTenantDTO);
        startStaticScanPopup.openYourReleases()
                .openDetailsForRelease(appPayload.getApplicationName(), appPayload.getReleaseName())
                .openStaticScanSetup()
                .chooseAssessmentType("Static Standard")
                .chooseEntitlement("Subscription");

        AllureReportUtil.info("Get assessment and entitlement values from UI");
        var staticSetup = new StaticScanSetupPage();
        var assessmentTypeTypeUIValue = staticSetup.getAssessmentTypeOptions();

        String staticEntitlementUIValue = staticSetup.getEntitlementSelectedValue();

        AllureReportUtil.info("API call to get Assessment and entitlement values for static scan");
        Response staticAssessmentType = apiActions.getReleasesApiProvider()
                .getAssessmentTypes(releaseID, FodCustomTypes.ScanType.Static);
        staticAssessmentType.then()
                .assertThat()
                .statusCode(HTTP_OK);
        Assert.assertTrue(staticAssessmentType.jsonPath().getInt("totalCount") > 0);

        List staticAssessmentTypeTypeAPIValue = staticAssessmentType.jsonPath().getList("items.name")
                .stream().distinct().collect(Collectors.toList());
        Integer randomId = rand.nextInt(staticAssessmentType.jsonPath().getList("items").size());
        String staticEntitlementAPIValue = staticAssessmentType.jsonPath().getString(String.format("items[%d].entitlementId", randomId));

        AllureReportUtil.info("Verify static scan setup  UI and API assessment type and entitlement values");
        for (int i = 0; i < staticAssessmentTypeTypeAPIValue.size(); i++) {
            Assert.assertTrue(staticAssessmentTypeTypeAPIValue.get(i).equals(assessmentTypeTypeUIValue.get(i + 1)));
        }
        Assert.assertEquals(staticEntitlementUIValue, staticEntitlementAPIValue);


        AllureReportUtil.info("Dynamic scan setup");
        var dy = staticSetup.openDynamicScanSetup();
        dy.openDynamicScanSetup()
                .setAssessmentType("Dynamic Standard");

        AllureReportUtil.info("Get assessment and entitlement values from UI");
        var dynamicSetup = new DynamicScanSetupPage();

        WaitUtil.waitForTrue(() -> !dynamicSetup.getAssessmentTypeDropdown().getOptionsTextValues().isEmpty(),
                Duration.ofSeconds(60), () -> dynamicSetup.openDynamicScanSetup());

        var dynamicAssessmentTypeTypeUIValue = dynamicSetup.getAssessmentTypeDropdown().getOptionsTextValues();
        String dynamicEntitlementUIValue = dynamicSetup.getEntitlementDropdown().getSelectedOption();

        AllureReportUtil.info(dynamicAssessmentTypeTypeUIValue.toString());

        AllureReportUtil.info("API call to get Assessment and entitlement values for Dynamic scan");
        Response dynamicAssessmentType = apiActions.getReleasesApiProvider()
                .getAssessmentTypes(releaseID, FodCustomTypes.ScanType.Dynamic);
        dynamicAssessmentType.then()
                .assertThat()
                .statusCode(HTTP_OK);
        Assert.assertTrue(dynamicAssessmentType.jsonPath().getInt("totalCount") > 0);

        List dynamicAssessmentTypeTypeAPIValue = dynamicAssessmentType.jsonPath().getList("items.name").
                stream().distinct().collect(Collectors.toList());
        Integer randomId2 = rand.nextInt(dynamicAssessmentType.jsonPath().getList("items").size());
        String dynamicEntitlementAPIValue = dynamicAssessmentType.jsonPath()
                .getString(String.format("items[%d].entitlementId", randomId2));


        AllureReportUtil.info("Verify static scan setup  UI and API assessment type and entitlement values");
        for (int i = 0; i < dynamicAssessmentTypeTypeAPIValue.size(); i++) {
            Assert.assertTrue(dynamicAssessmentTypeTypeAPIValue.get(i).equals(dynamicAssessmentTypeTypeUIValue.get(i + 1)));
        }
        Assert.assertTrue(dynamicEntitlementUIValue.contains(dynamicEntitlementAPIValue),
                dynamicEntitlementAPIValue + " should contains: " + dynamicEntitlementUIValue);
    }

    @Owner("sbehera3@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("738016")
    @Description("Context sensitive help link to FoD Debricked doc section on Static Scan setup page")
    @Test(groups = {"regression"})
    public void validateLinkInStaticScanSetUpPage() {

        var applicationDTO = ApplicationDTO.createDefaultInstance();

        SoftAssertions softAssertions = new SoftAssertions();
        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationActions.createApplication(applicationDTO);

        var staticScanSetupPage = new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartStaticScan();
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);

        softAssertions.assertThat(staticScanSetupPage.getSonatypeMessage())
                .as("Validation Note message should be visible")
                .contains("Note: Refer to the Debricked documentation for the list of required files " +
                        "to be submitted in the payload for a successful Debricked scan.");
        softAssertions.assertThat(staticScanSetupPage.helpLink.isDisplayed())
                .as("Validation help link is displayed after Note message")
                .isTrue();
        softAssertions.assertThat(staticScanSetupPage.helpLink.isEnabled())
                .as("Validation help link is enabled after Note message")
                .isTrue();
        softAssertions.assertThat(staticScanSetupPage.helpLink.getAttribute("href"))
                .as("Validate help link sends to the documentation page")
                .contains("/Docs/en/index.htm#cshid=1011");
        softAssertions.assertThat(staticScanSetupPage.isHelpLinkVisibleAsQuestionCircle())
                .as("Validate help link visible as question circle")
                .isTrue();
        softAssertions.assertAll();
    }

    @Owner("yliben@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1541002")
    @Description("GET Dynamic API endpoint should return the file details present in additional Details Uploaded Files")
    @Test(groups = {"regression"})
    public void validateDynamicApiFileDetails(){
        ApplicationPayload appPayload = ApplicationPayload.getInstanceForTenantByName(defaultTenantDTO.getTenantName());
        appPayload.setApplicationType(ApplicationType.WebThickClient.getTypeValue());
        appPayload.setOwnerId(apiActions.getCurrentSL().getUserId());
        appID1 = apiActions.getApplicationsApiProvider()
                .createApplication(appPayload).jsonPath().getInt("applicationId");
        releaseID1 = apiActions.getApplicationsApiProvider()
                .getApplicationReleases(appID1).jsonPath().getInt("items[0].releaseId");

        var dynamicScanDTO = DynamicScanDTO.createDefaultDynamicPlusAPIsInstance();
        var dynamicScanSetupPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(appPayload.getApplicationName(), appPayload.getReleaseName())
                .openDynamicScanSetup();

        AllureReportUtil.info("Setup dynamic scan with dynamic plus api with api type soap and upload .wsdl file" +
                "verify fileId, fileName, and createdDate by get /api/v3/releases/{releaseId}/dynamic-scans/scan-setup");
        dynamicScanSetupPage.setAssessmentType("Dynamic Plus APIs")
                        .setDynamicSiteUrl(dynamicScanDTO.getDynamicSiteUrl())
                        .setEntitlement("Single Scan")
                        .setTimeZone(dynamicScanDTO.getTimezone())
                        .setEnvironmentFacing(dynamicScanDTO.getEnvironmentalFacing())
                        .setTimeZone(dynamicScanDTO.getTimezone())
                        .setEnvironmentFacing(FodCustomTypes.EnvironmentFacing.EXTERNAL)
                        .getApiPanel().expand()
                        .enableApi(true)
                        .setApiType(FodCustomTypes.DynamicScanApiType.SOAP)
                        .uploadExpandWebServiceDocument("payloads/dast/SOAPLegacyZero.wsdl");
        dynamicScanSetupPage.pressSaveBtn();

        Response response = apiActions.getDynamicScansApiProvider()
                .getDynamicScanSetupDetails(releaseID1);
        response.then().
                assertThat().
                statusCode(HTTP_OK);

        assertThat(response.jsonPath().getInt("scanDocument[0].fileId"))
                .as("File id must not be empty")
                .isNotNull();

        assertThat(response.jsonPath().getString("scanDocument[0].fileName"))
                .as("File name should be same as uploaded file name")
                .isEqualTo("SOAPLegacyZero.wsdl");

        assertThat(response.jsonPath().getString("scanDocument[0].createdDate"))
                .as("Created Date id must not be empty")
                .isNotEmpty();


        AllureReportUtil.info("Setup dynamic scan by uploading  additional documentation for supported file types and " +
                "verify fileId, fileName, and createdDate by get /api/v3/releases/{releaseId}/dynamic-scans/scan-setup  ");
        dynamicScanSetupPage.getAdditionalDetailsPanel().expand()
                .uploadAdditionalDocumentation("payloads/fod/_test.txt");
        dynamicScanSetupPage.pressSaveBtn();

        Response response1 = apiActions.getDynamicScansApiProvider()
                .getDynamicScanSetupDetails(releaseID1);
        response1.then().
                assertThat().
                statusCode(HTTP_OK);

        assertThat(response1.jsonPath().getInt("scanDocument[1].fileId"))
                .as("File id must not be empty")
                .isNotNull();

        assertThat(response1.jsonPath().getString("scanDocument[1].fileName"))
                .as("File name should be same as uploaded file name")
                .isEqualTo("_test.txt");

        assertThat(response1.jsonPath().getString("scanDocument[1].createdDate"))
                .as("Created Date id must not be empty")
                .isNotEmpty();
    }
}