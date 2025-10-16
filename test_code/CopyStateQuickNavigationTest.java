package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("355001")
@Owner("vdubovyk@opentext.com")
@Slf4j
public class CopyStateQuickNavigationTest extends FodBaseTest {
    ApplicationDTO webAppDTO;

    @DataProvider(name = "releaseNames")
    public static Object[][] releaseNames() {
        return new Object[][]{
                {"First Release", ""},
                {"Second Release", "First Release"},
                {"Third Release", "Second Release"}
        };
    }

    @MaxRetryCount(2)
    @Description("Test to verify the correctness of the releases after copying from another.")
    @Test(dataProvider = "releaseNames", groups = {"regression"})
    public void copyStateQuickNavigationTest(String releaseName, String copiedFromRelease) {

        boolean expectedState = !"First Release".equals(releaseName);

        LogInActions.tamUserLogin(defaultTenantDTO);

        if (!expectedState) {
            webAppDTO = ApplicationDTO.createDefaultInstance();
            webAppDTO.setReleaseName(releaseName);
            ApplicationActions.createApplication(webAppDTO);
        } else {
            ReleaseDTO releaseDTO = ReleaseDTO.createDefaultInstance();
            releaseDTO.setReleaseName(releaseName);
            releaseDTO.setCopyState(true);
            releaseDTO.setCopyFromReleaseName(copiedFromRelease);
            ReleaseActions.createRelease(webAppDTO, releaseDTO);
        }

        var release = new TenantTopNavbar()
                .openApplications()
                .openDetailsFor(webAppDTO.getApplicationName())
                .getReleaseByName(releaseName);

        assertThat(release.getCopyStateSourceRelease())
                .as("Verifying the source release for '%s'", releaseName)
                .isEqualTo(copiedFromRelease);

        if (expectedState) {
            release.openReleaseDetailsForSourceRelease();
        }

        assertThat(new TenantTopNavbar()
                .openApplications()
                .openYourReleases()
                .getReleaseByAppAndReleaseNames(webAppDTO.getApplicationName(), releaseName)
                .getCopyStateSourceRelease())
                .as("Verifying the source release for '%s'", releaseName)
                .isEqualTo(copiedFromRelease);

        assertThat(new TenantTopNavbar()
                .openApplications()
                .openDetailsFor(webAppDTO.getApplicationName())
                .getReleaseByName(releaseName)
                .openReleaseDetails()
                .getCopyStateLabel()
                .exists())
                .as("Checking if the copy state label exists for release '%s'", releaseName)
                .isEqualTo(expectedState);

        DynamicScanActions.importDynamicScanTenant(webAppDTO, "payloads/fod/dynamic.zero.fpr");
    }
}