package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.EntitlementsActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
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

import static com.codeborne.selenide.Selenide.refresh;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static utils.api.ResponseCodes.*;

@Owner("yliben@opentext.com")
@FodBacklogItem("738008")
public class OSSEntitlementTest extends FodBaseTest {
    private final String JSON_VALIDATION_PATH = "fod/JSON_schema_validation/tenant/%s";
    TenantDTO tenantDTO,tenantDTO1,tenantDTO2;
    EntitlementDTO entitlementDTO;
    FodApiActions apiActions,apiActions1,apiActions2,apiActions3;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Create tenant and added Ent")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setQuantityPurchased(200);

        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(100);

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlementDTO);
        tenantDTO1 = TenantDTO.createDefaultInstance();
        tenantDTO2 = TenantDTO.createDefaultInstance();

        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        TenantActions.createTenant(tenantDTO1);
        EntitlementsActions.createEntitlements(tenantDTO1, false,entitlementDTO);
        BrowserUtil.clearCookiesLogOff();

        TenantActions.createTenant(tenantDTO2);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO1, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO2, true);
        BrowserUtil.clearCookiesLogOff();

        apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        apiActions1 = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.VIEW_ISSUES);

        apiActions2 = FodApiActions
                .init(new UserPayload(tenantDTO1.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO1.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);

        apiActions3 = FodApiActions
                .init(new UserPayload(tenantDTO2.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO2.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
    }

    @Severity(SeverityLevel.BLOCKER)
    @Description("Return a list of active open source entitlements for tenant")
    @Test(dependsOnMethods = "prepareTestData")
    public void getTenantOpenSourceEntitlementTest() {
        Response response = apiActions.getTenantsApiProvider().getTenantOpenSourceEntitlement();
        response.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .assertThat().body(matchesJsonSchemaInClasspath(String.format(JSON_VALIDATION_PATH,
                "tenant_Open_Source_Entitlement.json")));

        AllureReportUtil.info("Get a list of active OSS entitlement with out allowed scope");
        Response response1 = apiActions1.getTenantsApiProvider().getTenantOpenSourceEntitlement();
        response1.then()
                .assertThat()
                .statusCode(HTTP_UNAUTHORIZED);
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("verify without active OSS Entitlements it returns message No Active Open Source Entitlements Available")
    @Test(dependsOnMethods = "getTenantOpenSourceEntitlementTest")
    public void verifyErrorMessageWithNoActiveOSSEntitlementsTest() {
        Response response = apiActions2.getTenantsApiProvider().getTenantOpenSourceEntitlement();
        response.then()
                .assertThat()
                .statusCode(HTTP_NOT_FOUND);
        Assert.assertTrue(response.body().asString().contains("No Active Open Source Entitlements Available."));
    }
    @Severity(SeverityLevel.NORMAL)
    @Description("verify when no active Debricked Premium Entitlements endpoint should returns active Debricked Trial Entitlements")
    @Test(dependsOnMethods = "verifyErrorMessageWithNoActiveOSSEntitlementsTest")
    public void verifyDebrickedTrialReturnsWithNoActiveDebrickPremiumTest() {
        AllureReportUtil.info("Verify Debricked Trial Entitlements returns only when no other entitlement present");
        var openEntitlementsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .findTenant(tenantDTO2.getTenantName()).openTenantByName(tenantDTO2.getTenantName())
                .openEntitlements();
        var entitlementsPopup = openEntitlementsPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedTrialEntitlement)
                .pressAddEntitlement();

        entitlementsPopup.setPurchasedQuantity(1000);
        entitlementsPopup.pressSaveButton();
        refresh();

        Response response = apiActions3.getTenantsApiProvider().getTenantOpenSourceEntitlement();
        response.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .assertThat().body(matchesJsonSchemaInClasspath(String.format(JSON_VALIDATION_PATH,
                        "tenant_Open_Source_Entitlement.json")));

        Assert.assertTrue(response.jsonPath().get("entitlementType").toString().contains("DebrickedTrial"));

        AllureReportUtil.info("Verify debricked Premium active entitlement return  when both debricked and Debricked Trial available.");
        EntitlementsActions.createEntitlements(tenantDTO2, false,
                EntitlementDTO.createDefaultInstance(FodCustomTypes.EntitlementType.DebrickedEntitlement));
        Response response1 = apiActions3.getTenantsApiProvider().getTenantOpenSourceEntitlement();
        response1.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .assertThat().body(matchesJsonSchemaInClasspath(String.format(JSON_VALIDATION_PATH,
                        "tenant_Open_Source_Entitlement.json")));

        Assert.assertTrue(response1.jsonPath().get("entitlementType").toString().contains("Debricked"));
        BrowserUtil.clearCookiesLogOff();
    }
}
