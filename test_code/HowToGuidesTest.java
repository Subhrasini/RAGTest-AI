package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.WithProxy;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;
import utils.DastBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.open;
import static com.fortify.fod.common.config.FodConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("dgochev@opentext.com")
@DastBacklogItem("800055")

public class HowToGuidesTest extends FodBaseTest {

    private final String settingName = "HowToVideosYoutubePlaylist";
    private final String guideUrl = "https://dki.io/c40a12f5";
    private final String url = "https://share.cds.dominknow.one/";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate How-To Guide page")
    @Test(groups = {"regression"})
    @WithProxy
    public void howToGuidesTest() {
        var siteSettingsPage = open(TEST_ENDPOINT_ADMIN_IP, AdminLoginPage.class)
                .login(ADMIN_USER_NAME, ADMIN_PASSWORD)
                .adminTopNavbar
                .openConfiguration()
                .openSettingByName(settingName)
                .setValue(guideUrl)
                .save();

        assertThat(siteSettingsPage.getSettingByName(settingName).getValue())
                .as("New settings value is not set")
                .isEqualTo(guideUrl);


        open(TEST_ENDPOINT_TENANT_IP, TenantLoginPage.class).loginAsTam(defaultTenantDTO)
                .tenantTopNavbar
                .openHowToGuides();

        assertThat(WebDriverRunner.url())
                .as("How-To Guide url mismatch")
                .contains(url);
    }
}
