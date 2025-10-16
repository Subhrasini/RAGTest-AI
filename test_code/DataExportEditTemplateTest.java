package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.DataExportDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.reports.dataexport.DataExportPage;
import com.fortify.fod.ui.pages.tenant.reports.dataexport.DataExportPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.DataExportActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;

@FodBacklogItem("1750004")
@Owner("agoyal3@opentext.com")
public class DataExportEditTemplateTest extends FodBaseTest {

    static DataExportDTO dataExportDTO;
    static DataExportPage dataExportPage;

    String expectedDate = LocalDate.now(Clock.system(ZoneId.of("America/Los_Angeles"))).toString();

    @FodBacklogItem("1750004")
    @Owner("agoyal3@opentext.com")
    @Test(groups = {"regression"})
    public static String prepareTestData(String tempType) {
        AllureReportUtil.info("Create a data export for each type of template");

        LogInActions.tamUserLogin(defaultTenantDTO);

        dataExportDTO = DataExportDTO.createDefaultInstance();
        dataExportDTO.setTemplate(FodCustomTypes.DataExportTemplate.valueOf(tempType));
        dataExportDTO.setExportName(tempType + "_" + "template");

        dataExportPage = DataExportActions.createExport(dataExportDTO);
        return dataExportDTO.getExportName();
    }

    @FodBacklogItem("1750004")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the functionality of the data export edit template")
    @Test(priority = 1, groups = {"regression"}, dataProvider = "templateTypes")
    public void dataExportEditTemplateTest(String templateType) {

        String templateName = prepareTestData(templateType);
        sleep(5000);
        refresh();

        DataExportPopup dataExportPopup = dataExportPage.getDataExportByName(templateName).pressEdit();

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(dataExportPopup.dataExportWizardModal.isDisplayed())
                .as("Verify the data export wizard is opened")
                .isTrue();

        if (templateType.equals("Issues"))
            dataExportPopup.pressNextButton().setFilterValue("SAST Aviator", "true")
                    .pressNextButton().pressNextButton();
        else if (templateType.equals("Scans"))
            dataExportPopup.pressNextButton().setDateFrom(-10).setDateTo(1).
                    setFilterValue("Scan Type", "Static").pressNextButton().pressNextButton();
        else if (templateType.equals("Applications") || templateType.equals("ApplicationRelease"))
            dataExportPopup.pressNextButton().setFilterValue("Scan Type", "Static")
                    .pressNextButton();
        else if (templateType.equals("EntitlementConsumption"))
            dataExportPopup.setExportTemplate(FodCustomTypes.DataExportTemplate.Applications)
                    .pressNextButton().pressNextButton();

        WaitUtil.waitForTrue(() -> {
            if (!dataExportPopup.getActiveStep().equals("Summary")) {
                dataExportPopup.pressNextButton();
            }
            return dataExportPopup.getActiveStep().equals("Summary");
        }, Duration.ofMinutes(3), false);

        dataExportPage = dataExportPopup.pressSaveButton();

        softAssertions.assertThat(dataExportPopup.dataExportWizardModal.isDisplayed())
                .as("Verify the data export wizard is closed")
                .isFalse();

        dataExportPage.getDataExportByName(templateName).pressRunExportNow();
        refresh();

        String actualCreatedDate = dataExportPage.getDataExportByName(templateName)
                .getCreatedDate(0).split(":")[1].replace("/", "-");

        softAssertions.assertThat(actualCreatedDate)
                .as("Verify the file creation date")
                .isEqualTo(expectedDate);

        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        File dataExportedFile = dataExportPage.getDataExportByName(templateName).download(0);

        if (templateType.equals("Issues")) {

            List<String> fAColumnToValidate = new ArrayList();
            fAColumnToValidate.add("SAST Aviator");

            var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(fAColumnToValidate);

            softAssertions.assertThat(dataFromCsv.size() > 0)
                    .as("Verify the number of records are exist against the record as FA ")
                    .isTrue();
        } else if (templateType.equals("Applications") || templateType.equals("ApplicationRelease")
                || templateType.equals("Scans")) {
            List<String> scanColumnToValidate = new ArrayList();
            scanColumnToValidate.add("Scan Type");

            var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(scanColumnToValidate);

            softAssertions.assertThat(dataFromCsv.size() > 0)
                    .as("Verify the number of records are exist against the  ")
                    .isTrue();
        }

        dataExportPage.getDataExportByName(templateName).deleteTemplateFile(0);
        dataExportPage.getDataExportByName(templateName).deleteTemplateFile(0);

        softAssertions.assertThat(dataExportPage.getDataExportByName(templateName).getNumberOfFilesExist())
                .as("Verify all the generated files gets deleted for a template")
                .isEqualTo(0);

        dataExportPage.getDataExportByName(templateName).deleteTemplate();

        softAssertions.assertAll();
    }

    @DataProvider(name = "templateTypes")
    public Object[][] templateTypes() {
        return new Object[][]
                {
                        {"Issues"},
                        {"Scans"},
                        {"Applications"},
                        {"ApplicationReleases"},
                        {"EntitlementConsumption"}
                };
    }
}

