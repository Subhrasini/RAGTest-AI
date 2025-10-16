package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.admin.user_menu.ChangePasswordPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.LocalTime;

import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class TenantChangePasswordTest extends FodBaseTest {

    public String settingName = "RequiredPasswordLength";
    public String newValue = null;
    public String existingValue = null;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Set RequiredPasswordLength flag in admin SiteSettingsPage")
    @Test(groups = {"hf", "regression"})
    public void setRequiredPasswordLengthFlag() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        existingValue = new AdminTopNavbar().openConfiguration().getSettingValueByName(settingName);
        newValue = String.valueOf(Integer.parseInt(existingValue) - 1);
        SiteSettingsActions.setValueInSettings(settingName, newValue, false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("534002")
    @Description("Verify error message while changing password with character length that doesn't satisfy RequiredPasswordLength site setting")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"setRequiredPasswordLengthFlag"})
    public void verifyErrorMessage() {

        ChangePasswordPage changePasswordPage;
        var isApplied = false;
        var newPassword = UniqueRunTag.generate().substring(0, Integer.parseInt(newValue) - 2);
        var endTime = LocalTime.now().plusMinutes(2);
        do {
            changePasswordPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAccountSettings().pressChangePassword();
            changePasswordPage.setOldPass(FodConfig.TAM_PASSWORD).setNewPass(newPassword).setConfirmPass(newPassword).pressOkBtn();
            isApplied = changePasswordPage.getValidationErrorMessage()
                    .equals("Passwords must be at least " + newValue + " characters long, must not contain easy common password phrases, and have at least: 1 capital letter, 1 lower case letter, 1 number and 1 special character.");
            if (isApplied)
                break;

            BrowserUtil.clearCookiesLogOff();
            sleep(20000);
        } while (LocalTime.now().isBefore(endTime));

        assertThat(isApplied).as("Password length site setting value exist in error message").isTrue();

    }

    @AfterClass
    public void setRequiredPassLenFlagToDefaultValue() {
        setupDriver("setRequiredPassLenFlagToDefaultValue");
        SiteSettingsActions.setValueInSettings(settingName, existingValue, true);
        attachTestArtifacts();
    }
}
