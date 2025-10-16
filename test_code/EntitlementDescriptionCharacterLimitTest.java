package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class EntitlementDescriptionCharacterLimitTest extends FodBaseTest {

    TenantDTO tenantDTO;
    EntitlementDTO sonatypeEntitlement;
    ApplicationDTO applicationDTO_1, applicationDTO_2;
    StaticScanDTO staticScanDTO;
    int quantityPurchased = 1;

    @Owner("oradchenko@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to edit entitlement description, expected maximum length is 50 characters")
    @Test(groups = {"regression"})
    public void entitlementDescriptionCharacterLimitTest() {
        var allowedString = new String(new char[50]).replace('\0', 'z');
        var exceededLengthString = new String(new char[55]).replace('\0', 'e');

        var entitlementsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openEntitlements();
        var entitlementIds = entitlementsPage.getActiveTable().getAllColumnValues(0);
        var popup = entitlementsPage.pressEditDescription(entitlementIds.get(0));
        popup.setDescription(allowedString);
        assertThat(popup.getDescriptionValue().length())
                .as("Check if allowed length is actually allowed")
                .isEqualTo(allowedString.length());

        popup.setDescription(exceededLengthString);
        assertThat(popup.getDescriptionValue().length()).as("Check if exceeded value is limited to 50").isEqualTo(50);
    }

    @MaxRetryCount(1)
    @Owner("sbehera3@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("801052")
    @Description("Unable to Consume Newly added Sonatype/Debricked Entitlement if existing Used Entitlement are still Active")
    @Test(groups = {"regression", "hf"})
    public void activeEntitlementTest() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        applicationDTO_1 = ApplicationDTO.createDefaultInstance();
        applicationDTO_2 = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setFileToUpload("payloads/fod/Sonatype vulns with yellow banner_CUT.zip");
        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(quantityPurchased);

        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(5);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(applicationDTO_1);

        AllureReportUtil.info("Create and complete scans, verify entitlement usage");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO_1);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO_1, true);
        assertThat(new AdminTopNavbar().openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements()
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement).getAll().get(0)
                .getQuantityConsumed())
                .as("Sonatype entitlements: consumed count should match expected")
                .isEqualTo(quantityPurchased);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(applicationDTO_2);
        AllureReportUtil.info("Create another scan to verify The portal should allow the new application to consume " +
                "the available Sonatype/Debricked Entitlement");
        assertThat(StaticScanActions.createStaticScan(staticScanDTO, applicationDTO_2,
                FodCustomTypes.SetupScanPageStatus.InProgress).getScanStatusFromIcon())
                .as("Validate that user is able to start a scan")
                .contains(FodCustomTypes.SetupScanPageStatus.InProgress.getTypeValue());

    }

}
