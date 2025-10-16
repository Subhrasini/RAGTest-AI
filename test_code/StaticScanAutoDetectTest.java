package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("1610018")
@Owner("pgaikwad@opentext.com")
@Slf4j
public class StaticScanAutoDetectTest extends FodBaseTest {
    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Auto Detect for Python and Java Payload")
    @Test(groups = {"regression"}, dataProvider = "payloads")
    public void technologyStackAutoDetect(String path, String langStack, String langLevel) {
        webApp = ApplicationDTO.createDefaultInstance();

        var advancedSettingsPanel = ApplicationActions.createApplication(webApp, defaultTenantDTO, true)
                .pressStartStaticScan().getAdvancedSettingsPanel();

        WaitUtil.waitForTrue(() -> advancedSettingsPanel.expand().getTechnologyStackDropdown().isDisplayed(),
                Duration.ofMinutes(1), false);

        WaitUtil.waitForTrue(() -> {
                    if (advancedSettingsPanel.expand().getTechnologyStackDropdown().getSelectedOption().equals("Auto Detect")) {
                        return true;
                    }
                    return false;
                },
                Duration.ofMinutes(1), true);

        assertThat(advancedSettingsPanel.getSelectedTechnologyStack())
                .as("Validation of Default Technology Stack Selected")
                .isEqualTo("Auto Detect");

        AllureReportUtil.info("Validating Auto Detect for the given Payload : "
                + path.split("-")[0].split("/")[2]
                + " " + path.split("-")[1].split("\\.")[0]);
        createStaticScanAndValidate(path, langStack, langLevel);
    }

    private void createStaticScanAndValidate(String payload, String expectedTechStack, String expectedLangLevel) {
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload(payload);
        var scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp,
                FodCustomTypes.SetupScanPageStatus.Completed);
        var scanSummary = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).pressScanSummary();

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(scanSummary.getValueByName("Technology Stack"))
                .as("Validate Technology Stack for uploaded payload")
                .isEqualTo(expectedTechStack);
        softAssertions.assertThat(scanSummary.getValueByName("Language Level"))
                .as("Validation language level for uploaded payload")
                .isEqualTo(expectedLangLevel);
        softAssertions.assertAll();

        scanSummary.pressClose();
    }

    @DataProvider(name = "payloads")
    public Object[][] payloads() {
        return new Object[][]{
                {"payloads/fod/Python-3.zip", "PYTHON", "3"},
                {"payloads/fod/PythonDjango-4.zip", "PYTHON", "5.0 (Django)"},
                {"payloads/fod/Java-17.zip", "JAVA/J2EE/Kotlin", "17"},
                {"payloads/fod/Java-7.zip", "JAVA/J2EE/Kotlin", "1.8"}
        };
    }
}
