package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
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

@Owner("sbehera3@opentext.com")
@Slf4j
public class MobileScanTenantActivityTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare mobile application and mobile scan")
    @Test(groups = {"hf", "regression"})
    public void testDataPreparation() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("372002")
    @Description("Clicking a mobile scan at the Tenant Activity page returns error")
    @Test(dependsOnMethods = {"testDataPreparation"}, groups = {"hf", "regression"})
    public void validateRedirectionToMobScansPage() {

        var tenantActivityPage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openTenants().openTenantActivity();
        tenantActivityPage.findWithSearchBox(defaultTenantDTO.getTenantName());
        tenantActivityPage.getTable().waitForEntryAppear(defaultTenantDTO.getTenantName(), 0, 15);
        var mobileQueuePage = tenantActivityPage.getCellByTenantName(defaultTenantDTO.getTenantName()).clickMobileScan();
        assertThat(mobileQueuePage.adminTopNavbar.getActiveTab())
                .as("Verify the active tab in adminTopNavBar")
                .isEqualTo("Mobile");
        assertThat(mobileQueuePage.tabs.getActiveTab())
                .as("Verify active tab under Mobile tab")
                .isEqualTo("Scans");
        assertThat(mobileQueuePage.appliedFilters.getFilterByName("Tenant:").getValue())
                .as("Verify default applied filter in mobile scans page")
                .isEqualTo(defaultTenantDTO.getTenantName());
        boolean isPresent = false;
        for (var scan : mobileQueuePage.getAllScans()) {
            if (scan.getApplication().equals(applicationDTO.getApplicationName()) && scan.getRelease().equals(applicationDTO.getReleaseName())) {
                isPresent = true;
                break;
            }
        }
        assertThat(isPresent)
                .as("Verify application name and release name in the scan row under the tenant")
                .isTrue();
    }
}
