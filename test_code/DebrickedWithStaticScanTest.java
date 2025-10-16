package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.ReleasePayload;
import com.fortify.fod.api.payloads.StaticScanPayload;
import com.fortify.fod.api.payloads.UserPayload;
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
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.HashMap;
import java.util.Map;

import static utils.api.ResponseCodes.HTTP_OK;

@Owner("yliben@opentext.com")
@FodBacklogItem("631002")
@Slf4j
public class DebrickedWithStaticScanTest extends FodBaseTest {
    TenantDTO tenantDTO,tenantDTO1;
    EntitlementDTO entitlementDTO,entitlementDTO1;
    ApplicationPayload application,application1;
    ReleasePayload release,release1,release2,release3,release4,release5,release6,release7;
    EntitlementDTO sonatypeEntitlement, debrickedEntitlement;
    String sonatypeFilePath = "payloads/fod/10JavaDefects_Small(OS).zip";
    String debrickedFilePath = "payloads/fod/vuln_python_with_pipfilelock(debricked).zip";
    FodApiActions apiActions,apiActions1;
    int entitlementId;
    Integer scanId;
    Integer openSourceScanId;
    StaticScanPayload staticScanPayload;
    Map<String, Object> queryParams = new HashMap<>();

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , applications and entitlements for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Create tenants and entitlements");
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlementDTO);

        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);

        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        entitlementDTO1 = EntitlementDTO.createDefaultInstance();
        tenantDTO1 = TenantDTO.createDefaultInstance();
        tenantDTO1.setEntitlementDTO(entitlementDTO1);

        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);

        TenantActions.createTenant(tenantDTO1);
        EntitlementsActions.createEntitlements(tenantDTO1, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO1, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Get api access token for created tenant");
        apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        AllureReportUtil.info("Get api access token for created tenant1");
        apiActions1 = FodApiActions
                .init(new UserPayload(tenantDTO1.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO1.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        application = apiActions.createApplication(false);
        application.setOwnerId(apiActions.getCurrentSL().getUserId());

        AllureReportUtil.info("Create application and release for scans");
        release = apiActions.createRelease(application.getApplicationId(), apiActions.getCurrentSL().getUserId());
        release1 = apiActions.createRelease(application.getApplicationId(), apiActions.getCurrentSL().getUserId());
        release4 = apiActions.createRelease(application.getApplicationId(), apiActions.getCurrentSL().getUserId());

        application1 = apiActions1.createApplication(false);
        application1.setOwnerId(apiActions1.getCurrentSL().getUserId());

        release2 = apiActions1.createRelease(application1.getApplicationId(), apiActions1.getCurrentSL().getUserId());
        release3 = apiActions1.createRelease(application1.getApplicationId(), apiActions1.getCurrentSL().getUserId());
        release5 = apiActions1.createRelease(application1.getApplicationId(), apiActions1.getCurrentSL().getUserId());
        release6 = apiActions1.createRelease(application1.getApplicationId(), apiActions1.getCurrentSL().getUserId());
        release7 = apiActions1.createRelease(application1.getApplicationId(), apiActions1.getCurrentSL().getUserId());

        staticScanPayload = StaticScanPayload.defaultJavaScanInstance();
        queryParams.put("orderBy", "scanTypeId");
        queryParams.put("orderByDirection", "DESC");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start and completed static and Sonatype with doPenSourceScan set to true")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void sonatypeScanDOpenSourceScanTrueTest(){
        entitlementId = apiActions.getFirstEntitlementId();
        staticScanPayload.setDoOpenSourceScan(true);
        staticScanPayload.setEntitlementId(entitlementId);
        staticScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions.getStaticScansApiProvider()
                .startScan(release.getReleaseId(), staticScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(sonatypeFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Sonatype scan start and completed");
        Response scanSummaryInfoResponse = apiActions.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions.getReleasesApiProvider().getScan(release.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions.getStatusTypeOfStaticScan(release.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions.getReleasesApiProvider().getScans(release.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");

        Response scanSummaryInfoResponse1 = apiActions.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions.getReleasesApiProvider().getScan(release.getReleaseId(), openSourceScanId));
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("assessmentTypeName"),
                "Sonatype");
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("scanType"),"OpenSource");

        String statusTyp1 = apiActions.getStatusTypeOfStaticScan(release.getReleaseId(),
                openSourceScanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusTyp1, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start and completed static and debrick with doPenSourceScan set to true")
    @Test(groups = {"regression"}, dependsOnMethods = {"sonatypeScanDOpenSourceScanTrueTest"})
    public void debrickedScanDOpenSourceScanTrueTest(){
        entitlementId = apiActions1.getFirstEntitlementId();
        StaticScanPayload debrickScanPayload = new StaticScanPayload();
        debrickScanPayload.setDoOpenSourceScan(true);
        debrickScanPayload.setDoSonatypeScan(false);
        debrickScanPayload.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());
        debrickScanPayload.setAssessmentTypeId(14);
        debrickScanPayload.setLanguageLevel("2");
        debrickScanPayload.setAuditPreferenceType("Automated");
        debrickScanPayload.setEntitlementId(entitlementId);
        debrickScanPayload.setFragNo(-1);
        debrickScanPayload.setOffset(0);
        debrickScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions1.getStaticScansApiProvider()
                .startScan(release5.getReleaseId(), debrickScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(debrickedFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Debricked scan start and completed");
        Response scanSummaryInfoResponse = apiActions1.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release5.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions1.getStatusTypeOfStaticScan(release5.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions1.getReleasesApiProvider().getScans(release5.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");

        Response scanSummaryInfoResponse1 = apiActions1.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release5.getReleaseId(), openSourceScanId));
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("assessmentTypeName"),
                "Debricked");
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("scanType"),"OpenSource");

        String statusTyp1 = apiActions1.getStatusTypeOfStaticScan(release5.getReleaseId(),
                openSourceScanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusTyp1, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start and completed static and Sonatype with doSonatypeScan parameter set to true")
    @Test(groups = {"regression"}, dependsOnMethods = {"debrickedScanDOpenSourceScanTrueTest"})
    public void sonatypeDoSonatypeScanTrueTest(){
        entitlementId = apiActions.getFirstEntitlementId();
        staticScanPayload.setDoSonatypeScan(true);
        staticScanPayload.setEntitlementId(entitlementId);
        staticScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions.getStaticScansApiProvider()
                .startScan(release1.getReleaseId(), staticScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(sonatypeFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Sonatype scan start and completed");
        Response scanSummaryInfoResponse = apiActions.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions.getReleasesApiProvider().getScan(release1.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions.getStatusTypeOfStaticScan(release1.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions.getReleasesApiProvider().getScans(release1.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");

        Response scanSummaryInfoResponse1 = apiActions.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions.getReleasesApiProvider().getScan(release1.getReleaseId(), openSourceScanId));
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("assessmentTypeName"),
                "Sonatype");

        String statusTyp1 = apiActions.getStatusTypeOfStaticScan(release1.getReleaseId(),
                openSourceScanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusTyp1, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start and completed static and debrick with doSonatypeScan parameter set to true")
    @Test(groups = {"regression"}, dependsOnMethods = {"sonatypeDoSonatypeScanTrueTest"})
    public void debrickedScanDoSonatypeScanTrueTest(){
        entitlementId = apiActions1.getFirstEntitlementId();
        StaticScanPayload debrickScanPayload = new StaticScanPayload();
        debrickScanPayload.setDoOpenSourceScan(true);
        debrickScanPayload.setDoSonatypeScan(false);
        debrickScanPayload.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());
        debrickScanPayload.setAssessmentTypeId(14);
        debrickScanPayload.setLanguageLevel("2");
        debrickScanPayload.setAuditPreferenceType("Automated");
        debrickScanPayload.setEntitlementId(entitlementId);
        debrickScanPayload.setFragNo(-1);
        debrickScanPayload.setOffset(0);
        debrickScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions1.getStaticScansApiProvider()
                .startScan(release6.getReleaseId(), debrickScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(debrickedFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Debricked scan start and completed");
        Response scanSummaryInfoResponse = apiActions1.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release6.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions1.getStatusTypeOfStaticScan(release6.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 150);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions1.getReleasesApiProvider().getScans(release6.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");

        Response scanSummaryInfoResponse1 = apiActions1.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release6.getReleaseId(), openSourceScanId));
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("assessmentTypeName"),
                "Debricked");
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("scanType"),"OpenSource");

        String statusTyp1 = apiActions1.getStatusTypeOfStaticScan(release6.getReleaseId(),
                openSourceScanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusTyp1, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start static and Sonatype with doOpenSourceScan set true doSonatypeScan set false")
    @Test(groups = {"regression"}, dependsOnMethods = {"debrickedScanDoSonatypeScanTrueTest"})
    public void sonatypeScanDOpenSourceScanTrueDoSonatypeScanSetToFalseTest(){
        entitlementId = apiActions1.getFirstEntitlementId();
        StaticScanPayload debrickScanPayload = new StaticScanPayload();
        debrickScanPayload.setDoOpenSourceScan(true);
        debrickScanPayload.setDoSonatypeScan(false);
        debrickScanPayload.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());
        debrickScanPayload.setAssessmentTypeId(14);
        debrickScanPayload.setLanguageLevel("2");
        debrickScanPayload.setAuditPreferenceType("Automated");
        debrickScanPayload.setEntitlementId(entitlementId);
        debrickScanPayload.setFragNo(-1);
        debrickScanPayload.setOffset(0);
        debrickScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions1.getStaticScansApiProvider()
                .startScan(release2.getReleaseId(), debrickScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(debrickedFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Open source  scan start");
        Response scanSummaryInfoResponse = apiActions1.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release2.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions1.getStatusTypeOfStaticScan(release2.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 210);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions1.getReleasesApiProvider().getScans(release2.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");
        Assert.assertNotNull(openSourceScanId, "Default Scan id is null");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully start static and Sonatype with DoSonatypeScan set true doPenSourceScan set false")
    @Test(groups = {"regression"}, dependsOnMethods = {"sonatypeScanDOpenSourceScanTrueDoSonatypeScanSetToFalseTest"})
    public void sonatypeScanDOpenSourceScanFalseDoSonatypeScanSetToTrueTest(){
        entitlementId = apiActions1.getFirstEntitlementId();
        StaticScanPayload debrickScanPayload = new StaticScanPayload();
        debrickScanPayload.setDoOpenSourceScan(false);
        debrickScanPayload.setDoSonatypeScan(true);
        debrickScanPayload.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());
        debrickScanPayload.setAssessmentTypeId(14);
        debrickScanPayload.setLanguageLevel("2");
        debrickScanPayload.setAuditPreferenceType("Automated");
        debrickScanPayload.setEntitlementId(entitlementId);
        debrickScanPayload.setFragNo(-1);
        debrickScanPayload.setOffset(0);
        debrickScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions1.getStaticScansApiProvider()
                .startScan(release3.getReleaseId(), debrickScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(debrickedFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Open source  scan start");
        Response scanSummaryInfoResponse = apiActions1.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release3.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions1.getStatusTypeOfStaticScan(release3.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions1.getReleasesApiProvider().getScans(release3.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");
        Assert.assertNotNull(openSourceScanId, "Default Scan id is null");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Scan successfully openSource scan start when doOpenSourceScan set true DoSonatypeScan set true")
    @Test(groups = {"regression"}, dependsOnMethods = {"sonatypeScanDOpenSourceScanFalseDoSonatypeScanSetToTrueTest"})
    public void sonatypeScanDOpenSourceScanTrueDoSonatypeScanSetToTrueTest(){
        entitlementId = apiActions1.getFirstEntitlementId();
        StaticScanPayload debrickScanPayload = new StaticScanPayload();
        debrickScanPayload.setDoOpenSourceScan(true);
        debrickScanPayload.setDoSonatypeScan(true);
        debrickScanPayload.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());
        debrickScanPayload.setAssessmentTypeId(14);
        debrickScanPayload.setLanguageLevel("2");
        debrickScanPayload.setAuditPreferenceType("Automated");
        debrickScanPayload.setEntitlementId(entitlementId);
        debrickScanPayload.setFragNo(-1);
        debrickScanPayload.setOffset(0);
        debrickScanPayload.setEntitlementFrequencyType("Subscription");

        Response staticScanResponse = apiActions1.getStaticScansApiProvider()
                .startScan(release7.getReleaseId(), debrickScanPayload.objectToMap(),
                        ApiHelper.retrievePayloadFile(debrickedFilePath), StaticScansApiProvider.StaticScanRequestType.SCAN);

        staticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);

        scanId = staticScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Default Scan id is null");

        AllureReportUtil.info("Verify Open source scan start ");
        Response scanSummaryInfoResponse = apiActions1.getResponseByStatusCode(5, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release7.getReleaseId(), scanId));
        String analysisStatusType = scanSummaryInfoResponse.jsonPath().getString("analysisStatusType");
        Assert.assertTrue(analysisStatusType.equals("Queued") || analysisStatusType.equals("In_Progress"));

        String statusType = apiActions1.getStatusTypeOfStaticScan(release7.getReleaseId(),
                scanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusType, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());

        Response openSourceScan =  apiActions1.getReleasesApiProvider().getScans(release7.getReleaseId(),queryParams);
        openSourceScan.then()
                .assertThat()
                .statusCode(HTTP_OK);

        openSourceScanId = openSourceScan.jsonPath().getInt("items[0].scanId");
        Assert.assertNotNull(openSourceScanId, "Default Scan id is null");
        Assert.assertEquals(openSourceScan.jsonPath().getString("items[1].scanType"),"Static");

        Response scanSummaryInfoResponse1 = apiActions1.getResponseByStatusCode(3, HTTP_OK,
                () -> apiActions1.getReleasesApiProvider().getScan(release7.getReleaseId(), openSourceScanId));
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("assessmentTypeName"),
                "Debricked");
        Assert.assertEquals(scanSummaryInfoResponse1.jsonPath().getString("scanType"),"OpenSource");

        String statusTyp1 = apiActions1.getStatusTypeOfStaticScan(release7.getReleaseId(),
                openSourceScanId, FodCustomTypes.ScansPageStatus.Completed.getTypeValue(), 250);
        Assert.assertEquals(statusTyp1, FodCustomTypes.ScansPageStatus.Completed.getTypeValue());
    }
}
