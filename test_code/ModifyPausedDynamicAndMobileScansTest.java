package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ModifyPausedDynamicAndMobileScansTest extends FodBaseTest {

    String expectedTitle = "Scan Settings Updated by Tenant";
    String timeZone = "(UTC-10:00) Hawaii";
    FodCustomTypes.ScansDetailsPageStatus expectedScanStatus = FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to pause dynamic scan and modify it")
    @Test(groups = {"regression"})
    public void modifyPausedDynamicScan() {
        ApplicationDTO dynamicApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(DynamicScanDTO.createDefaultInstance(), dynamicApp,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        var dynamicScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName())
                .pressPauseButton().pressOkButton();
        refresh();
        assertThat(dynamicScanOverviewPage.getStatus())
                .as("Scan status should be changed on : " + expectedScanStatus.getTypeValue())
                .isEqualTo(expectedScanStatus.getTypeValue());

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(dynamicApp)
                .openDynamicScanSetup()
                .setTimeZone(timeZone)
                .pressSaveBtn();

        BrowserUtil.clearCookiesLogOff();

        dynamicScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName());

        assertThat(dynamicScanOverviewPage.getTitle())
                .as("Title should be changed on: " + expectedTitle)
                .contains(expectedTitle);

        assertThat(dynamicScanOverviewPage.getOverviewValueByTitle("Customer Selected Time Zone"))
                .as("Time zone should be changed on: " + timeZone)
                .isEqualTo(timeZone);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to pause mobile scan and modify it")
    @Test(groups = {"regression"})
    public void modifyPausedMobileScan() {
        ApplicationDTO mobileApp = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(MobileScanDTO.createDefaultScanInstance(), mobileApp,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        var mobileScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                .openDetailsFor(mobileApp.getApplicationName())
                .pressPauseButton().pressOkButton();
        refresh();
        assertThat(mobileScanOverviewPage.getStatus())
                .as("Scan status should be changed on : " + expectedScanStatus.getTypeValue()
                        + " or " + FodCustomTypes.ScansDetailsPageStatus.WaitingScanImported.getTypeValue())
                .containsAnyOf(FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue(),
                        FodCustomTypes.ScansDetailsPageStatus.WaitingScanImported.getTypeValue());

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(mobileApp)
                .openMobileScanSetup()
                .setTimeZone(timeZone)
                .pressSaveBtn();

        BrowserUtil.clearCookiesLogOff();

        mobileScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openMobile()
                .openDetailsFor(mobileApp.getApplicationName());

        assertThat(mobileScanOverviewPage.getTitle())
                .as("Title should be changed on: " + expectedTitle)
                .contains(expectedTitle);

        assertThat(mobileScanOverviewPage.getOverviewValueByTitle("Customer Selected Time Zone"))
                .as("Time zone should be changed on: " + timeZone)
                .isEqualTo(timeZone);
    }
}
