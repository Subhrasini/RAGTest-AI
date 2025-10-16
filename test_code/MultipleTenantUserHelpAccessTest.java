package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("813002")
public class MultipleTenantUserHelpAccessTest extends FodBaseTest {

    TenantDTO tenantDTO;
    String emailAddress = "flashtest@fod.com";
    String userName = "FOD-FLASH-USER-APPLEAD";
    TenantUserDTO tenantUserDTO;

    @MaxRetryCount(3)
    @Description("Prepare tenant, active secLead and set tenantUserDTO")
    @Test(groups = {"regression", "hf"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setUserEmail(emailAddress);
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        tenantUserDTO = TenantUserDTO.createDefaultInstance();
        tenantUserDTO.setTenant(defaultTenantDTO.getTenantCode());
        tenantUserDTO.setUserEmail(emailAddress);
        tenantUserDTO.setUserName(userName);
        tenantUserDTO.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login with users with exact emails and validate Help Center access")
    @Test(groups = {"regression", "hf"}, dependsOnMethods = "prepareTestData")
    public void validateMultipleEmailAddressesTest() {
        AllureReportUtil.info("Login with activated secLead on new Tenant");
        checkHelpCenterAccess(tenantDTO.getUserName(), tenantDTO);

        AllureReportUtil.info("Create new user on default tenant");
        var usersPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openUsers();
        var exists = usersPage.findWithSearchBox(emailAddress).usersTable;
        if (exists.getAllDataRows().get(0).exists()) {
            usersPage.deleteUserByName(userName);
        }
        usersPage.createUser(tenantUserDTO);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Login with new user...");
        checkHelpCenterAccess(tenantUserDTO.getUserName(), defaultTenantDTO);

    }

    public void checkHelpCenterAccess(String userName, TenantDTO tenantDTO) {
        var login = LogInActions
                .tenantUserLogIn(userName, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode()).tenantTopNavbar;
        AllureReportUtil.info("Validate Help Center availability");
        login.openHelpMenu();
        assertThat(login.helpMenuItems.texts())
                .as("Help Center should exist in Help Menu")
                .contains("Help Center");
        BrowserUtil.clearCookiesLogOff();
    }
}

