package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
@FodBacklogItem("760036")
public class ReportScheduledUserCreatedTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    TenantDTO tenantDTO;
    TenantUserDTO securityLead, applicationLead1, applicationLead2;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;

    @MaxRetryCount(1)
    @Description("Admin user should be able to create static and dynamic scans as well as verify copy states for scans including cancelled and paused scans")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();

        securityLead = TenantUserDTO.createDefaultInstance();
        securityLead.setTenant(tenantDTO.getTenantCode());
        securityLead.setUserName(securityLead.getUserName() + "-SECLEAD");
        securityLead.setRole(FodCustomTypes.TenantUserRole.SecurityLead);

        applicationLead1 = TenantUserDTO.createDefaultInstance();
        applicationLead1.setTenant(tenantDTO.getTenantCode());
        applicationLead1.setUserName(applicationLead1.getUserName() + "-APPLEAD1");
        applicationLead1.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        applicationLead2 = TenantUserDTO.createDefaultInstance();
        applicationLead2.setTenant(tenantDTO.getTenantCode());
        applicationLead2.setUserName(applicationLead2.getUserName() + "-APPLEAD2");
        applicationLead2.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        TenantUserActions.createTenantUsers(tenantDTO, securityLead, applicationLead1, applicationLead2);

        LogInActions.tenantUserLogIn(securityLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, false);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(1)
    @Description("Admin user should be able to create static and dynamic scans as well as verify copy states for scans including cancelled and paused scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void assignApplicationDownloadPDF() {
        var tenantTopNav = LogInActions.tenantUserLogIn(securityLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar;

        tenantTopNav.openAdministration()
                .openUsers()
                .pressAssignApplicationsByUser(applicationLead1.getUserName())
                .assignApplication(applicationDTO)
                .pressSave();

        tenantTopNav.openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openReleaseSettings()
                .setOwner(applicationLead1.getLastName())
                .pressSave();

        tenantTopNav.openApplications()
                .getAppByName(applicationDTO.getApplicationName())
                .openDetails()
                .openReports()
                .pressScheduleReportBtn()
                .setStaticScanReportType("Static Summary")
                .setSdlcDevelopment(true)
                .setReportType("PDF")
                .pressSave();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("10");
        staticScanDTO.setFileToUpload("payloads/fod/static.java.zip");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);

        WaitUtil.waitForTrue(() -> !Table.isEmpty(),
                Duration.ofSeconds(30), true);

        var reportsPage = tenantTopNav.openReports();
        var getReports = reportsPage.getAllReports();

        var getMyCreatedBy = getReports.get(0).getCreatedBy();
        assertThat(getMyCreatedBy)
                .as("Created by should reflect the created Security Lead")
                .isEqualTo(securityLead.getUserName());

        var reports = tenantTopNav.openReports()
                .getAllReports().get(0);
        reports.waitForReportStatus(FodCustomTypes.ReportStatus.Completed);
        var theReport = reports.downloadReport();
        assertThat(theReport)
                .as("Report with .pdf extension should be downloaded")
                .isNotEmpty()
                .hasExtension("pdf");

    }

    @MaxRetryCount(1)
    @Description("Admin user should be able to create static and dynamic scans as well as verify copy states for scans including cancelled and paused scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "assignApplicationDownloadPDF"})
    public void assignApplicationDownloadZip() throws ZipException {
        var secLeadTenantTopNav = LogInActions.tenantUserLogIn(securityLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar;

        secLeadTenantTopNav.openAdministration()
                .openUsers()
                .pressAssignApplicationsByUser(applicationLead2.getUserName())
                .assignApplication(applicationDTO)
                .pressSave();

        secLeadTenantTopNav.openApplications()
                .getAppByName(applicationDTO.getApplicationName())
                .openDetails()
                .openReports()
                .pressScheduleReportBtn()
                .setDynamicScanReportType("Dynamic Summary")
                .setSdlcDevelopment(true)
                .setReportType("HTML")
                .pressSave();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tenantUserLogIn(applicationLead2.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications();

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var appLeadTenantTopNav = LogInActions.tenantUserLogIn(applicationLead2.getUserName(),
                FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar;

        var reportsPage = appLeadTenantTopNav.openReports();

        WaitUtil.waitFor(WaitUtil.Operator.Equals, 2, () -> reportsPage.getAllReports(),
                Duration.ofMinutes(2), true);
        var getReports = reportsPage.getAllReports();
        var getMyCreatedBy = getReports.get(1).getCreatedBy();
        assertThat(getMyCreatedBy)
                .as("Created by should reflect the created Security Lead")
                .isEqualTo(securityLead.getUserName());

        var reports = appLeadTenantTopNav.openReports()
                .getAllReports().get(1);
        reports.waitForReportStatus(FodCustomTypes.ReportStatus.Completed);
        var theReport = reports.downloadReport("zip");
        assertThat(theReport)
                .as("Zip file should be downloaded")
                .isNotEmpty()
                .hasExtension("zip");

        String zipFilePath = theReport.getPath();
        String unzippedFilePath = theReport.getParent();

        new ZipFile(zipFilePath).extractAll(unzippedFilePath);

        var unzippedFile = Arrays.stream(Objects.requireNonNull(new File(unzippedFilePath).listFiles()))
                .filter(file -> !file.getName().contains("zip"))
                .findFirst().orElse(null);

        assertThat(unzippedFile).as("Zip file should contain .html").isNotNull().isNotEmpty().hasExtension("html");

    }

}
