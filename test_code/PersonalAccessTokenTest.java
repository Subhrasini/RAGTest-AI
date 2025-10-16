package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.api.payloads.ApplicationPayload;
import com.fortify.fod.api.payloads.DynamicScansPayload;
import com.fortify.fod.api.payloads.ReportsPayload;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.function.Supplier;

import static com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi.*;
import static utils.api.ResponseCodes.*;

@Owner("vdubovyk@opentext.com")
@FodBacklogItem("605003")
@Slf4j
public class PersonalAccessTokenTest extends FodBaseTest {

    TenantDTO tenantDTO;
    EntitlementDTO entitlementDTO, sonatypeEntitlementDTO;
    ApplicationDTO application;
    Integer releaseId, entitlementId, tenantId, applicationId;

    private void initTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission",
                        //"Scan Third Party Libraries",
                        "Enable Source Download"
                });

        entitlementDTO = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlementDTO = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlementDTO.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);

        application = ApplicationDTO.createDefaultInstance();
    }

    @MaxRetryCount(3)
    @Description("Preparing test data")
    @Test(groups = {"regression"})

    public void prepareTestData() {
        initTestData();

        TenantActions.createTenant(tenantDTO);

        tenantId = Integer.parseInt(new FodSQLUtil().getTenantIdByName(tenantDTO.getTenantName()));

        EntitlementsActions.createEntitlements(tenantDTO, false, entitlementDTO);
        entitlementId = new EntitlementCell().getId();

        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlementDTO);

        TenantUserActions.activateSecLead(tenantDTO, true);

        ApplicationActions.createApplication(application, tenantDTO, false);

        var releaseUrl = WebDriverRunner.url();
        releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));

        applicationId = Integer.parseInt(new FodSQLUtil().getApplicationIdByName(application.getApplicationName()));
    }

    @MaxRetryCount(3)
    @Description("Test for 'Start Scans' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void startScansScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Start Scans Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("start-scans", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), START_SCANS);

        Integer assessmentTypeId = apiActions.getAssessmentIdByScanTypeAndName(
                releaseId,
                FodCustomTypes.ScanType.Dynamic,
                "AUTO-DYNAMIC");

        DynamicScansPayload scanSetupPayload = DynamicScansPayload.dynamicScanSetup(assessmentTypeId, entitlementId);
        apiActions.getDynamicScansApiProvider().saveDynamicScanSetup(releaseId, scanSetupPayload);
        DynamicScansPayload startScanPayload = DynamicScansPayload.startDynamicScan(assessmentTypeId, entitlementId);

        Response response = apiActions.getDynamicScansApiProvider().startDynamicScan(releaseId, startScanPayload);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(200);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'manage apps' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void manageAppsScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Manage Applications Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("manage-apps", true)
                .pressSave()
                .pressClose();

        var userId = Integer.parseInt(new FodSQLUtil().getUserIdByUserName(tenantDTO.getUserName()));

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), MANAGE_APPS);

        ApplicationPayload applicationPayload = ApplicationPayload.getDefaultInstance();
        applicationPayload.setOwnerId(userId);

        Response response = apiActions.getApplicationsApiProvider().createApplication(applicationPayload);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_CREATED);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 201, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'VIEW APPS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void viewAppsScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "View Applications Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("view-apps", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), VIEW_APPS);

        Response response = apiActions.getApplicationsApiProvider().getApplications();

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'MANAGE ISSUES' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void manageIssuesScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Manage Issues Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("manage-issues", true)
                .pressSave()
                .pressClose();


        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), MANAGE_ISSUES);

        Response response = apiActions.getApplicationsApiProvider().getApplicationAuditTemplates(applicationId);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'VIEW ISSUES' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void viewIssuesScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "View Issues Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("view-issues", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), VIEW_ISSUES);

        Response response = apiActions.getVulnerabilitiesApiProvider().getVulnerabilitiesByReleaseId(releaseId);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'MANAGE REPORTS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void manageReportsScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Manage Reports Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("manage-reports", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), MANAGE_REPORTS);

        ReportsPayload reportsPayload = ReportsPayload.getInstance(applicationId, releaseId, -8);

        Response response = apiActions.getReportsApiProvider().createReport(reportsPayload);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_UNPROCESSABLE_ENTITY);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 422, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'VIEW REPORTS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void viewReportsScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "View Reports Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("view-reports", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), VIEW_REPORTS);

        Response response = apiActions.getItemsResponseWithDelay(4, true,
                apiActions.getReportsApiProvider()::getListOfReports);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'MANAGE USERS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void manageUsersScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Manage Users Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("manage-users", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), MANAGE_USERS);

        Response response = apiActions.getUserGroupManagementApiProvider().getUserGroups();

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'VIEW USERS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void viewUsersScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openUserManagement()
                .openGroups()
                .addGroup("Brand New Group " + UniqueRunTag.generate(), "", true)
                .tenantTopNavbar
                .openPersonalAccessTokens();

        var userGroupId = Integer.parseInt(new FodSQLUtil().getUserGroupIdByTenantId(tenantId));

        String personalAccessTokenName = "View Users Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("view-users", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), VIEW_USERS);

        Response response = apiActions.getUserGroupApplicationAccessProvider()
                .getUserGroupApplicationAccess(userGroupId);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'MANAGE NOTIFICATIONS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void manageNotificationsScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Manage Notifications Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("manage-notifications", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), MANAGE_NOTIFICATIONS);

        Response response = apiActions
                .getItemsResponseWithDelay(4, true,
                        apiActions.getNotificationsApiProvider()::getAListOfUnreadNotifications);

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'VIEW-TENANT-DATA' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void viewTenantDataScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "View Tenant Data Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("view-tenant-data", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), VIEW_TENANT_DATA);

        Response response = apiActions.getTenantsApiProvider().getTenantsEntitlements();

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Test for 'API-TENANT' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void apiTenantScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "API Tenant Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("api-tenant", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), API_TENANT);

        Response response = apiActions.getTenantsApiProvider().getTenantsEntitlements();

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_OK);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 200, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }

    @MaxRetryCount(3)
    @Description("Negative test for 'START-SCANS' scope")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData",
            enabled = true)

    public void negativeStartScansScopeTest() {
        var personalAccessTokensPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openPersonalAccessTokens();

        String personalAccessTokenName = "Negative Start Scans Scope " + UniqueRunTag.generate();

        personalAccessTokensPage
                .pressAddAccessToken()
                .setPersonalAccessTokenName(personalAccessTokenName)
                .setAllowedScopesByName("start-scans", true)
                .pressSave()
                .pressClose();

        var apiActions = FodApiActions.init(new UserPayload(tenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD), tenantDTO.getTenantName(), START_SCANS);

        Response response = apiActions.getUserGroupApplicationAccessProvider().getUserGroups();

        Supplier sup = () -> response
                .then()
                .assertThat()
                .statusCode(HTTP_UNAUTHORIZED);

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 401, sup, Duration.ofMinutes(1), true);

        personalAccessTokensPage.deleteTokenByName(personalAccessTokenName);
    }
}