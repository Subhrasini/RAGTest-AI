package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.popups.AddEditGroupPopup;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("482002")
@Owner("vdubovyk@opentext.com")
@Slf4j
public class GroupDescriptionFieldTest extends FodBaseTest {
    TenantDTO tenantDTO;
    AdminUserDTO tamUser, tamManagerUser, seniorTamUser;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test data preparation for Group Description Field test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantActions.createTenant();
        tamUser = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAM);
        tamUser.setTenant(tenantDTO.getTenantName());
        tamManagerUser = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAMManager);
        tamManagerUser.setTenant(tenantDTO.getTenantName());
        seniorTamUser = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.SeniorTAM);
        seniorTamUser.setTenant(tenantDTO.getTenantName());

        AdminUserActions.createAdminUsers(tamUser, tamManagerUser, seniorTamUser);
        TenantUserActions.activateSecLead(tenantDTO, true);
    }

    @MaxRetryCount(2)
    @Description("Group Description Field test in Tenant Portal")
    @Test(groups = {"regression"}, dataProvider = "userRolesTypes",
            dependsOnMethods = {"testDataPreparation"})
    public void groupDescriptionFieldTest(String userRole) {
        var uniqueRunTag = UniqueRunTag.generate();
        String groupName = "Test group " + uniqueRunTag;
        String groupDescription = "Randomly generated test description + " + uniqueRunTag;
        String newDescription = "New randomly generated test description + " + uniqueRunTag;

        if (userRole.equals(tenantDTO.getUserName())) {
            LogInActions.tenantUserLogIn(userRole, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        } else {
            LogInActions
                    .tamUserLogin(userRole, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        }

        assertThat(
                new TenantTopNavbar()
                        .openAdministration()
                        .openGroups()
                        .addGroup(groupName, groupDescription, false)
                        .findGroup(groupName)
                        .editGroup(groupName)
                        .getDescriptionInput()
                        .text())
                .as("Group description should be the same")
                .isEqualTo(groupDescription);

        assertThat(
                new AddEditGroupPopup()
                        .setDescription(newDescription)
                        .pressSave()
                        .findGroup(groupName)
                        .editGroup(groupName)
                        .getDescriptionInput()
                        .text())
                .as("Group description should be the same")
                .isEqualTo(newDescription);
    }

    @DataProvider(name = "userRolesTypes", parallel = false)
    public Object[][] userRolesTypes() {
        return new Object[][]{
                {tenantDTO.getUserName()},
                {tamUser.getUserName()},
                {seniorTamUser.getUserName()},
                {tamManagerUser.getUserName()}
        };
    }
}