package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@Slf4j
public class DeliveryQueueFunctionalityTest extends FodBaseTest {

    TenantDTO tenantDTO;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;
    MobileScanDTO mobileScanDTO;
    ApplicationDTO staticApp, dynamicApp, mobileApp;
    List<String> staticQueueHeaders = Arrays.asList("Scan ID", "Tenant", "Application", "Scan Start Date",
            "Scan Status", "SLA", "Elapsed Time", "Assigned User", "Scan Job Status", "Data Center");
    List<String> dynamicQueueHeaders = Arrays.asList("Scan ID", "Tenant", "Application", "Scan Start Date",
            "Scan Status", "SLA", "Elapsed Time", "Workflow", "Data Center");
    List<String> mobileQueueHeaders = Arrays.asList("Scan ID", "Tenant", "Application", "Scan Start Date",
            "Scan Status", "SLA", "Elapsed Time", "Assigned User", "Workflow", "Data Center", "Phase");

    public void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        staticApp = ApplicationDTO.createDefaultInstance();
        dynamicApp = ApplicationDTO.createDefaultInstance();
        mobileApp = ApplicationDTO.createDefaultMobileInstance();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType("Dynamic Basic");
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare applications and static/dynamic/mobile scan for test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        init();

        TenantActions.createTenant(tenantDTO);

        ApplicationActions.createApplication(staticApp, tenantDTO, true);
        ApplicationActions.createApplication(dynamicApp, tenantDTO, false);
        ApplicationActions.createApplication(mobileApp, tenantDTO, false);

        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validating Static, Dynamic and Mobile page exists and inside pages item exists")
    @Test(groups = {"regression"}, dependsOnMethods = "testDataPreparation")
    public void validatePageAndItemExistsTest() {
        var deliveryQueuePage = AdminLoginPage.navigate()
                .login(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD);

        deliveryQueuePage.openStaticTab();
        assertThat(
                deliveryQueuePage
                        .getDeliveryQueueTable()
                        .getColumnHeaders())
                .as("All headers are present")
                .containsExactlyInAnyOrderElementsOf(staticQueueHeaders);

        deliveryQueuePage.openDynamicTab();
        assertThat(
                deliveryQueuePage
                        .getDeliveryQueueTable()
                        .getColumnHeaders())
                .as("All headers are present")
                .containsExactlyInAnyOrderElementsOf(dynamicQueueHeaders);

        deliveryQueuePage.openMobileTab();
        assertThat(
                deliveryQueuePage
                        .getDeliveryQueueTable()
                        .getColumnHeaders())
                .as("All headers are present")
                .containsExactlyInAnyOrderElementsOf(mobileQueueHeaders);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validating Static, Dynamic and Mobile page Search field by Searching given tenant")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void validateSearchTenantTest() {
        var deliveryQueuePage = AdminLoginPage.navigate()
                .login(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD);

        deliveryQueuePage.openStaticTab();
        deliveryQueuePage.findWithSearchBox(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Static Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());

        deliveryQueuePage.openDynamicTab();
        deliveryQueuePage.findWithSearchBox(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Dynamic Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());

        deliveryQueuePage.openMobileTab();
        deliveryQueuePage.findWithSearchBox(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Mobile Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validating Static, Dynamic and Mobile page Filter functionality by filtering given tenant")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void validateFilterByTenantTest() {
        var deliveryQueuePage = AdminLoginPage.navigate()
                .login(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD);

        deliveryQueuePage.openStaticTab();
        deliveryQueuePage
                .filters
                .expandAllFilters()
                .setFilterByName("Tenant")
                .clickFilterOptionByName(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Static Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());

        deliveryQueuePage.openDynamicTab();
        deliveryQueuePage
                .filters
                .expandAllFilters()
                .setFilterByName("Tenant")
                .clickFilterOptionByName(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Dynamic Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());

        deliveryQueuePage.openMobileTab();
        deliveryQueuePage
                .filters
                .expandAllFilters()
                .setFilterByName("Tenant")
                .clickFilterOptionByName(tenantDTO.getTenantName());
        assertThat(
                deliveryQueuePage
                        .getAllDeliveryQueues()
                        .get(0)
                        .getTenant())
                .as("Verify Mobile Scan row which contains given tenant name in the 'Tenant' field")
                .isEqualTo(tenantDTO.getTenantName());
    }
}