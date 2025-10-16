package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AutoStartDynamicScanTest extends FodBaseTest {
    DynamicScanDTO dynamicScan;
    ApplicationDTO app;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate that default web inspect scanner present and available")
    @Test(groups = {"regression"})
    public void validateScannerStatus() {
        var scannersPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic().openScannerFarm();
        var scanners = scannersPage.getAllScanners();
        assertThat(scanners).hasSizeGreaterThan(0);
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(FodConfig.TEST_ENDPOINT_ADMIN);
        var expectedScannerName = m.find() ? "FODQA" + m.group(0) + "-WI" : "11";

        var scanner = scannersPage.getScannerByName(expectedScannerName);
        assertThat(scanner)
                .as("Scanner with name " + expectedScannerName + "should be present")
                .isNotNull();
        assertThat(scanner.getScannerStatus())
                .as("Scanner should be available")
                .satisfiesAnyOf(
                        param -> assertThat(param).isEqualTo("Available"),
                        param -> assertThat(param).contains("Claimed")
                );
    }

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("1771009")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify dynamic scans with a future date should not start immediately")
    @Test(groups = {"regression","hf"})
    public void scheduledDynamicScanWithFutureDate(){
        app= ApplicationDTO.createDefaultInstance();
        dynamicScan = DynamicScanDTO.createDefaultInstance();
        dynamicScan.setStartInFuture(true);

        AllureReportUtil.info("Create application and start a dynamic scan with future date");
        ApplicationActions.createApplication(app, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScan, app, FodCustomTypes.SetupScanPageStatus.Scheduled);
        sleep(300000L);
        BrowserUtil.clearCookiesLogOff();

        var dynamicQueuePage=LogInActions.adminLogIn().adminTopNavbar.openDynamic();
        dynamicQueuePage.appliedFilters.clearAll();
        dynamicQueuePage.findWithSearchBox(app.getApplicationName());
        assertThat(dynamicQueuePage.getAnalysisStatuses())
                .as("Scan should be in scheduled status and should not changed to In Progress status")
                .containsOnly("Scheduled");
    }
}
