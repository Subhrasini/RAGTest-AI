package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.*;
import com.fortify.fod.api.providers.StaticScansApiProvider;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.EntitlementsActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import java.util.HashMap;
import java.util.Map;
import static utils.api.ResponseCodes.*;

@Owner("yliben@opentext.com")
@FodBacklogItem("1694015")
@Slf4j
public class ExpiredEntitlementScanTest extends FodBaseTest {
    FodApiActions oSSTenant, tenant;
    TenantDTO tenantDTO, oSSTenantDTO;
    ApplicationPayload application1, application2, application3, application4;
    ReleasePayload release1, release2, release3, release4;
    EntitlementDTO debrickedEntitlement, sonatypeEntitlement, entitlementDTO, expiredEntitlementDTO;
    private final String debrickedFilePath = "payloads/fod/vuln_python_with_pipfilelock(debricked).zip";
    private final String sonatypeFilePath = "payloads/fod/10JavaDefects_Small(OS).zip";
    String ASSESSMENT_NAME = "AUTO-STATIC";
    private Integer technologyStackId = 7;
    private Map<String, Object> queryParams = new HashMap<>();
    private Integer scanID;
    private Integer entitlementID, oSSEntitlementID;

    @BeforeClass
    public void prepareTestData() {
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setQuantityPurchased(200);

        expiredEntitlementDTO = EntitlementDTO.createDefaultInstance();
        expiredEntitlementDTO.setQuantityPurchased(0);

        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlementDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, expiredEntitlementDTO);
        BrowserUtil.clearCookiesLogOff();

        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        oSSTenantDTO = TenantDTO.createDefaultInstance();
        oSSTenantDTO.setEntitlementDTO(debrickedEntitlement);
        oSSTenantDTO.setEntitlementDTO(sonatypeEntitlement);

        TenantActions.createTenant(oSSTenantDTO);
        EntitlementsActions.createEntitlements(oSSTenantDTO, false, sonatypeEntitlement);
        EntitlementsActions.createEntitlements(oSSTenantDTO, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(oSSTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        tenant = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        oSSTenant = FodApiActions.init(new UserPayload(oSSTenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), oSSTenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        application1 = tenant.createApplication(false);
        application2 = tenant.createApplication(false);
        application1.setOwnerId(tenant.getCurrentSL().getUserId());
        application2.setOwnerId(tenant.getCurrentSL().getUserId());
        release1 = tenant.createRelease(application1.getApplicationId(), tenant.getCurrentSL().getUserId());
        release2 = tenant.createRelease(application2.getApplicationId(), tenant.getCurrentSL().getUserId());


        application3 = oSSTenant.createApplication(false);
        application4 = oSSTenant.createApplication(false);
        application3.setOwnerId(oSSTenant.getCurrentSL().getUserId());
        application4.setOwnerId(oSSTenant.getCurrentSL().getUserId());
        release3 = oSSTenant.createRelease(application3.getApplicationId(), oSSTenant.getCurrentSL().getUserId());
        release4 = oSSTenant.createRelease(application4.getApplicationId(), oSSTenant.getCurrentSL().getUserId());
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Start defaults scan with endpoints failing to handle Expired entitlement scenario - fallbackToActiveEntitlement")
    @MaxRetryCount(2)
    @Test()
    public void startDefaultScanFallbackToActiveEntitlementTest() {
        AllureReportUtil.info("set fallbackToActiveEntitlement to false and verify all the scan should fail with expired entitlement");
        entitlementID = tenant.getTenantsApiProvider().getTenantsEntitlements().jsonPath().getInt("tenantEntitlements[1].entitlementId");

        Integer assessmentTypeId = tenant.getAssessmentIdByScanTypeAndName(release1.getReleaseId(),
                FodCustomTypes.ScanType.Static, ASSESSMENT_NAME);
        PutStaticScanSetupPayload scanSetup = PutStaticScanSetupPayload.getDefaultInstance(assessmentTypeId,
                entitlementID, technologyStackId);
        scanSetup.setLanguageLevelId(17);
        scanSetup.setAuditPreferenceType("Automated");
        scanSetup.setEntitlementFrequencyType("Subscription");

        Response saveStaticScan = tenant.getStaticScansApiProvider()
                .saveStaticScanSetupDetails(release1.getReleaseId(), scanSetup);
        saveStaticScan.then().assertThat().statusCode(HTTP_OK);

        queryParams.put("releaseId", release1.getReleaseId());
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("fallbackToActiveEntitlement", false);

        Response staticDefaultsScanResponse = tenant.getStaticScansApiProvider()
                .startDefaultScan(release1.getReleaseId(), queryParams,
                        ApiHelper.retrievePayloadFile(FodConfig.API_PATH_STATIC_SCAN_ZIP),
                        StaticScansApiProvider.StaticScanRequestType.DEFAULT_SCAN);

        staticDefaultsScanResponse.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);
        Assert.assertTrue(staticDefaultsScanResponse.jsonPath().getString("errors[0].message").
                contains("EntitlementId specified is expired. Specify a different entitlementId to proceed scanning"));


        AllureReportUtil.info("set fallbackToActiveEntitlement to true and verify all scan should  start and complete " +
                "with one active and one expired entitlement");
        queryParams.put("fallbackToActiveEntitlement", true);
        Response staticDefaultsScanResponse1 = tenant.getStaticScansApiProvider()
                .startDefaultScan(release1.getReleaseId(), queryParams,
                        ApiHelper.retrievePayloadFile(FodConfig.API_PATH_STATIC_SCAN_ZIP),
                        StaticScansApiProvider.StaticScanRequestType.DEFAULT_SCAN);

        staticDefaultsScanResponse1.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanID = staticDefaultsScanResponse1.jsonPath().getInt("scanId");
        AllureReportUtil.info("Verify default static scan start and completed with fallbackToActiveEntitlement set to true");
        Response scanSummaryInfoResponse = tenant.getResponseByStatusCode(5, HTTP_OK,
                () -> tenant.getReleasesApiProvider().getScan(release1.getReleaseId(), scanID));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = tenant.getStatusTypeOfStaticScan(release1.getReleaseId(),
                scanID, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Verify scan with defaults should fail even when available entitlements in Debricked or sonatype" +
            " and no available entitlements in fortify")
    @MaxRetryCount(2)
    @Test(dependsOnMethods = "startDefaultScanFallbackToActiveEntitlementTest")
    public void startDefaultOSSScanFallbackToActiveEntitlementTestTest() {

        Integer assessmentTypeId =
                oSSTenant.getAssessmentIdByScanTypeAndName(release3.getReleaseId(), FodCustomTypes.ScanType.Static,
                        ASSESSMENT_NAME);

        oSSEntitlementID = oSSTenant.getFirstEntitlementId();
        PutStaticScanSetupPayload scanSetupBeforeChange =
                PutStaticScanSetupPayload.getDefaultInstance(assessmentTypeId, oSSEntitlementID, 9);
        scanSetupBeforeChange.setAuditPreferenceType("Automated");
        scanSetupBeforeChange.setPerformOpenSourceAnalysis(true);

        oSSTenant.getStaticScansApiProvider()
                .saveStaticScanSetupDetails(release3.getReleaseId(), scanSetupBeforeChange).then()
                .statusCode(HTTP_OK);

        queryParams.put("releaseId", release3.getReleaseId());
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("fallbackToActiveEntitlement", true);

        Response staticScanResponse = oSSTenant.getStaticScansApiProvider()
                .startScan(release3.getReleaseId(), queryParams,
                        ApiHelper.retrievePayloadFile(sonatypeFilePath), StaticScansApiProvider.StaticScanRequestType.DEFAULT_SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);
        Assert.assertTrue(staticScanResponse.jsonPath().getString("errors[0].message").
                contains("EntitlementId specified is expired. Specify a different entitlementId to proceed scanning"));
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Start scan with endpoints failing to handle Expired entitlement scenario - fallbackToActiveEntitlement")
    @MaxRetryCount(2)
    @Test(dependsOnMethods = "startDefaultOSSScanFallbackToActiveEntitlementTestTest")
    public void startScanFallbackToActiveEntitlementTest() {
        AllureReportUtil.info("set fallbackToActiveEntitlement to false and verify all the scan should fail with expired entitlement");
        entitlementID = tenant.getTenantsApiProvider().getTenantsEntitlements().jsonPath().getInt("tenantEntitlements[1].entitlementId");

        Integer assessmentTypeId = tenant.getAssessmentIdByScanTypeAndName(release2.getReleaseId(),
                FodCustomTypes.ScanType.Static, ASSESSMENT_NAME);
        PutStaticScanSetupPayload scanSetup = PutStaticScanSetupPayload.getDefaultInstance(assessmentTypeId,
                entitlementID, technologyStackId);
        scanSetup.setLanguageLevelId(17);
        scanSetup.setAuditPreferenceType("Automated");
        scanSetup.setEntitlementFrequencyType("Subscription");

        Response saveStaticScan = tenant.getStaticScansApiProvider()
                .saveStaticScanSetupDetails(release2.getReleaseId(), scanSetup);
        saveStaticScan.then().assertThat().statusCode(HTTP_OK);

        queryParams.put("releaseId", release2.getReleaseId());
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("fallbackToActiveEntitlement", false);
        queryParams.put("assessmentTypeId", assessmentTypeId);
        queryParams.put("entitlementId",entitlementID);
        queryParams.put("entitlementFrequencyType","Subscription");
        queryParams.put("auditPreferenceType","Automated");

        Response staticDefaultsScanResponse = tenant.getStaticScansApiProvider()
                .startScan(release2.getReleaseId(), queryParams,
                        ApiHelper.retrievePayloadFile(FodConfig.API_PATH_STATIC_SCAN_ZIP),
                        StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticDefaultsScanResponse.then()
                .assertThat()
                .statusCode(HTTP_UNPROCESSABLE_ENTITY);
        Assert.assertTrue(staticDefaultsScanResponse.jsonPath().getString("errors[0].message").
                contains("EntitlementId specified is expired. Specify a different entitlementId to proceed scanning"));


        AllureReportUtil.info("set fallbackToActiveEntitlement to true and verify all scan should  start and complete " +
                "with one active and one expired entitlement");
        queryParams.put("fallbackToActiveEntitlement", true);
        Response staticDefaultsScanResponse1 = tenant.getStaticScansApiProvider()
                .startScan(release2.getReleaseId(), queryParams,
                        ApiHelper.retrievePayloadFile(FodConfig.API_PATH_STATIC_SCAN_ZIP),
                        StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticDefaultsScanResponse1.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanID = staticDefaultsScanResponse1.jsonPath().getInt("scanId");
        AllureReportUtil.info("Verify  static scan start and completed with fallbackToActiveEntitlement set to true");
        Response scanSummaryInfoResponse = tenant.getResponseByStatusCode(5, HTTP_OK,
                () -> tenant.getReleasesApiProvider().getScan(release2.getReleaseId(), scanID));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = tenant.getStatusTypeOfStaticScan(release2.getReleaseId(),
                scanID, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Verify scan  should fail even when available entitlements in Debricked or sonatype" +
            " and no available entitlements in fortify")
    @MaxRetryCount(2)
    @Test(dependsOnMethods = "startScanFallbackToActiveEntitlementTest")
    public void startOSSScanFallbackToActiveEntitlementTestTest() {
        oSSEntitlementID = oSSTenant.getFirstEntitlementId();
        Integer assessmentTypeId =
                oSSTenant.getAssessmentIdByScanTypeAndName(release4.getReleaseId(), FodCustomTypes.ScanType.Static,
                        ASSESSMENT_NAME);

        PutStaticScanSetupPayload scanSetupBeforeChange =
                PutStaticScanSetupPayload.getDefaultInstance(assessmentTypeId, oSSEntitlementID, 9);
        scanSetupBeforeChange.setPerformOpenSourceAnalysis(true);
        queryParams.put("releaseId", release4.getReleaseId());
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("fallbackToActiveEntitlement", true);

        oSSTenant.getStaticScansApiProvider()
                .saveStaticScanSetupDetails(release4.getReleaseId(), scanSetupBeforeChange).then()
                .statusCode(HTTP_OK);

        Response startOpenSourceScanResponse = oSSTenant.getStaticScansApiProvider().startScan(release4.getReleaseId(),
                queryParams, ApiHelper.retrievePayloadFile(debrickedFilePath),StaticScansApiProvider.StaticScanRequestType.SCAN);

        startOpenSourceScanResponse.then()
                .assertThat()
                .statusCode(HTTP_UNPROCESSABLE_ENTITY);
        Assert.assertTrue(startOpenSourceScanResponse.jsonPath().getString("errors[0].message").
                contains("EntitlementId specified is expired. Specify a different entitlementId to proceed scanning"));
    }
}