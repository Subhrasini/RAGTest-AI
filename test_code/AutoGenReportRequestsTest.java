package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AutoGenReportRequestsTest extends FodBaseTest {

    ApplicationDTO webApp, mobileApp;
    ReleaseDTO prodReleaseWebApp, devReleaseWebApp, qaReleaseWebApp, prodReleaseMobileApp, devReleaseMobileApp;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;
    MobileScanDTO mobileScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Auto Generation Reports for different scan types")
    @Test(groups = {"regression"})
    public void autoGenReportRequestsTest() {
        AllureReportUtil.info("Create tenant / applications for test execution");
        testDataPreparationPart1();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Schedule report auto generation");
        scheduleAutoGenReportTest();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create releases / scans for test execution");
        testDataPreparationPart2();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify auto generated report for web production release");
        webAppScheduledAutoGenReportTest();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify auto generated report for mobile dev release");
        mobileAppScheduledAutoGenReportTest();
    }

    public void init() {
        prodReleaseWebApp = ReleaseDTO.createDefaultInstance();
        prodReleaseWebApp.setSdlcStatus(FodCustomTypes.Sdlc.Production);

        devReleaseWebApp = ReleaseDTO.createDefaultInstance();
        devReleaseWebApp.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        qaReleaseWebApp = ReleaseDTO.createDefaultInstance();
        qaReleaseWebApp.setSdlcStatus(FodCustomTypes.Sdlc.QaTest);

        prodReleaseMobileApp = ReleaseDTO.createDefaultInstance();
        prodReleaseMobileApp.setSdlcStatus(FodCustomTypes.Sdlc.Production);

        devReleaseMobileApp = ReleaseDTO.createDefaultInstance();
        devReleaseMobileApp.setSdlcStatus(FodCustomTypes.Sdlc.Development);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
    }

    public void testDataPreparationPart1() {
        webApp = ApplicationDTO.createDefaultInstance();
        mobileApp = ApplicationDTO.createDefaultMobileInstance();

        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, false);
    }

    public void scheduleAutoGenReportTest() {
        var appReportPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openApplications().openDetailsFor(webApp.getApplicationName())
                .openReports();

        assertThat(appReportPage.scheduleReportBtn.exists()).isTrue();
        assertThat(appReportPage.importBtn.exists()).isTrue();
        assertThat(appReportPage.newReportBtn.exists()).isTrue();

        var scheduleReportPopup = appReportPage.pressScheduleReportBtn();
        assertThat(scheduleReportPopup.popup.isDisplayed()).isTrue();

        scheduleReportPopup
                .setDefaultValues()
                .setStaticScanReportType("PCI 3.1 DSS Compliance")
                .pressSave();
        assertThat(scheduleReportPopup.getErrorMessages())
                .contains("Select at least one SDLC Status is required");

        scheduleReportPopup
                .pressCancel()
                .pressScheduleReportBtn()
                .setReportEmailOnComplete(true)
                .setSdlcDevelopment(true)
                .pressSave();
        assertThat(scheduleReportPopup.getErrorMessages())
                .contains("Email Notification List is required");

        scheduleReportPopup
                .pressCancel()
                .pressScheduleReportBtn()
                .setNotificationList("Test").pressSave();
        assertThat(scheduleReportPopup.getErrorMessages())
                .contains("There is an error in the Email Notification List");

        scheduleReportPopup
                .pressCancel()
                .pressScheduleReportBtn()
                .setSdlcDevelopment(false)
                .setReportEmailOnComplete(false)
                .setNotificationList("");
        assertThat(scheduleReportPopup.scheduleReportFileTypeDropDown.$$("option").texts())
                .contains("HTML", "PDF");

        scheduleReportPopup.setStaticScanReportType("PCI 3.1 DSS Compliance")
                .setDynamicScanReportType("PCI 3.1 DSS Compliance")
                .setReportType("PDF")
                .setSdlcProduction(true)
                .pressSave();

        appReportPage.tenantTopNavbar.openApplications().openDetailsFor(mobileApp.getApplicationName())
                .openReports()
                .pressScheduleReportBtn()
                .setDefaultValues()
                .setStaticScanReportType("STIG 6.2 Compliance")
                .setDynamicScanReportType("PCI 3.2 DSS Compliance")
                .setReportType("HTML")
                .setSdlcProduction(false)
                .setSdlcDevelopment(true)
                .pressSave();
    }

    public void testDataPreparationPart2() {
        init();

        LogInActions.tamUserLogin(defaultTenantDTO);
        ReleaseActions.createReleases(webApp, prodReleaseWebApp, devReleaseWebApp, qaReleaseWebApp);
        ReleaseActions.createReleases(mobileApp, prodReleaseMobileApp, devReleaseMobileApp);

        StaticScanActions.createStaticScan(staticScanDTO, webApp, devReleaseWebApp,
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.Scheduled);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp, prodReleaseWebApp.getReleaseName(),
                FodCustomTypes.SetupScanPageStatus.Queued, FodCustomTypes.SetupScanPageStatus.Scheduled);

        MobileScanActions.createMobileScan(defaultTenantDTO, mobileScanDTO, mobileApp, prodReleaseMobileApp, false,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.Queued);

        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.completeMobileScan(mobileApp, prodReleaseMobileApp, null, false, true);
        BrowserUtil.clearCookiesLogOff();

        MobileScanActions.createMobileScan(defaultTenantDTO, mobileScanDTO, mobileApp, devReleaseMobileApp, true,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.Queued);

        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(webApp, true);
        DynamicScanActions.completeDynamicScanAdmin(webApp, false);

        MobileScanActions.completeMobileScan(mobileApp, devReleaseMobileApp, null, false, false);
    }

    public void webAppScheduledAutoGenReportTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        var date = LocalDate.now().format(formatter);

        var reports = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(webApp.getApplicationName())
                .openReports().getAllReports();

        assertThat(reports).hasSize(1);
        var report = reports.get(0);
        report.waitForReportStatus(FodCustomTypes.ReportStatus.Completed);
        assertThat(report.getReportName())
                .matches(prodReleaseWebApp.getReleaseName() + "_" + date + "\\d{4}" + "_\\d{4}");
        var reportFile = report.downloadReport();
        assertThat(reportFile).isNotEmpty().hasExtension("pdf");
        assertThat(reportFile.getName())
                .matches(prodReleaseWebApp.getReleaseName() + "_" + date + "\\d{4}" + "_\\d+?\\.pdf");
    }

    @SneakyThrows(ZipException.class)
    public void mobileAppScheduledAutoGenReportTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        var date = LocalDate.now().format(formatter);

        var reports = LogInActions.tamUserLogin(defaultTenantDTO)
                .openDetailsFor(mobileApp.getApplicationName())
                .openReports().getAllReports();

        assertThat(reports).hasSize(1);
        var report = reports.get(0);
        report.waitForReportStatus(FodCustomTypes.ReportStatus.Completed);
        assertThat(report.getReportName())
                .matches(devReleaseMobileApp.getReleaseName() + "_" + date + "\\d{4}" + "_\\d{4}");
        var reportFile = report.downloadReport("zip");
        assertThat(reportFile).isNotEmpty().hasExtension("zip");

        String zipFilePath = reportFile.getPath();
        String unzippedFilePath = reportFile.getParent();

        new ZipFile(zipFilePath).extractAll(unzippedFilePath);

        var unzippedFile = Arrays.stream(Objects.requireNonNull(new File(unzippedFilePath).listFiles()))
                .filter(file -> !file.getName().contains("zip"))
                .findFirst().orElse(null);

        assertThat(unzippedFile).isNotNull().isNotEmpty().hasExtension("html");
    }
}
