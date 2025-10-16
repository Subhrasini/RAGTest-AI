package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;
import utils.FodBacklogItem;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("dgochev@opentext.com")
@FodBacklogItem("1633011")
public class CustomizedUserStaticScanTest extends FodBaseTest {

    TenantUserDTO userDTO;
    private ApplicationDTO applicationDTO;
    private StaticScanDTO staticScanDTO;
    private TenantUserRoleDTO tenantUserRoleDTO;

    void setup() {
        createUser();
        createUserRole();
        createApplication();
        createAndRunScan();
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Verify if user with custom role can start scan")
    @Test(groups = {"regression"})
    void customizedUserStaticScanTest() {
        setup();
        secLeadLogin();
        TenantUserActions.assignApplication(userDTO.getUserName(), applicationDTO);
        TenantUserActions.editUserRole(userDTO.getUserName(), tenantUserRoleDTO.getRoleName());
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tenantUserLogIn(userDTO.getUserName(), userDTO.getPassword(), defaultTenantDTO.getTenantCode());
        var page = StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Completed);

        assertThat(page.getScanStatusFromIcon())
                .as("Scan is not started")
                .containsAnyOf(FodCustomTypes.SetupScanPageStatus.Queued.getTypeValue(),
                        FodCustomTypes.SetupScanPageStatus.InProgress.getTypeValue(),
                        FodCustomTypes.SetupScanPageStatus.Completed.getTypeValue());
    }

    private void secLeadLogin() {
        LogInActions.tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
    }

    void createUser() {
        secLeadLogin();
        userDTO = TenantUserDTO.createDefaultInstance();
        userDTO.setTenant(defaultTenantDTO.getTenantName());
        TenantUserActions.createTenantUser(userDTO);
        BrowserUtil.clearCookiesLogOff();

        secLeadLogin();
        TenantDTO tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setAssignedUser(userDTO.getUserName());
        tenantDTO.setTenantCode(defaultTenantDTO.getTenantCode());
    }

    void createUserRole() {
        tenantUserRoleDTO = TenantUserRoleDTO.createDefaultInstance();
        tenantUserRoleDTO.setDownloadTools(true);
        tenantUserRoleDTO.setAccessTraining(true);
        tenantUserRoleDTO.setIssuePermissions(FodCustomTypes.RoleApplicationIssuePermissions.Edit);
        tenantUserRoleDTO.setReportsPermissions(FodCustomTypes.RoleApplicationReportsPermissions.Create);
        tenantUserRoleDTO.setStartDynamicScanPermissions(FodCustomTypes.RoleStartScanPermissions.Allow);
        tenantUserRoleDTO.setStartStaticScanPermissions(FodCustomTypes.RoleStartScanPermissions.Allow);
        tenantUserRoleDTO.setStartMobileScanPermissions(FodCustomTypes.RoleStartScanPermissions.Allow);
        TenantRoleActions.createRole(tenantUserRoleDTO);
    }

    void createApplication() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, false);
    }

    void createAndRunScan() {
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Automated);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
    }
}
