package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.applications.application.ApplicationScansPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class SourceCodeDownloadButtonTest extends FodBaseTest {
    private final int sourceFileSize = 7154;
    TenantDTO tenantDTO;
    ApplicationDTO application;
    TenantUserDTO reviewerUser;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create application and static scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Enable Source Download"});
        reviewerUser = TenantUserDTO.createDefaultInstance();
        reviewerUser.setTenant(tenantDTO.getTenantCode());
        reviewerUser.setRole(FodCustomTypes.TenantUserRole.Reviewer);
        application = ApplicationDTO.createDefaultInstance();
        application.setSdlcStatus(FodCustomTypes.Sdlc.Production);

        var entitlement = EntitlementDTO.createDefaultInstance();
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        TenantActions.createTenant(tenantDTO, true, false);
        new AdminTopNavbar().openTenants();
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlement);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        TenantUserActions.createTenantUsers(tenantDTO, reviewerUser);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);

        ApplicationActions.createApplication(application);
        var staticScan = StaticScanDTO.createDefaultInstance();
        staticScan.setFileToUpload("payloads/fod/static.java.zip");
        staticScan.setLanguageLevel("1.8");
        StaticScanActions.createStaticScan(staticScan, application)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Download source code button should be visible for SecLead")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void downloadButtonForSecLeadTest() {
        File sourceCode;

        // Your scans page
        var scanCell = LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openYourScans().getScanByType(FodCustomTypes.ScanType.Static);
        sourceCode = scanCell.downloadSourceCode();
        assertThat(sourceCode)
                .as("Check if file can be downloaded on Your Scans Page, downloaded file size should be 7154")
                .hasSize(sourceFileSize);


        // Application scans page
        var appScanCell = new TenantTopNavbar().openApplications().openYourApplications()
                .openDetailsFor(application.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);
        sourceCode = appScanCell.downloadSourceCode();
        assertThat(sourceCode)
                .as("Check if file can be downloaded on Application Scans Page, downloaded file size should be 7154")
                .hasSize(sourceFileSize);

        // Release scans page
        var releaseScanCell = new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(application)
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);
        sourceCode = releaseScanCell.downloadSourceCode();
        assertThat(sourceCode)
                .as("Check if file can be downloaded on Release Scans Page, downloaded file size should be 7154")
                .hasSize(sourceFileSize);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Download source code button should be visible for TAM")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void downloadButtonTamTest() {
        File sourceCode;
        // Verify as TAM
        var scanCell = LogInActions
                .tamUserLogin(tenantDTO.getAssignedUser(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .openDetailsFor(application.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);

        var tamDropdownOptions = scanCell.openDropdown().getDropdownOptions();
        assertThat(tamDropdownOptions).as("Source code download should be available to TAM").contains("Download Source Code");
        sourceCode = scanCell.downloadSourceCode();
        assertThat(sourceCode)
                .as("Check if file can be downloaded by TAM, downloaded file size should be 7154")
                .hasSize(sourceFileSize);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Download source code button should be visible if user has permission to start static scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void downloadButtonStartStaticScansPermissionTest() {
        File sourceCode;
        var scanCell = LogInActions
                .tenantUserLogIn(reviewerUser.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(application.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);

        var tamDropdownOptions = scanCell.openDropdown().getDropdownOptions();

        assertThat(tamDropdownOptions)
                .as("Source code download should be available to TAM")
                .isNotEmpty()
                .doesNotContain("Download Source Code");
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openRoles()
                .editRole("Reviewer").setStartStaticScans(FodCustomTypes.RoleStartScanPermissions.Allow).save();

        BrowserUtil.clearCookiesLogOff();
        scanCell = LogInActions
                .tenantUserLogIn(reviewerUser.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(application.getApplicationName())
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);


        sourceCode = scanCell.downloadSourceCode();
        assertThat(sourceCode)
                .as("Check if file can be downloaded by TAM, downloaded file size should be 7154")
                .hasSize(sourceFileSize);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("There should be an error when downloading source code from old scan")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void downloadButtonOldScanTest() {
        var daysCount = LogInActions.adminLogIn().adminTopNavbar
                .openConfiguration()
                .getSettingByName("PurgeSourceCodeDays")
                .getValue();

        log.info("PurgeSourceCodeDays = {}", daysCount);
        BrowserUtil.clearCookiesLogOff();

        var scanCell = LogInActions
                .tenantUserLogIn("AUTO-SL", FodConfig.TAM_PASSWORD, "AUTO-TENANT")
                .tenantTopNavbar.openApplications()
                .openDetailsFor("AutoWebApp")
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);

        scanCell.openDropdown().clickDownloadSourceCode();
        assertThat(new ApplicationScansPage().getMainText()).as("There should be error message about expired download")
                .contains(String.format("The requested file exceeds the %s day retention policy.", daysCount));
    }
}
