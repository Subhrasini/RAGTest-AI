package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.EntitlementsActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;


@Owner("oradchenko@opentext.com")
@Slf4j
public class PurgeTenantTest extends FodBaseTest {

    TenantDTO tenant, tenantA, tenantB;
    ApplicationDTO webApp, webApp1, webApp2;
    EntitlementDTO debrickedEntitlement;
    StaticScanDTO debrickedStaticScanDto;
    String debrickedFilePath = "payloads/fod/vuln_python_with_pipfilelock.zip";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant and application")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenant = TenantDTO.createDefaultInstance();
        tenant.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenant, true);
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, tenant, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to import scan from tenant side.")
    @Test(groups = {"regression"}, dependsOnMethods = "testDataPreparation")
    public void importScanTest() {
        StaticScanActions.importScanTenant(tenant, webApp, "payloads/fod/static.java.fpr", true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to purge tenant on admin site.")
    @Test(groups = {"regression"}, dependsOnMethods = "importScanTest")
    public void purgeTenantTest() {
        TenantActions.purgeTenant(tenant.getTenantName(), true, true, true);
    }

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("800043")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create two tenants and entitlements")
    @Test(groups = {"regression", "hf"})
    public void prepareDataForOssTest() {
        tenantA = TenantDTO.createDefaultInstance();
        tenantA.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        tenantB = TenantDTO.createDefaultInstance();
        tenantB.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(100);

        TenantActions.createTenant(tenantA, true);
        TenantActions.createTenant(tenantB, false);
        EntitlementsActions.createEntitlements(tenantA, false, debrickedEntitlement);
        EntitlementsActions.createEntitlements(tenantB, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();
        debrickedStaticScanDto = StaticScanDTO.createDefaultInstance();
        debrickedStaticScanDto.setFileToUpload(debrickedFilePath);
        debrickedStaticScanDto.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON);
        debrickedStaticScanDto.setLanguageLevel("3");
        debrickedStaticScanDto.setOpenSourceComponent(true);
    }

    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Oss components count after purging one tenant")
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareDataForOssTest"})
    public void verifyOssComponentAfterPurgingTenant() {
        webApp1 = ApplicationDTO.createDefaultInstance();
        webApp2 = ApplicationDTO.createDefaultInstance();
        AllureReportUtil.info("Create application and run open source scan for TenantA");
        ApplicationActions.createApplication(webApp1, tenantA, true);
        StaticScanActions.createStaticScan(debrickedStaticScanDto, webApp1, FodCustomTypes.SetupScanPageStatus.Completed);
        var releaseCell1 = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(webApp1)
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        int scanId1 = releaseCell1.getScanId();
        int totalCount1 = releaseCell1.getTotalCount();
        assertThat(getVulnerabilityCount(scanId1))
                .as("Scan should have some oss vulnerabilities")
                .isEqualTo(totalCount1);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create application and run open source scan for TenantB with same payload");
        ApplicationActions.createApplication(webApp2, tenantB, true);
        StaticScanActions.createStaticScan(debrickedStaticScanDto, webApp2, FodCustomTypes.SetupScanPageStatus.Completed);
        var releaseCell2 = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(webApp2)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        int scanId2 = releaseCell2.getScanId();
        int totalCount2 = releaseCell2.getTotalCount();
        assertThat(getVulnerabilityCount(scanId2))
                .as("Scan should have some oss vulnerabilities")
                .isEqualTo(totalCount2);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Purge TenantA from admin site");
        TenantActions.purgeTenant(tenantA.getTenantName(), true, true, false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify OssComponentsCount after purging TenantA");
        assertThat(getVulnerabilityCount(scanId1))
                .as("Oss components count should be zero for the purged tenant")
                .isZero();
        assertThat(getVulnerabilityCount(scanId2))
                .as("Oss components count should remain same for unpurged tenant")
                .isEqualTo(totalCount2);
    }

    private int getVulnerabilityCount(int scanId) {
        var query = "select count(*) from (select distinct OpenSourceComponentId from OpenSourceVulnerabilityFinding  where" +
                " scanid=" + "'" + scanId + "'" + ")osvf left join  OpenSourceComponentVulnerability oscv on " +
                "oscv.OpenSourceComponentId = osvf.OpenSourceComponentId";
        return Integer.parseInt(new FodSQLUtil().getStringValueFromDB(query));
    }
}
