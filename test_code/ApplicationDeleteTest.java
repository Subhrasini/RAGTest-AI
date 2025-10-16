package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ApplicationDeleteTest extends FodBaseTest {

    ApplicationDTO webApp;
    StaticScanDTO scanDto;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to create application and static scan")
    @Test(groups = {"regression"})
    public void applicationDeleteTest() {
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);
        scanDto = StaticScanDTO.createDefaultInstance();
        scanDto.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        StaticScanActions.createStaticScan(scanDto, webApp,
                FodCustomTypes.SetupScanPageStatus.Queued);

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Delete Application Negative Test");

        var modal = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(webApp.getApplicationName())
                .openSettings()
                .pressDelete();

        assertThat(modal.getMessage()).as("Confirm modal opened")
                .isEqualTo("Are you sure you want to delete this application and all of its releases? This action is permanent!");

        modal.clickButtonByText("Yes");
        assertThat(modal.getMessage())
                .as("Application with started scan can't be deleted")
                .isEqualTo("The application has one or more active scans and cannot be deleted");

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Cancel Scan Test");

        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.cancelScanTenant(webApp);

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Delete Application Test");

        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationActions.purgeApplication(webApp);
    }
}
