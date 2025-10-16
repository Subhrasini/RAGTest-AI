package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.ui.pages.admin.mobile.mobile_scan_details.mobius.MobiusCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class MobiusTabTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;
    String expectedPlatform = "Android";
    ArrayList<String> expectedMobiusStatuses = new ArrayList<>() {{
        add("Pending");
        add("Sending");
        add("Sent");
        add("Success");
        add("Downloading Results");
        add("Importing Results");
        add("Import Succeeded");
    }};

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create mobile application and scan. Validate Mobius and App Info pages functionality")
    @Test(groups = {"regression"})
    public void mobiusTabTest() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        mobileScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO);

        BrowserUtil.clearCookiesLogOff();

        var overview = LogInActions.adminLogIn().adminTopNavbar
                .openMobile().openDetailsFor(applicationDTO.getApplicationName())
                .openOverview();
        overview.waitForMobiusSuccess();

        var appInfo = overview.openAppInfo();

        assertThat(appInfo.platformElement.text().trim()).isNotBlank().isEqualTo(expectedPlatform);
        assertThat(appInfo.fileSizeElement.text().trim()).isNotBlank();
        assertThat(appInfo.nameElement.text().trim()).isNotBlank();
        assertThat(appInfo.checksumElement.text().trim()).isNotBlank();
        assertThat(appInfo.versionElement.text().trim()).isNotBlank();
        assertThat(appInfo.identifierElement.text().trim()).isNotBlank();
        assertThat(appInfo.minimumOsRequirementsElement.text().trim()).isNotBlank();
        assertThat(appInfo.nativeArchitecturesElement.text().trim()).isNotBlank();
        assertThat(appInfo.deviceRequirementsElement.exists()).isTrue();
        assertThat(appInfo.additionalNotesElement.exists()).isTrue();

        var mobius = appInfo.openMobius();

        assertThat(mobius.getTable().getColumnHeaders())
                .containsExactly("Job Id", "Mobius Scan ID", "Created", "Status", "Message");

        var mobiusCells = mobius.getAllMobiusCells();

        assertThat(mobiusCells).hasSizeGreaterThanOrEqualTo(7);
        var mobiusActualStatuses = mobiusCells.stream()
                .map(MobiusCell::getStatus).collect(Collectors.toList());

        if (mobiusCells.size() == 7) {
            assertThat(mobiusActualStatuses)
                    .containsExactlyElementsOf(expectedMobiusStatuses);
        } else {
            assertThat(mobiusActualStatuses)
                    .containsExactlyInAnyOrderElementsOf(expectedMobiusStatuses);
        }

        assertThat(mobiusActualStatuses.stream().map(String::toLowerCase).collect(Collectors.toList()))
                .doesNotContain("error", "fail", "failure");

        assertThat(mobiusCells.get(0).getMobiusScanId()).hasSize(32);

        var issuesPage = mobius.openIssues();
        assertThat(issuesPage.getAllCount()).isPositive();

        var overviewPage = issuesPage.openOverview().publishScan().waitScanCompleted();
        assertThat(overviewPage.getStatus()).isEqualTo("Completed");
    }
}
