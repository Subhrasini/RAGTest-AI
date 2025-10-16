package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.DastBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("dgochev@opentext.com")
@DastBacklogItem("1264003")
public class OpenSourceComponentsPageTest extends FodBaseTest {

    private String filePath = "payloads/fod/WebGoat_test.zip";
    private ApplicationDTO appDto;

    @BeforeClass
    public void createApplication() {
        appDto = ApplicationActions.createApplication(defaultTenantDTO);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Check if user can navigate to vulnerability details by clicking on the severity icon")
    @Test(groups = {"regression"})
    void vulnerabilityDetailsByClickingSeverityIconTest() {
        var scanDto = StaticScanDTO.createDefaultInstance().toBuilder()
                .openSourceComponent(true).fileToUpload(filePath).build();

        LogInActions.tamUserLogin(defaultTenantDTO);
        var cell =
                StaticScanActions.createStaticScan(scanDto, appDto, FodCustomTypes.SetupScanPageStatus.Completed)
                        .openOpenSourceComponents()
                        .getAllComponentsOnPage().get(0);

        for (var vuln : cell.getVulnerabilities()) {

            AllureReportUtil.info("Verify %s severity".formatted(vuln.getSeverity()));

            if (vuln.getIssueCount() > 0) {
                assertThat(vuln.hasDetails()).as("%s severity is not clickable".formatted(vuln.getSeverity())).isTrue();

                var activeTab = vuln.openDetails().getActiveSeverityTab();
                assertThat(activeTab)
                        .as("Details for %s severity leads to %s tab".formatted(vuln.getSeverity(), activeTab))
                        .isEqualTo(vuln.getSeverity());
                Selenide.back();
            } else {
                assertThat(vuln.hasDetails())
                        .as("%s severity has 0 vulnerabilities, but leads to ReleaseIssuesPage")
                        .isFalse();
            }
        }
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Check if vulnerability file path is present")
    @Test(dependsOnMethods = "vulnerabilityDetailsByClickingSeverityIconTest", groups = {"regression"})
    void vulnerabilityFileLocationTest() {
        var fileLocation =
                LogInActions.tamUserLogin(defaultTenantDTO)
                .getAppByName(appDto.getApplicationName())
                .openDetails()
                .getReleaseByName(appDto.getReleaseName())
                .openReleaseDetails()
                .openIssues()
                .clickCritical()
                .getGroupIssues("Open Source")
                .get(0)
                .openDetails()
                .openVulnerability()
                .getFileLocations();

        assertThat(fileLocation).as("File location is empty").isNotEmpty();

    }
}
