package com.fortify.fod.ui.test.regression;

import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.DynamicScansPayload;
import com.fortify.fod.api.payloads.ReleasePayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;

import static utils.api.ResponseCodes.HTTP_OK;

public class DynamicScanEndpointChangesTest extends FodBaseTest {
    FodApiActions apiActions;
    UserPayload currentTestUser;
    ApplicationPayload application;
    ReleasePayload release;
    Integer scanId;

    @BeforeClass
    public void apiActionsCreation() {
        apiActions = FodApiActions.init(new UserPayload(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD),
                defaultTenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
        currentTestUser = apiActions.getCurrentSL();
        application = apiActions.createApplication(false);
        application.setOwnerId(apiActions.getCurrentSL().getUserId());
        release = apiActions.createRelease(application.getApplicationId(), apiActions.getCurrentSL().getUserId());
    }

    @Owner("yliben@opentext.com")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dynamic Webservice assessment type cannot be alter after Dynamic scan completed")
    @Test
    @FodBacklogItem("563001")
    public void verifyAPIUserCannotAlterDynamicSetup() {
        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                release.getReleaseId(),
                FodCustomTypes.ScanType.Dynamic,
                "AUTO-DYNAMIC");
        Integer entitlementId = apiActions.getFirstEntitlementId();
        DynamicScansPayload scanSetupPayload = DynamicScansPayload.dynamicScanSetup(assessmentTypeId, entitlementId);
        scanSetupPayload.setHasFormsAuthentication(false);
        Response scanSetupResponse = apiActions.getDynamicScansApiProvider().saveDynamicScanSetup(release.getReleaseId()
                , scanSetupPayload);
        Assert.assertTrue(scanSetupResponse.jsonPath().getBoolean("success"), "scan setup not saved");

        //Start dynamic scan
        DynamicScansPayload startScanPayload = DynamicScansPayload.startDynamicScan(assessmentTypeId, entitlementId);
        Response dynamicScanResponse = apiActions.getDynamicScansApiProvider()
                .startDynamicScan(release.getReleaseId(), startScanPayload);
        dynamicScanResponse.then().
                assertThat().
                statusCode(HTTP_OK);

        //get scan Id
        scanId = dynamicScanResponse.jsonPath().getInt("scanId");

        Response scanSummaryInfoResponse = apiActions.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions.getReleasesApiProvider()
                        .getScan(release.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Scheduled") || analysisStatusType.equals("In_Progress"));

        //complete dynamic scan
        LogInActions.adminLogIn().adminTopNavbar
                .openDynamic()
                .openDetailsFor(application.getApplicationName())
                .publishScan();

        //update scan setup and verify api user can't alter after scan completed
        //try update dynamic scope setup
        scanSetupPayload.setDynamicSiteURL(FodConfig.API_SCAN_DUMMY_URL);
        scanSetupPayload.setAllowFormSubmissions(true);
        scanSetupPayload.setAllowSameHostRedirects(true);
        scanSetupPayload.setRestrictToDirectoryAndSubdirectories(false);

        Response scanSetupScopeResponse = apiActions.getDynamicScansApiProvider()
                .saveDynamicScanSetup(release.getReleaseId()
                        , scanSetupPayload);
        Assert.assertTrue(scanSetupScopeResponse.jsonPath()
                .getString("errors").contains("The submitted scan setup cannot override current scan settings"));
        Assert.assertFalse(scanSetupScopeResponse.jsonPath().getBoolean("success"), "scan setup saved");

        //try update Authentication and other setups
        scanSetupPayload.setRequiresNetworkAuthentication(true);
        scanSetupPayload.setNetworkPassword("test");
        scanSetupPayload.setNetworkUserName("netWorkUser");
        scanSetupPayload.setHasFormsAuthentication(true);
        scanSetupPayload.setPrimaryUserName("primaryUser");
        scanSetupPayload.setSecondaryUserName("secondName");
        scanSetupPayload.setPrimaryUserPassword("test");
        scanSetupPayload.setSecondaryUserPassword("test");

        Response scanSetupAuthResponse = apiActions.getDynamicScansApiProvider()
                .saveDynamicScanSetup(release.getReleaseId()
                        , scanSetupPayload);
        Assert.assertTrue(scanSetupAuthResponse.jsonPath()
                .getString("errors").contains("The submitted scan setup cannot override current scan settings"));
        Assert.assertFalse(scanSetupAuthResponse.jsonPath().getBoolean("success"), "scan setup saved");
    }
}
