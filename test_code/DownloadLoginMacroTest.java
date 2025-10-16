package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
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

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
@FodBacklogItem("1466001")
public class DownloadLoginMacroTest extends FodBaseTest {
    ApplicationDTO webApp, webApp1, dynamicApp;
    DynamicScanDTO websiteScanDto, websiteScanDto1, dynamicScanDTO;

    //@MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify download login macro file for a site authenticated scans")
    @Test(groups = {"regression"}, priority = 1)
    public void downloadLoginMacroWithSiteAuth() {
        webApp = ApplicationDTO.createDefaultInstance();
        websiteScanDto = DynamicScanDTO.createDefaultDastAutomatedInstance();
        websiteScanDto.setUseSiteAuth(true);
        websiteScanDto.setLoginMacroFile("payloads/fod/API/loginmacro.webmacro");
        websiteScanDto.setScopeScanPolicy(FodCustomTypes.DastAutomatedPolicyType.Passive);

        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(websiteScanDto, webApp, FodCustomTypes.SetupScanPageStatus.Completed);
        var releaseCell = new TenantTopNavbar().openApplications().openYourReleases().openDetailsForRelease(webApp)
                .openScans().getScanByType(FodCustomTypes.ScanType.Dynamic)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        assertThat(releaseCell.getDropdownOptions())
                .as("Download Login Macro new option should be present in the dropdown")
                .contains("Download Login Macro");
        assertThat(releaseCell.downloadLoginMacroFile().getName())
                .as("Verify login Macro file is downloaded from release scans page")
                .contains(".webmacro");
    }

    //@MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify download login macro file for a scan without site authentication")
    @Test(groups = {"regression"})
    public void downloadLoginMacroWithoutSiteAuth() {
        webApp1 = ApplicationDTO.createDefaultInstance();
        websiteScanDto1 = DynamicScanDTO.createDefaultDastAutomatedInstance();
        websiteScanDto1.setScopeScanPolicy(FodCustomTypes.DastAutomatedPolicyType.Passive);

        ApplicationActions.createApplication(webApp1, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(websiteScanDto1, webApp1, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        validateErrorMessageForMacroUpload(webApp1);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify download login macro file for a regular dynamic  scan")
    @Test(groups = {"regression"})
    public void downloadLoginMacroForDynamicScans() {
        dynamicApp = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(dynamicApp.getReleaseName(), "payloads/fod/dynamic.zero.fpr",
                true, true, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, false);
        BrowserUtil.clearCookiesLogOff();
        validateErrorMessageForMacroUpload(dynamicApp);
    }

    private void validateErrorMessageForMacroUpload(ApplicationDTO appDto) {
        var releaseScanCell = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApplications().openYourReleases().openDetailsForRelease(appDto).openScans()
                .getScanByType(FodCustomTypes.ScanType.Dynamic).waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        releaseScanCell.openDropdown().clickDownloadLoginMacro();
        var modal = new ModalDialog();
        assertThat(modal.getMessage())
                .as("Error message should show if login macro doesn't exist")
                .isEqualToIgnoringCase(String.format("Scan %s does not have a login macro file", releaseScanCell.getScanId()));
        modal.pressClose();
    }
}
