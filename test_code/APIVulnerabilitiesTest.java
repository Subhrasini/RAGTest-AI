package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class APIVulnerabilitiesTest extends FodBaseTest {

    @FodBacklogItem("1628005")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Max Number of Vulnerabilities Records Per API Call test")
    @Test(groups = "{regression}")
    public void maxAPIVulnerabilitiesCountTest() {
        var tenantDTO = TenantActions.createTenant();

        page(TenantDetailsPage.class).setMaxNumberOfVulnerabilitiesPerApiCall("500")
                .pressSave();

        TenantUserActions.activateSecLead(tenantDTO, true);

        var applicationDTO = ApplicationActions.createApplication();

        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/1096319_scandata.fpr");

        log.info("Starting API part");
        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        JsonPath bodyJson = apiActions.getVulnerabilities(releaseId);
        Assert.assertFalse(bodyJson.getList("items").isEmpty());

        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseId(releaseId);
        response.then()
                .assertThat()
                .statusCode(HTTP_OK);

        var responseBody = response.getBody();
        Assert.assertTrue(responseBody.jsonPath()
                .getInt("totalCount") > 50, "The total vulnerabilities count is greater than 50");
        Assert.assertEquals(responseBody.jsonPath()
                .getInt("limit"), 500, "The max count of vulnerabilities limit is set to 500");
    }
}