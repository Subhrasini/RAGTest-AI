package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.endpoints.ScansEndpoint;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import groovy.util.logging.Slf4j;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.HashMap;
import java.util.Map;

import static utils.api.ResponseCodes.HTTP_BAD_REQUEST;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("yliben@opentext.com")
@FodBacklogItem("737006")
@Slf4j
public class SiteTreeEndpointLimitTest extends FodBaseTest {

    public DynamicScanDTO dynamicScanDTO;
    public ApplicationDTO applicationDTO;
    TenantDTO tenantDTO;
    private Integer releaseId;
    private Integer scanId;
    public String fileName = "payloads/fod/2107709.scan";
    public String dynamicURL = "http://zero.webappsecurity.com";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Start and complete dynamic scan")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        applicationDTO = ApplicationActions.createApplication(tenantDTO, true);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setDynamicSiteUrl(dynamicURL);
        dynamicScanDTO.setAssessmentType("Dynamic Website Assessment");
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(
                applicationDTO.getReleaseName(),
                fileName,
                true,
                true,
                true,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify Site Tree Api endpoint with different limits values")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void siteTreeEndpointLimitAPITest() {
        Map<String, Object> queryParams = new HashMap<>();

        var releasePage = LogInActions.tamUserLogin(tenantDTO).openYourReleases();
        releasePage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());

        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        var apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        Response responseGetScanId = apiActions.getReleasesApiProvider().getScans(releaseId);
        scanId = responseGetScanId.jsonPath().getInt("items.scanId[0]");

        AllureReportUtil.info("Query parameter  limit less than 1");
        queryParams.put("limit", -1);
        Response responseLimitLessThanOne = apiActions.getApplicationMonitoringApiProvider()
                .getEndpointsQueryPrams(ScansEndpoint.getSiteTree(scanId), queryParams);
        responseLimitLessThanOne.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);
        Assert.assertTrue(responseLimitLessThanOne.jsonPath().getString("errors.message")
                .contains("The limit parameter cannot be less than 1"));

        AllureReportUtil.info("Query parameter limit zero");
        queryParams.put("limit", 0);
        Response responseLimitZero = apiActions.getApplicationMonitoringApiProvider()
                .getEndpointsQueryPrams(ScansEndpoint.getSiteTree(scanId), queryParams);
        responseLimitZero.then()
                .assertThat()
                .statusCode(HTTP_BAD_REQUEST);
        Assert.assertTrue(responseLimitZero.jsonPath().getString("errors.message")
                .contains("The limit parameter cannot be less than 1"));


        AllureReportUtil.info("Query parameter limit 1");
        queryParams.put("limit", 1);
        Response responseLimitOne = apiActions.getApplicationMonitoringApiProvider()
                .getEndpointsQueryPrams(ScansEndpoint.getSiteTree(scanId), queryParams);
        responseLimitOne.then()
                .assertThat()
                .statusCode(HTTP_OK);

        AllureReportUtil.info("With out limit values");
        Response responseWithNoLimit = apiActions.getApplicationMonitoringApiProvider()
                .getEndpoints(ScansEndpoint.getSiteTree(scanId));
        responseWithNoLimit.then()
                .assertThat()
                .statusCode(HTTP_OK);
    }
}
