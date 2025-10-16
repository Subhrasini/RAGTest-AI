package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.io.File;
import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class ApplicationScansTest extends FodBaseTest {

    ApplicationDTO app;
    StaticScanDTO staticScanDto;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Application Scan Test")
    @Test(groups = {"regression"})
    public void applicationScanTest() throws FileNotFoundException {
        app = ApplicationDTO.createDefaultInstance();
        staticScanDto = StaticScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(app, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDto, app, FodCustomTypes.SetupScanPageStatus.InProgress);
        StaticScanActions.cancelScanTenant(app);
        StaticScanActions.createStaticScan(staticScanDto, app, FodCustomTypes.SetupScanPageStatus.Completed);

        AllureReportUtil.info("<<< Validate Application Scan Summary >>>");
        var appScan = new TenantTopNavbar().openApplications()
                .openDetailsFor(app.getApplicationName())
                .openScans().getAllScans(false)
                .get(0);

        var scanSummary = appScan.pressScanSummary();
        assertThat(scanSummary.getValueByName("Scan Id"))
                .as("Validate 'Scan Id' value")
                .matches("^\\d+$");
        assertThat(scanSummary.getValueByName("Entitlement Type"))
                .as("Validate 'Entitlement Type' value")
                .isEqualTo("Subscription");
        assertThat(scanSummary.getValueByName("Scan third-party libraries for static security assessment"))
                .as("Validate 'third-party libraries' value")
                .isEqualTo("No");
        assertThat(scanSummary.getValueByName("Scan Started"))
                .as("Validate 'Scan Started' value")
                .matches("\\d{4}.\\d{2}.\\d{2} \\d{2}\\:\\d{2}\\:\\d{2} \\D{2}");
        assertThat(scanSummary.getValueByName("Scan Completed"))
                .as("Validate 'Scan Completed' value")
                .matches("\\d{4}.\\d{2}.\\d{2} \\d{2}\\:\\d{2}\\:\\d{2} \\D{2}");
        assertThat(scanSummary.getValueByName("# Issues - Total"))
                .as("Validate '# Issues - Total' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("# Issues -Critical"))
                .as("Validate '# Issues -Critical' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("# Issues - High"))
                .as("Validate '# Issues - High' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("# Issues - Medium"))
                .as("Validate '# Issues - Medium' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("# Issues - Low"))
                .as("Validate '# Issues - Low' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("Pause Count"))
                .as("Validate 'Pause Count' value")
                .isEqualTo("0");
        assertThat(scanSummary.getValueByName("Pause Reasons"))
                .as("Validate 'Pause Reasons' value")
                .isEqualTo("---");
        assertThat(scanSummary.getValueByName("Scan Method"))
                .as("Validate 'Scan Method' value")
                .isEqualTo("Browser");
        assertThat(scanSummary.getValueByName("Scan Tool"))
                .as("Validate 'Scan Tool' value")
                .isEqualTo("WebUI");
        assertThat(scanSummary.getValueByName("Scan Tool Version"))
                .as("Validate 'Scan Tool Version' value")
                .matches("^\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,4}$");
        assertThat(scanSummary.getValueByName("Assessment Type"))
                .as("Validate 'Assessment Type' value")
                .isEqualTo("AUTO-STATIC");
        assertThat(scanSummary.getValueByName("Audit Preference"))
                .as("Validate 'Audit Preference' value")
                .isEqualTo(FodCustomTypes.AuditPreference.Automated.getTypeValue());
        assertThat(scanSummary.getValueByName("Technology Stack"))
                .as("Validate 'Technology stack' value")
                .isEqualTo(FodCustomTypes.TechnologyStack.JAVA.getTypeValue());
        assertThat(scanSummary.getValueByName("Language Level"))
                .as("Validate 'Language Level' value")
                .isEqualTo("1.8");
        scanSummary.pressClose();

        AllureReportUtil.info("<<< Validate Application Manifest Download >>>");
        File manifest;
        manifest = appScan.downloadManifest();
        var expectedManifestFileName = appScan.getScanId() + "_manifest.txt";
        assertThat(manifest)
                .as("File name should be {scan_id}_manifest.txt")
                .hasName(expectedManifestFileName);
        assertThat(manifest.length())
                .as("Manifest file should not be empty")
                .isGreaterThan(50);

        AllureReportUtil.info("<<< Validate Application Results Download >>>");
        File results;
        results = appScan.downloadResults();
        assertThat(results.getName())
                .as("File name should be ****_scandata.fpr")
                .contains("_scandata.fpr");
        assertThat(results.length())
                .as("Results file should not be empty")
                .isGreaterThan(50);
    }
}