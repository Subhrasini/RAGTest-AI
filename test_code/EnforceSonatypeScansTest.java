package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.applications.release.static_scan_setup.StaticScanSetupPage;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationsPage;
import com.fortify.fod.ui.pages.tenant.applications.your_releases.YourReleasesPage;
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

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class EnforceSonatypeScansTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO webApp;

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant on admin site.")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        var entitlement = EntitlementDTO.createDefaultInstance();
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        var tenantPage = page(TenantDetailsPage.class);
        tenantPage.adminTopNavbar.openTenants();
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlement);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);

        String query = String.format(
                "insert into TenantSetting(TenantId, SettingName, SettingValue) values((select TenantId from TenantMaster where TenantName = '%s'), 'MandatorySonatypeScan', 'True');", tenantDTO.getTenantCode());
        new FodSQLUtil().executeQuery(query).close();
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should see sonatype message about mandatory scanning for specific tenant")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void enforceSonatypeScansTest() {
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, tenantDTO, true);
        var releaseDetailsPage = page(ReleaseDetailsPage.class);
        releaseDetailsPage.openStaticScanSetup();
        var staticScanSetupPage = page(StaticScanSetupPage.class);
        staticScanSetupPage.chooseAssessmentType("AUTO-STATIC");
        staticScanSetupPage.chooseEntitlement("Subscription");
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("1.9");
        staticScanSetupPage.chooseAuditPreference(FodCustomTypes.AuditPreference.Automated);
        staticScanSetupPage.pressSaveBtn();
        Selenide.refresh();

        assertThat(staticScanSetupPage.getSonatypeCheckbox().isDisabled())
                .as("Sonatype checkbox should be disabled").isTrue();
        assertThat(staticScanSetupPage.getSonatypeMessage())
                .as("Check if sonatype message exists")
                .containsIgnoringCase("Sonatype scanning has been made mandatory for all applications under this tenant.");
        assertThat(staticScanSetupPage.getSonatypeCheckbox().checked())
                .as("Sonatype checkbox should be checked").isTrue();

    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to crete static scan from tenant site")
    @Test(groups = {"regression"}, dependsOnMethods = {"enforceSonatypeScansTest"})
    public void createStaticScanTest() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("1.9");
        staticScanDTO.setFileToUpload("payloads/fod/10JavaDefectsAnt.zip");

        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        StaticScanActions.createStaticScan(staticScanDTO, webApp);
        StaticScanActions.completeStaticScan(webApp, true);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should see completed statuses of static scan on tenant site and check number of open source issues")
    @Test(groups = {"regression"}, dependsOnMethods = {"createStaticScanTest"})
    public void openSourceComponentsTest() {
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        var applicationsPage = page(YourApplicationsPage.class);
        applicationsPage.openYourReleases();
        var yourReleasesPage = page(YourReleasesPage.class);
        var releaseDetailsPage = yourReleasesPage
                .openDetailsForRelease(webApp.getApplicationName(), webApp.getReleaseName());
        assertThat(releaseDetailsPage.getStaticScanStatus())
                .as("Status should be completed").isEqualToIgnoringCase("Completed");

        releaseDetailsPage.openScans();
        var releaseScansPage = page(ReleaseScansPage.class);
        var status = releaseScansPage
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed).getStatus();
        assertThat(status).as("Status should be completed for Open Source Scan").isEqualToIgnoringCase("Completed");
        releaseScansPage.openIssues();
        var issuesPage = page(ReleaseIssuesPage.class);
        issuesPage.groupBy("Category");
        int opensourceIssuesCount = issuesPage.getGroupIssues("Open Source").size();
        assertThat(opensourceIssuesCount).as("Issues count should equal 2").isEqualTo(2);
    }
}
