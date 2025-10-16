package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.api.payloads.UserPayload;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodApiActions;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
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
import utils.RetryAnalyzer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static utils.api.ResponseCodes.HTTP_OK;

@Owner("svpillai@opentext.com")
@Slf4j
public class TenantLastLoginDateTest extends FodBaseTest {
    TenantUserDTO developer;
    TenantDTO tenantDTO;
    FodApiActions apiActions;
    ApplicationDTO applicationDTO;

    String expectedLoginDate = "2022/03/26";
    String dateFormatPattern = "yyyy/mm/dd";
    DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
    Date today = Calendar.getInstance().getTime();
    String todayStr = dateFormat.format(today);
    String settingName = "MaxNumberofVulnerabilitiesRecordsPerAPICall";
    String errorMessage = "The field Max Number of Vulnerabilities Records Per API Call must be between 1 and 500.";
    String FPR_SCAN_FILE_PATH = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    String defaultTenantValue = "50";
    String newTenantValue = "60";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("590668")
    @Description("Create new user in Tenant side .Verify Last Login date of user after making it Inactive")
    @Test(groups = {"regression", "hf"})
    public void verifyLastLoginDateOfInactiveUser() {
        AllureReportUtil.info("Create new user with a role in tenant side ");
        LogInActions.tamUserLogin(defaultTenantDTO);
        developer = TenantUserDTO.createDefaultInstance();
        developer.setTenant(defaultTenantDTO.getTenantName());
        developer.setUserName("DevLogin-" + developer.getUserName());
        developer.setRole(FodCustomTypes.TenantUserRole.Developer);
        TenantUserActions.createTenantUsers(defaultTenantDTO, developer);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Update Last Login Date of new user");
        var tenantName = defaultTenantDTO.getTenantName();
        var userName = developer.getUserName();
        var query = String.format("UPDATE UserAccount\n" +
                "SET LastLoginDate = '2022-03-26 23:00:18.16'\n" +
                "FROM UserAccount u\n" +
                "         join TenantUser t on t.Userid = u.Userid\n" +
                "         join TenantMaster tm on t.TenantId = tm.TenantID\n" +
                "where tm.TenantName = '%s'\n" +
                "and u.UserName = '%s'", tenantName, userName);
        new FodSQLUtil().executeQuery(query).close();
        var secLeadLogin = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openUsers();
        secLeadLogin.findWithSearchBox(developer.getUserName());
        assertThat(secLeadLogin.getUserStatusByName(developer.getUserName()))
                .as("Status of the user should be active")
                .isEqualTo("Active");
        assertThat(secLeadLogin.getLastLoginDateByName(developer.getUserName()))
                .as("Last Login date  of the user should be updated as past date")
                .isEqualTo(expectedLoginDate);
        secLeadLogin.editUserByName(developer.getUserName()).setInactive(true).pressSaveBtn();
        assertThat(secLeadLogin.getUserStatusByName(developer.getUserName()))
                .as("Status of the user should be Inactive")
                .isEqualTo("Inactive");
        BrowserUtil.clearCookiesLogOff();

        var loginPage = TenantLoginPage.navigate()
                .setTenantCode(defaultTenantDTO.getTenantCode())
                .setLogin(developer.getUserName())
                .setPassword(FodConfig.TAM_PASSWORD);
        loginPage.pressLoginButton();
        assertThat(loginPage.getErrorMessage())
                .as("Inactive user wasn't logged in.")
                .isEqualTo("The username, password and/or tenant was incorrect.");
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Validate Last Login Date when user status is inactive and failed Login");
        secLeadLogin = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openUsers();
        secLeadLogin.findWithSearchBox(developer.getUserName());
        assertThat(secLeadLogin.getUserStatusByName(developer.getUserName()))
                .as("Status of the user should be Inactive")
                .isEqualTo("Inactive");
        assertThat(secLeadLogin.getLastLoginDateByName(developer.getUserName()))
                .as("Last Login date  of the user should not be changed to current date")
                .isEqualTo(expectedLoginDate)
                .isNotEqualTo(todayStr);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("727002")
    @Description("Verify new tenant settings in site settings and tenant details page")
    @Test(groups = {"regression"})
    public void verifyMaxNumberOfVulnerabilitiesRecordInAdmin() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        AllureReportUtil.info("Verify new setting for getting max number of vulnerabilities is present is site " +
                "settings page");
        var settingsPage = new AdminTopNavbar().openConfiguration();
        assertThat(settingsPage.isSettingExists(settingName))
                .as("Setting 'MaxNumberofVulnerabilitiesRecordsPerAPICall' should be present")
                .isTrue();
        assertThat(settingsPage.getSettingValueByName(settingName))
                .as("Verify default setting value is set to 500")
                .isEqualTo("500");

        AllureReportUtil.info("Verify new setting for getting max number of vulnerabilities is present is tenant " +
                "details page");
        var tenantDetailsPage = new AdminTopNavbar().openTenants()
                .openTenantByName(tenantDTO.getTenantName());
        assertThat(tenantDetailsPage.maxNumberOfVulnerabilitiesPerApiCall.exists())
                .as("New tenant setting Max Number of Vulnerabilities Records Per API Call appeared")
                .isTrue();
        assertThat(tenantDetailsPage.getMaxVulnerabilitiesCount())
                .as("Default setting value is set to 50")
                .isEqualTo(defaultTenantValue);
        tenantDetailsPage.setMaxNumberOfVulnerabilitiesPerApiCall(newTenantValue).pressSave();
        assertThat(tenantDetailsPage.getMaxVulnerabilitiesCount())
                .as("Admin user should be able to edit the values")
                .isEqualTo(newTenantValue);
        tenantDetailsPage.setMaxNumberOfVulnerabilitiesPerApiCall("501").pressSave();
        assertThat(tenantDetailsPage.getTenantDetailsErrorMessage())
                .as("Maximum value should not increase beyond the default value 500")
                .isEqualTo(errorMessage);

        AllureReportUtil.info("Verify changing value for one tenant should not affect other tenants");
        assertThat(new AdminTopNavbar().openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName()).getMaxVulnerabilitiesCount())
                .as("Changing vulnerability count for one tenant should not affect other tenants")
                .isEqualTo(defaultTenantValue);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("727002")
    @Description("Verify the limit value in Get Request")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyMaxNumberOfVulnerabilitiesRecordInAdmin"})
    public void getMaximumRecordsOfVulnerabilitiesOnApiCall() {
        TenantUserActions.activateSecLead(tenantDTO, true);
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, false);
        var releaseUrl = WebDriverRunner.url();
        var releaseId = Integer.parseInt(
                releaseUrl.substring(releaseUrl.indexOf("Releases") + 9, releaseUrl.indexOf("/Overview")));
        StaticScanActions.importScanTenant(applicationDTO, FPR_SCAN_FILE_PATH);
        AllureReportUtil.info("Verify limit value should be less or equal to Max Number of Vulnerabilities Records Per" +
                " API Call set for current tenant");
        apiActions = FodApiActions.init(
                new UserPayload(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD),
                tenantDTO.getTenantCode(),
                AccessScopeRestrictionsFodApi.API_TENANT
        );
        Response response = apiActions.getVulnerabilitiesApiProvider().getVulnerabilitiesByReleaseId(releaseId);
        response.then()
                .assertThat()
                .statusCode(HTTP_OK)
                .body("limit", lessThanOrEqualTo(Integer.parseInt(newTenantValue)))
                .body("limit", not(greaterThan(Integer.parseInt(newTenantValue))));
    }
}
