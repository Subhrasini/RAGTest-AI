package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.ResetPasswordPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.open;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class ForgotPasswordTest extends FodBaseTest {
    String targetSubject = "Reset your Password!";
    String targetToList;
    String newPassword = "Spi!pass007^2";

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Forgot Password Test")
    @Test(groups = {"regression"})
    public void forgotPasswordTest() {
        var tenantUser = TenantUserDTO
                .createInstanceWithUserRole(FodCustomTypes.TenantUserRole.SecurityLead);
        tenantUser.setTenant(defaultTenantDTO.getTenantName());
        LogInActions.tenantUserLogIn(defaultTenantDTO.getUserName(),
                FodConfig.ADMIN_PASSWORD, defaultTenantDTO.getTenantCode());
        TenantUserActions.createTenantUser(tenantUser);
        targetToList = tenantUser.getUserEmail();

        BrowserUtil.clearCookiesLogOff();
        TenantLoginPage
                .navigate()
                .clickOnForgotYourPasswordLink()
                .fillFormAndPressSubmit(tenantUser.getUserName(), defaultTenantDTO.getTenantCode());

        Supplier<String> sup = () -> {
            try {
                var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
                Document html = Jsoup.parse(mailBody);
                return html.selectXpath("//*[@href][contains(text(), 'Reset your Password')]")
                        .attr("href");
            } catch (Exception | Error e) {
                log.error(e.getMessage());
                return "";
            }
        };

        WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual, "",
                sup, Duration.ofMinutes(2), false);

        String link = sup.get();
        open(link);
        new ResetPasswordPage().fillFormAndPressOk(newPassword, "some answer");
        LogInActions.tenantUserLogIn(tenantUser.getUserName(), newPassword, defaultTenantDTO.getTenantCode());
    }
}