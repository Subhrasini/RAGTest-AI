package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.user_menu.SwitchTenantPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ProtectedTenantAccessTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO webApp, mobApp;
    StaticScanDTO staticScanDTO = StaticScanDTO.createDefaultInstance();
    MobileScanDTO mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
    DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

    AdminUserDTO admin, staticOperator, dynamicTester, tam, mobileTester, staticManager, dynamicManager, mobileManager,
            staticAuditor, tamManager, seniorTam, operations;
    ArrayList<AdminUserDTO> users;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant (Country-US) and all admin users. Set tenant as protected")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        users = new ArrayList<>();
        tenantDTO = TenantDTO.createDefaultInstance();
        webApp = ApplicationDTO.createDefaultInstance();
        mobApp = ApplicationDTO.createDefaultMobileInstance();

        admin = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.Admin);
        staticOperator = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticOperator);
        dynamicTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicTester);
        tam = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAM);
        mobileTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.MobileTester);
        staticManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticManager);
        dynamicManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicManager);
        mobileManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.MobileManager);
        staticAuditor = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.StaticAuditor);
        tamManager = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAMManager);
        seniorTam = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.SeniorTAM);
        operations = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.Operations);
        tenantDTO.setCountry("United States");
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Allow scanning with no entitlements"
                });
        TenantActions.createTenant(tenantDTO);

        users.add(tam);
        users.add(admin);
        users.add(staticOperator);
        users.add(dynamicTester);
        users.add(mobileTester);
        users.add(staticManager);
        users.add(dynamicManager);
        users.add(mobileManager);
        users.add(staticAuditor);
        users.add(tamManager);
        users.add(seniorTam);
        users.add(operations);

        for (var user : users) {
            user.setCountry("Ukraine");
            user.setTenant(tenantDTO.getTenantName());
            AdminUserActions.createAdminUser(user, false);
        }

        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(webApp, tenantDTO, true);
        ApplicationActions.createApplication(mobApp);
        StaticScanActions.createStaticScan(staticScanDTO, webApp);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        MobileScanActions.createMobileScan(mobileScanDTO, mobApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        var message = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantCode())
                .setProtected(true)
                .pressSave().getTenantDetailsSuccessMessage();

        assertThat(message).as("Settings should be saved successfully")
                .isEqualTo("The Tenant Details have been saved");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate user access to protected tenant with Non US users")
    @Parameters({"User Role"})
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            dataProvider = "adminUsersRoles", dataProviderClass = FodUiTestDataProviders.class)
    public void protectedTenantAccessWithNonUsUsers(FodCustomTypes.AdminUserRole role) {
        users.stream().filter(user -> user.getRole().equals(role))
                .findFirst().ifPresent(userToValidate -> validateUserAccess(userToValidate, false));
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Change country on US for all users")
    @Test(groups = {"regression"}, dependsOnMethods = {"protectedTenantAccessWithNonUsUsers"})
    public void changeUsersCountry() {
        var us = "United States";
        var usersPage = LogInActions.adminLogIn().adminTopNavbar.openUsers();
        for (var user : users) {
            user.setCountry(us);
            Supplier<Boolean> sup = () -> {
                usersPage.editUserByName(user.getUserName())
                        .selectCountry(us).pressSaveBtn();
                return usersPage.getUserByName(user.getUserName()).getCountry().equals(us);
            };

            WaitUtil.waitForTrue(sup, Duration.ofMinutes(2), true);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate user access to protected tenant with US users")
    @Parameters({"User Role"})
    @Test(groups = {"regression"}, dependsOnMethods = {"changeUsersCountry"},
            dataProvider = "adminUsersRoles", dataProviderClass = FodUiTestDataProviders.class)
    public void protectedTenantAccessWithUsUsers(FodCustomTypes.AdminUserRole role) {
        users.stream().filter(user -> user.getRole().equals(role))
                .findFirst().ifPresent(userToValidate -> validateUserAccess(userToValidate, true));
    }

    private void validateUserAccess(AdminUserDTO user, boolean visibleForUser) {
        var hasAccess = visibleForUser ? "has" : "hasn't";
        if (user.getRole().equals(FodCustomTypes.AdminUserRole.TAM)) {
            TenantLoginPage.navigate()
                    .setLogin(user.getUserName())
                    .setPassword(user.getPassword())
                    .setTenantCode(tenantDTO.getTenantCode())
                    .pressLoginButton();

            if (visibleForUser) {
                assertThat(page(SwitchTenantPage.class).tenants())
                        .as("Tam user %s access to tenant", hasAccess)
                        .contains(tenantDTO.getTenantCode());
            } else {
                assertThat(page(SwitchTenantPage.class).getInfoMessage())
                        .as("Tam user %s access to tenant", hasAccess)
                        .isEqualTo("There are not any available tenants to select.");
            }

            BrowserUtil.clearCookiesLogOff();
        }

        var page = LogInActions.adminUserLogIn(user);
        var accessMap = getAccessMap(user);
        boolean validation = false;
        var adminUrl = FodConfig.TEST_ENDPOINT_ADMIN;

        for (var access : accessMap.entrySet()) {
            if (access.getKey().equals("static") && access.getValue()) {
                var scan = page.adminTopNavbar.openStatic().findScanByAppName(webApp.getApplicationName());
                validation = visibleForUser == (scan != null);
            }

            if (access.getKey().equals("mobile") && access.getValue()) {
                var scan = page.adminTopNavbar.openMobile().getScanByApplicationDto(mobApp);
                validation = visibleForUser == (scan != null);
            }

            if (access.getKey().equals("dynamic") && access.getValue()) {
                var scan = page.adminTopNavbar.openDynamic().getLatestScanByApplicationDto(webApp);
                validation = visibleForUser == (scan != null);
            }

            assertThat(validation)
                    .as("User %s %s access to the %s scan", user.getRole(), hasAccess, access.getKey())
                    .isTrue();
        }
    }

    private HashMap<String, Boolean> getAccessMap(AdminUserDTO userRole) {
        var mobilePage = "mobile";
        var staticPage = "static";
        var dynamicPage = "dynamic";
        var accessMap = new HashMap<String, Boolean>();
        switch (userRole.getRole()) {
            case StaticOperator, StaticManager, StaticAuditor -> accessMap.put(staticPage, true);
            case DynamicTester, DynamicManager -> accessMap.put(dynamicPage, true);
            case MobileTester, MobileManager -> accessMap.put(mobilePage, true);
            case TAM, TAMManager, SeniorTAM, Operations -> {
                accessMap.put(mobilePage, true);
                accessMap.put(staticPage, true);
                accessMap.put(dynamicPage, true);
            }
        }

        return accessMap;
    }
}
