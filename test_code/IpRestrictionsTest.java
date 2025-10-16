package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.exceptions.FodFailLoginException;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Owner("oradchenko@opentext.com")
public class IpRestrictionsTest extends FodBaseTest {
    TenantUserDTO securityLead;

    @MaxRetryCount(3)
    @Description("User should not be able to log in from non-whitelisted IP")
    @Severity(SeverityLevel.NORMAL)
    @Test()
    public void ipRestrictionTest() {
        final String ipRestrictionSetting = "Ip|16.103.230.*";
        final String expectedErrorMessage = "Access denied due to tenant IP restrictions.";
        TenantDTO tenant;
        int tenantId;
        String actualErrorMessage = "";
        tenant = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenant);

        tenantId = Integer.parseInt(new FodSQLUtil().getTenantIdByName(tenant.getTenantName()));

        var page = LogInActions.tamUserLogin(tenant).tenantTopNavbar.openAdministration().openSettings()
                .openSecurityTab();

        page.setIpRestriction(true);
        var ip = page.getUserIpAddress();
        page.pressAddIp().setName("My ip").setIP(ip).save();
        page.pressSave();
        assertThat(page.allowedIpTable.getAllDataRows()).as("Ip table should not be empty").isNotEmpty();
        assertThat(page.allowedIpTable.getCellByIndexes(1, 0).getText())
                .as("Saved ip address should equal entered one").isEqualTo(ip);

        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenant);
        BrowserUtil.clearCookiesLogOff();

        new FodSQLUtil().executeQuery(String.format("UPDATE TenantSetting\n" +
                "SET SettingValue = N'%s'\n" +
                "WHERE TenantId = %d\n" +
                "  AND SettingName LIKE N'IpRestrictions' ESCAPE '#';", ipRestrictionSetting, tenantId));

        Supplier<String> supplier = () -> {
            try {
                LogInActions.tamUserLogin(tenant);
            } catch (FodFailLoginException exception) {
                return exception.getMessage();
            }
            BrowserUtil.clearCookiesLogOff();
            return "";
        };

        actualErrorMessage = WaitUtil.waitFor(
                WaitUtil.Operator.Equals,
                expectedErrorMessage,
                supplier, Duration.ofSeconds(120),
                false);

        assertThat(actualErrorMessage).as("There should be error message about ip restriction")
                .isNotBlank().isEqualTo(expectedErrorMessage);
    }

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("1794002")
    @MaxRetryCount(2)
    @Description("IP Restrictions turned off due to security issue")
    @Severity(SeverityLevel.NORMAL)
    @Test()
    public void ipRestrictionTurnedOffTest() {
        securityLead = TenantUserDTO.createDefaultInstance();
        securityLead.setTenant(defaultTenantDTO.getTenantCode());
        securityLead.setUserName(securityLead.getUserName() + "-SECLEAD");
        securityLead.setRole(FodCustomTypes.TenantUserRole.SecurityLead);
        LogInActions.tamUserLogin(defaultTenantDTO);
        TenantUserActions.createTenantUsers(defaultTenantDTO, securityLead);
        BrowserUtil.clearCookiesLogOff();

        assertThat(LogInActions
                .tenantUserLogIn(securityLead.getUserName(), FodConfig.TAM_PASSWORD, securityLead.getTenant())
                .tenantTopNavbar.openAdministration().openSettings()
                .openSecurityTab().ipRestrictionSwitcher.isEnabled())
                .as("Ip restriction switcher should be enabled")
                .isTrue();
    }
}
