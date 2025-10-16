package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.*;
import com.fortify.fod.api.test.BaseFodApiTest;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.test.actions.LogInActions;
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
import java.util.List;
import java.util.Map;
import static com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi.API_TENANT;
import static com.fortify.fod.common.custom_types.FodCustomTypes.ScanType.Dynamic;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("yliben@opentext.com")
@FodBacklogItem("1662028")
@Slf4j

public class PutScanImportTest extends BaseFodApiTest {
    private ApplicationPayload application1, application2;
    private ReleasePayload release1, release2;
    private Integer staticScanId;
    private Integer dynamicScanId;
    private Integer entitlementId;
    private final String ASSESSMENT_NAME = "AUTO-STATIC";
    private final String FPR_STATIC_SCAN_FILE_PATH = "payloads/fod/net40_scan(15,27,0,12).fpr";
    private final String FPR_DYNAMIC_SCAN_FILE_PATH = "payloads/fod/dynamic.zero.fpr";
    private final String JSON_VALIDATION_PATH_DYNAMIC = "fod/JSON_schema_validation/dynamic-scans/%s";
    private final String JSON_VALIDATION_PATH_STATIC = "fod/JSON_schema_validation/scans/%s";
    @BeforeClass
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO,true,false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(),API_TENANT);

        application1 = apiActions.createApplication(false);
        application1.setOwnerId(apiActions.getCurrentSL().getUserId());
        release1 = apiActions.createRelease(application1.getApplicationId(), apiActions.getCurrentSL().getUserId());

        application2 = apiActions.createApplication(false);
        application2.setOwnerId(apiActions.getCurrentSL().getUserId());
        release2 = apiActions.createRelease(application2.getApplicationId(), apiActions.getCurrentSL().getUserId());
    }

    @Severity(SeverityLevel.CRITICAL)
    @Description("PUT scan static import and Verify ScanID along  with reference ID")
    @MaxRetryCount(2)
    @Test()
    public void putImportStaticScanTest() {
        AllureReportUtil.info("setup static scan and PUT import static scan");
        Integer technologyStackId = apiActions.getLookupItemsProvider()
                .getLookupItems("LanguageLevels").jsonPath().getInt("items[1].value");
        Assert.assertNotNull(technologyStackId, "Technology stack id is null");

        apiActions.getStaticScansApiProvider().getStaticScanSetupDetails(release1.getReleaseId()).then()
                .assertThat()
                .statusCode(HTTP_OK);

        Integer assessmentTypeId =
                apiActions.getAssessmentIdByScanTypeAndName(release1.getReleaseId(), FodCustomTypes.ScanType.Static, ASSESSMENT_NAME);
        log.info("Set {} Assessment Type Id: {}", FodCustomTypes.ScanType.Static.getTypeValue(), assessmentTypeId);

        PutStaticScanSetupPayload scanSetupBeforeChange =
                PutStaticScanSetupPayload.getDefaultInstance(assessmentTypeId, entitlementId, technologyStackId);
        apiActions.getStaticScansApiProvider()
                .saveStaticScanSetupDetails(release1.getReleaseId(), scanSetupBeforeChange).then()
                .statusCode(HTTP_OK);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        Response importStaticScanResponse = apiActions.importScan(FodCustomTypes.ScanType.Static, release1.getReleaseId(),
                queryParams, FPR_STATIC_SCAN_FILE_PATH);

        AllureReportUtil.info("Verify response code 200 for PUT import static scan and " +
                "response should have referenceId and ScanId");
        importStaticScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .body(matchesJsonSchemaInClasspath(
                        getPathToJsonValidation(JSON_VALIDATION_PATH_STATIC, "import_static_scan.json")));

        staticScanId = importStaticScanResponse.jsonPath().getInt("scanId");

        Assert.assertNotNull(importStaticScanResponse.jsonPath().getString("referenceId"));


        AllureReportUtil.info("Verify Scan ID reflected in the scan page");
        var staticScanSummary = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(application1.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static)
                .pressScanSummary();

        assertThat(staticScanSummary.getValueByName("Scan Id").contains(staticScanId.toString()))
                .as("Scan Id present on the scan page")
                .isTrue();
        staticScanSummary.pressClose();
        BrowserUtil.clearCookiesLogOff();
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("PUT dynamic import scan and Verify ScanID along  with reference ID")
    @MaxRetryCount(2)
    @Test(dependsOnMethods = {"putImportStaticScanTest"})
    public void putImportDynamicScanTest() {
        AllureReportUtil.info("setup dynamic scan and PUT import  dynamic scan");
        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                release2.getReleaseId(),
                Dynamic,
                "AUTO-DYNAMIC");
        Integer entitlementId = apiActions.getFirstEntitlementId();
        DynamicScansPayload dynamicScansPayload = DynamicScansPayload
                .dynamicScanSetup(assessmentTypeId, entitlementId);
        dynamicScansPayload.setIsWebService(false);
        List<DynamicScansPayload.BlockoutDay> expectedBlockout =
                dynamicScansPayload.getBlockout();
        Response response = apiActions.getDynamicScansApiProvider()
                .saveDynamicScanSetup(release2.getReleaseId(), dynamicScansPayload);
        response.then().
                assertThat().
                statusCode(HTTP_OK);

        AllureReportUtil.info("Verify response code 200 for PUT dynamic static scan and " +
                "response should have referenceId and ScanId");

        String scanSessionId = apiActions.getReleasesApiProvider()
                .getImportScanSession(release2.getReleaseId())
                .jsonPath()
                .getString("importScanSessionId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fragNo", -1);
        queryParams.put("offset", 0);
        queryParams.put("importScanSessionId", scanSessionId);

        Response importDynamicScanResponse = apiActions.getDynamicScansApiProvider().importDynamicScanFile(
                release2.getReleaseId(),
                queryParams,
                ApiHelper.retrievePayloadFile(FPR_DYNAMIC_SCAN_FILE_PATH));

        importDynamicScanResponse.then().
                assertThat().
                statusCode(HTTP_OK).
                body(matchesJsonSchemaInClasspath(
                        getPathToJsonValidation(JSON_VALIDATION_PATH_DYNAMIC, "import_dynamic_scan.json")));

        dynamicScanId = importDynamicScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(importDynamicScanResponse.jsonPath().getString("referenceId"));

        AllureReportUtil.info("Verify Scan ID reflected in the scan page");
        var dynamicScanSummary = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(application2.getApplicationName())
                .openScans()
                .getScanByType(Dynamic)
                .pressScanSummary();

        assertThat(dynamicScanSummary.getValueByName("Scan Id").contains(dynamicScanId.toString()))
                .as("Scan Id present on the scan page")
                .isTrue();
        dynamicScanSummary.pressClose();
    }
}
