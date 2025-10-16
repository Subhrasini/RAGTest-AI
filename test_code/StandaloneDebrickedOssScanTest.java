package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.ApiHelper;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.ReleasePayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.test.BaseFodApiTest;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static utils.api.ResponseCodes.*;

@Owner("svpillai@opentext.com")
@FodBacklogItem("879021")
@Slf4j
public class StandaloneDebrickedOssScanTest extends BaseFodApiTest {
    private final String debrickedFilePath = "payloads/fod/vuln_python_with_pipfilelock(debricked).zip";
    private FodApiActions apiActionsByUser;
    private TenantDTO tenantDTO;
    private EntitlementDTO entitlementDTO, debrickedEntitlement;
    private ApplicationPayload application, application1;
    private ReleasePayload release, release1;
    private Integer scanId;
    private String deepLink_url;
    private Map<String, Object> queryParams = new HashMap<>() {
        {
            put("fragNo", -1);
            put("offset", 0);
        }
    };

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , applications and entitlements for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlementDTO);
        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);

        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        apiActionsByUser = FodApiActions.init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
        application = apiActionsByUser.createApplication(false);
        application.setOwnerId(apiActionsByUser.getCurrentSL().getUserId());
        release = apiActionsByUser.createRelease(application.getApplicationId(), apiActionsByUser.getCurrentSL().getUserId());
        application1 = apiActionsByUser.createApplication(false);
        application1.setOwnerId(apiActionsByUser.getCurrentSL().getUserId());
        release1 = apiActionsByUser.createRelease(application1.getApplicationId(), apiActionsByUser.getCurrentSL().getUserId());
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify open source scan starts and validate using get endpoints")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void startOpenSourceScanTest() {
        AllureReportUtil.info("Verify open source scan should be started successfully");
        Response ossScanResponse = apiActionsByUser.getOpenSourceScansApiProvider().startScan(release.getReleaseId(),
                queryParams, ApiHelper.retrievePayloadFile(debrickedFilePath));
        ossScanResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);
        scanId = ossScanResponse.jsonPath().getInt("scanId");
        Assert.assertNotNull(scanId, "Response should return unique scanId");

        Response scanResponse = apiActionsByUser.getItemsResponseWithDelay(4, true,
                apiActionsByUser.getScansApiProvider()::getAListOfScans);
        scanResponse.then().assertThat()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_CREATED)))
                .body("items.scanId", hasItem(scanId));

        Response scanSummaryResponse = apiActionsByUser.getScansApiProvider().getSummaryInfoForRequestedScanID(scanId);
        scanSummaryResponse.then().assertThat()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_CREATED)));
        Assert.assertEquals(scanSummaryResponse.jsonPath().getString("scanTool"), "Debricked",
                "Response should contain all the details of the scan along with scan tool Debricked");

        AllureReportUtil.info("Verify 200 status for getting SBOM by scanID");
        Response sbomResponse = apiActionsByUser.getResponseByStatusCode(10, 30000L, HTTP_OK,
                () -> apiActionsByUser.getOpenSourceComponentsApiProvider().downloadOpenSourceSBOMFile(scanId));
        sbomResponse.then()
                .assertThat()
                .statusCode(HTTP_OK);
        Assert.assertTrue(sbomResponse.getBody().asByteArray().length > 0);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Disable debricked entitlement and verify open source scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"startOpenSourceScanTest"})
    public void verifyOssByDisablingDebrickedEntitlement() {
        EntitlementsActions.disableEntitlementsByEntitlementType(tenantDTO,
                FodCustomTypes.EntitlementType.DebrickedEntitlement, true);
        BrowserUtil.clearCookiesLogOff();

        Response badResponse = apiActionsByUser.getOpenSourceScansApiProvider().startScan(release1.getReleaseId(), queryParams
                , ApiHelper.retrievePayloadFile(debrickedFilePath));
        badResponse.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);

        AllureReportUtil.info("Verify 404 not found response for invalid releaseId");
        Response notFoundResponse = apiActionsByUser.getOpenSourceScansApiProvider().startScan(001, queryParams
                , ApiHelper.retrievePayloadFile(debrickedFilePath));
        notFoundResponse.then()
                .assertThat()
                .statusCode(HTTP_NOT_FOUND);
    }
    @Owner("yliben@opentext.com")
    @FodBacklogItem("1738006")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Open Source - Verify New API endpoint to generate deeplinks filter options - Category, Status and Severity")
    @Test(dependsOnMethods = {"prepareTestData","verifyOssByDisablingDebrickedEntitlement"})
    public void verifyGenerateDeepLinksTest(){
        AllureReportUtil.info("Open Source - Verify generate deeplinks filter options - Category");
        Map<String, Object> deepLinksParamsCategory = new HashMap<>();
        deepLinksParamsCategory.put("filter","category:Open Source");

        Response responseDeepLinks = apiActionsByUser.getVulnerabilitiesApiProvider()
                .generateDeeplinkIssues(release.getReleaseId(), deepLinksParamsCategory);
        responseDeepLinks.then()
                .assertThat()
                .statusCode(HTTP_OK);
        String Url = responseDeepLinks.jsonPath().getString("url");

        String url_regex = "^https?://[a-zA-Z0-9.-]+/Releases/(\\d+)/Issues\\?d=([\\d+]+)$";

        Pattern pattern = Pattern.compile(url_regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern.matcher(Url);

        Assert.assertTrue(matcher1.matches(),"deeplink url generated for the issue filter by category");
        Assert.assertTrue(Url.contains(release.getReleaseId().toString()),"the release id is part of the deeplinks url");

        AllureReportUtil.info("generate deeplinks url with two filter and an OR condition ");
        Map<String, Object> deepLinksParam= new HashMap<>();
        deepLinksParam.put("filter","category:Open Source+severitystring:High|Medium");
        Response response = apiActionsByUser.getVulnerabilitiesApiProvider()
                .generateDeeplinkIssues(release.getReleaseId(), deepLinksParam);
        response.then()
                .assertThat()
                .statusCode(HTTP_OK);

        deepLink_url = response.jsonPath().getString("url");
        Matcher matcher2 = pattern.matcher(deepLink_url);

        Assert.assertTrue(matcher2.matches(),"deeplink url generated for the issue filter by category and Severity");
        Assert.assertTrue(deepLink_url.contains(release.getReleaseId().toString()),"the release id is part of the deeplinks url");

        AllureReportUtil.info("generate deeplinks url with three filter category,Severity and Status");
        Map<String, Object> deepLinksThreeParam= new HashMap<>();
        deepLinksThreeParam.put("filter","category:Open Source+status:New+severity:High");
        Response responseThreeFilters = apiActionsByUser.getVulnerabilitiesApiProvider()
                .generateDeeplinkIssues(release.getReleaseId(), deepLinksThreeParam);
        responseThreeFilters.then()
                .assertThat()
                .statusCode(HTTP_OK);

        deepLink_url = responseThreeFilters.jsonPath().getString("url");
        Matcher matcher3 = pattern.matcher(deepLink_url);

        Assert.assertTrue(matcher3.matches(),"deeplink url generated for the issue filter by category,Severity and Status");
        Assert.assertTrue(deepLink_url.contains(release.getReleaseId().toString()),"the release id is part of the deeplinks url");

        AllureReportUtil.info("Open Source scan -verify the response when it returns the zero values, it should return 200 ok response and return the url");
        Map<String, Object> deepLinksParam1 = new HashMap<>();
        deepLinksParam1.put("filter","category:Open Source+status:New+severity:Low");
        Response responseFilters = apiActionsByUser.getVulnerabilitiesApiProvider()
                .generateDeeplinkIssues(release.getReleaseId(), deepLinksParam1);
        responseFilters.then()
                .assertThat()
                .statusCode(HTTP_OK);

        deepLink_url = responseFilters.jsonPath().getString("url");
        Matcher matcher4 = pattern.matcher(deepLink_url);

        Assert.assertTrue(matcher4.matches(),"deeplink url generated for the issue filter by category and Severity");
        Assert.assertTrue(deepLink_url.contains(release.getReleaseId().toString()),"the release id is part of the deeplinks url");
    }
}
