package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementCell;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("690017")
@FodBacklogItem("743008")
@Slf4j
public class DebrickedTrialEntitlementTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO app1, app2;
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenants and applications for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        app1 = ApplicationDTO.createDefaultInstance();
        app2 = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setEntitlement("Subscription");
        staticScanDTO.setFileToUpload("payloads/fod/vuln_python_with_pipfilelock.zip");
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON);
        staticScanDTO.setLanguageLevel("3");
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(app1, tenantDTO, true);
        ApplicationActions.createApplication(app2, tenantDTO, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Debricked Trial  entitlement is present inside Tenants admin")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyDebrickedTrialEntitlement() {
        var entitlement = EntitlementDTO.createDefaultInstance();
        entitlement.setQuantityPurchased(2000);
        EntitlementsActions.createEntitlements(tenantDTO, true, entitlement);

        AllureReportUtil.info("Debricked Trial  Entitlement should be present in Entitlements page");
        var openEntitlementsPage = new AdminTopNavbar().openTenants()
                .findTenant(tenantDTO.getTenantName()).openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        assertThat(openEntitlementsPage.tabs.tabExists("Debricked Trial Entitlements"))
                .as("Debricked Trial Entitlements renamed to Debricked Trial Entitlements")
                .isTrue();

        AllureReportUtil.info("Debricked Trial  Entitlement should be consumed after open source scan when its enabled");
        var entitlementsPopup = openEntitlementsPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedTrialEntitlement)
                .pressAddEntitlement();
        assertThat(entitlementsPopup.getTitle())
                .as("Add Entitlement window should call Add Debricked Trial Entitlement")
                .contains("Add Debricked Trial Entitlement");
        entitlementsPopup.setPurchasedQuantity(1000);
        entitlementsPopup.pressSaveButton();
        refresh();
        assertThat(new EntitlementCell().getEnabled())
                .as("'Enabled' column for Debricked Trial  Entitlement should be 'Yes'")
                .isEqualTo("Yes");
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, app1, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(app1, true);
        verifyEntitlementsUsage(tenantDTO, 1000, 1,
                FodCustomTypes.EntitlementType.DebrickedTrialEntitlement, false);
        verifyEntitlementsUsage(tenantDTO, 2000, 11,
                FodCustomTypes.EntitlementType.FortifyEntitlement, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Entitlement usage when Debricked and Debricked Trial entitlements are enabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"verifyDebrickedTrialEntitlement"})
    public void verifyUsageOfDebrickedEntitlements() {
        AllureReportUtil.info("Debricked Entitlement should be consumed after open source scan when both debricked and" +
                " Debricked Trial Entitlements are enabled");
        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(1000);
        EntitlementsActions.createEntitlements(tenantDTO, true, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, app2, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        StaticScanActions.completeStaticScan(app2, true);
        verifyEntitlementsUsage(tenantDTO, 1000, 1,
                FodCustomTypes.EntitlementType.DebrickedEntitlement, false);
        verifyEntitlementsUsage(tenantDTO, 2000, 22,
                FodCustomTypes.EntitlementType.FortifyEntitlement, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify hide  Debricked Trial  or Trial entitlements from tenant portal")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyHideDebrickedTrialEntitlementInTenant() {
        var entitlementPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openAdministration().openEntitlements();
        assertThat(entitlementPage.tabs.tabExists("Debricked Trial Entitlements"))
                .as("Debricked Trial entitlement tab is removed")
                .isFalse();
        assertThat(entitlementPage.tabs.tabExists("Debricked Trial Entitlements"))
                .as("Debricked Trial  entitlement tab is hided from tenant portal")
                .isFalse();
        open(url() + "?et=debrickedtrial");
        assertThat(entitlementPage.tabs.getActiveTab())
                .as("Trial or Trial Entitlements not exists and shows Fortify entitlements page")
                .isEqualTo("Fortify Entitlements");
    }

    public void verifyEntitlementsUsage(TenantDTO tenantDTO,
                                        int expectedPurchased,
                                        int expectedConsumed,
                                        FodCustomTypes.EntitlementType entitlementType,
                                        boolean needLogin) {
        AllureReportUtil.info("Verify entitlements usage");
        if (needLogin) {
            LogInActions.adminLogIn();
        }
        var entitlementsPage = new AdminTopNavbar().openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements()
                .switchEntitlementsTab(entitlementType);
        var entitlement = entitlementsPage.getAll().get(0);
        assertThat(entitlement.getQuantityConsumed())
                .as("Consumed count should match expected count for entitlements" + entitlementType)
                .isEqualTo(expectedConsumed);
        assertThat(entitlement.getQuantityPurchased())
                .as("Purchased count should match expected count for entitlements" + entitlementType)
                .isEqualTo(expectedPurchased);
    }
}
