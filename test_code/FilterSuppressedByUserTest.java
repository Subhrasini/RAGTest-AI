package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;

@Owner("yliben@opentext.com")
@FodBacklogItem("420018")
@Slf4j
public class FilterSuppressedByUserTest extends FodBaseTest {

    TenantDTO tenantDTO;
    EntitlementDTO entitlement;
    ApplicationDTO applicationDTO;
    TenantUserDTO userA;
    TenantUserDTO userB;
    Integer releaseId;
    FodApiActions apiActions;
    private final String StaticFilePath = "payloads/fod/static.java.fpr";
    private final String DynamicFilePath = "payloads/fod/dynamic.zero.fpr";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Create tenant, create user and setup audit template")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        entitlement = EntitlementDTO.createDefaultInstance();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlement);
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission"
                });

        userA = TenantUserDTO.createDefaultInstance();
        userA.setTenant(tenantDTO.getTenantCode());

        userB = TenantUserDTO.createDefaultInstance();
        userB.setTenant(tenantDTO.getTenantCode());

        AllureReportUtil.info("Create Test Tenant and activate Security Lead");
        TenantActions.createTenant(tenantDTO)
                .adminTopNavbar
                .openTenants();
        BrowserUtil.clearCookiesLogOff();

        EntitlementsActions.createEntitlements(tenantDTO, true, entitlement);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create Test UserA");
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        TenantUserActions.createTenantUser(userA);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create Test UserB");
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        TenantUserActions.createTenantUser(userB);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Get api access token for created tenant");
        apiActions = FodApiActions
                .init(new UserPayload(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD),
                        tenantDTO.getTenantName(), AccessScopeRestrictionsFodApi.API_TENANT);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("verify vulnerabilities that was suppressed by user A is returned and suppressedBy user A")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifySuppressedIssueByUserA(){
        AllureReportUtil.info("Create app and import dynamic");
        applicationDTO = ApplicationDTO.createDefaultInstance();
        LogInActions.tenantUserLogIn(userA.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        ApplicationActions.createApplication(applicationDTO);
        DynamicScanActions.importDynamicScanTenant(applicationDTO,DynamicFilePath );
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Login in with UserA and suppressed an issue with suppressed option Not an issue");
        var issuesPage = LogInActions
                .tenantUserLogIn(userA.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();

        issuesPage.openIssueByIndex(0).setSeverity("Medium");
        issuesPage.clickMedium();

        var suppressedMedium = issuesPage.clickMedium().openIssueByIndex(0);
        suppressedMedium.setAuditorStatus("Not an Issue");
        issuesPage.clickMedium();
        BrowserUtil.clearCookiesLogOff();

        var releasePage = LogInActions.tenantUserLogIn(userA.getUserName(),
                FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases();
        releasePage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());


        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        AllureReportUtil.info("verify vulnerabilities that was suppressed by user A is returned and suppressedBy user A");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("includeSuppressed", true);
        queryParams.put("filters", "suppressedBy:"+userA.getUserName()+"");
        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseIdAndQueryPrams(releaseId, queryParams);

        response.then()
                .assertThat()
                .body("items.isSuppressed[0]", equalTo(true))
                .body("items.suppressedBy[0]", equalTo(""+userA.getUserName()+""))
                .body("items.auditorStatus[0]", equalTo("Not an Issue"));
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("verify vulnerabilities that was suppressed by user B is returned and suppressedBy user B")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifySuppressedIssueByUserA"})
    public void verifySuppressedIssueByUserB() {
        AllureReportUtil.info("Create app and import static");
        applicationDTO = ApplicationDTO.createDefaultInstance();
        LogInActions.tenantUserLogIn(userB.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        ApplicationActions.createApplication(applicationDTO);
        StaticScanActions.importScanTenant(tenantDTO, applicationDTO,StaticFilePath,false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Login in with UserB and suppressed an issue with suppressed option Risk Accepted");
        var issuesPage = LogInActions
                .tenantUserLogIn(userB.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();

        issuesPage.openIssueByIndex(0).setSeverity("High");
        issuesPage.clickHigh();

        var suppressedMedium = issuesPage.clickHigh().openIssueByIndex(0);
        suppressedMedium.setAuditorStatus("Risk Accepted");
        issuesPage.clickHigh();
        BrowserUtil.clearCookiesLogOff();

        var releasePage = LogInActions.tenantUserLogIn(userB.getUserName(),
                FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases();
        releasePage.openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());


        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        AllureReportUtil.info("verify vulnerabilities that was suppressed by user B is returned and suppressedBy user B");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("includeSuppressed", true);
        queryParams.put("filters", "suppressedBy:"+userB.getUserName()+"");
        Response response = apiActions.getVulnerabilitiesApiProvider()
                .getVulnerabilitiesByReleaseIdAndQueryPrams(releaseId, queryParams);

        response.then()
                .assertThat()
                .body("items.isSuppressed[0]", equalTo(true))
                .body("items.suppressedBy[0]", equalTo(""+userB.getUserName()+""))
                .body("items.auditorStatus[0]", equalTo("Risk Accepted"));
    }
}
