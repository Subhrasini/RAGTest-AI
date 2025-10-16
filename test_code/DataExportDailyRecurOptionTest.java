package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DataExportDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import com.opencsv.CSVReader;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.LocalDate.now;

@Owner("tmagill@opentext.com")
@FodBacklogItem("596048")
@Slf4j
public class DataExportDailyRecurOptionTest extends FodBaseTest {

    String staticFpr = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    String[] columnHeader = {"Audited Timestamp"};
    String openSourceJsonFile = "payloads/fod/21210_51134_cyclonedx.json";
    String commentValue = "Test comment for open source";

    @SneakyThrows
    @MaxRetryCount(2)
    @Description("Tam User should create export, check initial run time and ensure that export runs at scheduled time")
    @Test(groups = {"regression"},
            dataProvider = "dataExport", dataProviderClass = DataExportDailyRecurOptionTest.class)
    public void dataExportDailyRecurOptionTest(FodCustomTypes.DataExportTemplate exportTemplate) {
        setTestCaseName("Data Export Daily Recur Option with export type: " + exportTemplate.getTypeValue());
        var dataExportDTO = DataExportDTO
                .createDTOWithTemplate(exportTemplate);
        dataExportDTO.setRecurring(true);
        if (exportTemplate.equals(FodCustomTypes.DataExportTemplate.Issues)) {
            Date tomorrow = now().plusDays(1).toDate();
            dataExportDTO.setDayOfTheWeek(
                    new SimpleDateFormat("EEEE", Locale.ENGLISH).format(tomorrow.getTime()));
        } else {
            dataExportDTO.setRecurOption("Daily");
        }
        LogInActions.tamUserLogin(defaultTenantDTO);
        DataExportActions.createExport(dataExportDTO);

        /* To avoid of Timezone issue in this test need to check today date and today + 1 day */
        LocalDate dateToValidate = LocalDate.now();
        var dateToValidate2 = dateToValidate.plusDays(1);

        var db = new FodSQLUtil();
        var query =
                "select NextStartDateTime from ExportTemplate where Name = " + "'" + dataExportDTO.getExportName()
                        + "'" + ";";

        WaitUtil.waitForTrue(() -> !db.getStringValueFromDB(query).isEmpty(), Duration.ofMinutes(2), false);
        var dateFromQuery = db.getStringValueFromDB(query);

        assertThat(dateFromQuery)
                .as("Query result: " + dateFromQuery + "should contain " + dateToValidate
                        + " or " + dateToValidate2.toDate())
                .containsAnyOf(dateToValidate.toString(), dateToValidate2.toString());

        BrowserUtil.clearCookiesLogOff();

        String dateTimePattern = "yyyy-MM-dd HH:mm:ss.SS";

        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime addTime = localDateTime.plusMinutes(2).minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePattern);
        String formatFutureTime = addTime.format(formatter);

        var updateQuery =
                "Update ExportTemplate set NextStartDateTime = " + "'"
                        + formatFutureTime + "'" + " where Name = "
                        + "'" + dataExportDTO.getExportName() + "'" + ";";

        db.executeQuery(updateQuery);

        var validationQuery =
                "select NextStartDateTime from ExportTemplate where Name = "
                        + "'" + dataExportDTO.getExportName() + "'" + ";";
        var validatedQuery = db.getStringValueFromDB(validationQuery);
        db.close();

        assertThat(validatedQuery)
                .as("Set time in DB " + validatedQuery + "should be equal to " + formatFutureTime)
                .isEqualTo(formatFutureTime);

        var dataExportPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openReports()
                .openDataExport();
        var file = dataExportPage.getDataExportByName(dataExportDTO.getExportName())
                .download(0, Duration.ofMinutes(5));
        assertThat(file.getName())
                .as("Export file should contain " + dataExportDTO.getExportName())
                .contains(dataExportDTO.getExportName());
        var allColumnHeader = new ArrayList<String>();
        if (exportTemplate.equals(FodCustomTypes.DataExportTemplate.Issues)) {
            CSVReader reader = new CSVReader(new FileReader(file));
            for (var header : reader.readNext()) {
                allColumnHeader.add(header);
            }
            assertThat(allColumnHeader)
                    .as("Validate Downloaded file have a new column called Audited timestamp")
                    .contains(columnHeader);
        }
    }

    @Owner("kbadia@opentext.com")
    @SneakyThrows
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("711012")
    @Description("Verify that last audited timestamp added to the Issues Export")
    @Test(groups = {"regression"})
    public void validateLastAuditedTimestampTest() {
        AllureReportUtil.info("Create the new Issue Data Export Set 'Schedule' to 'Queue now'");
        var application = ApplicationDTO.createDefaultInstance();
        application = ApplicationActions.createApplication(defaultTenantDTO, true);
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        StaticScanActions.createStaticScan(staticScanDTO, application);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(application, staticFpr, true);
        StaticScanActions.completeStaticScan(application, false);
        BrowserUtil.clearCookiesLogOff();
        var issuesPage = LogInActions
                .tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(application).openIssues();
        var issue = issuesPage.openIssueByIndex(0);
        issue.setAssignedUser(defaultTenantDTO.getUserName())
                .setSeverity("Critical")
                .setDeveloperStatus("In Remediation")
                .setAuditorStatus("Remediation Required")
                .pressAddButton();
        BrowserUtil.clearCookiesLogOff();
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Audited Timestamp");
        }};
        var dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        LogInActions.tamUserLogin(defaultTenantDTO);
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage)
                .getAllRows(csvColumnsToValidate);
        assertThat(dataFromCsv)
                .as("Validate last audited timestamp added to the Issues Export")
                .isNotEmpty();
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Create a new 'Queued now' Issue Data Export " +
                "without the Audited timestamp column");
        var exportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        exportDTO.setColumnsToSelectAndUnSelect(new HashMap<>() {{
            put("Audited Timestamp", false);
        }});
        LogInActions.tamUserLogin(defaultTenantDTO);
        var generatedFile = DataExportActions.createDataExportAndDownload(exportDTO);
        CSVReader reader = new CSVReader(new FileReader(generatedFile));
        for (var header : reader.readNext()) {
            assertThat(header)
                    .as("Validate last audited timestamp does not added to the Issues Export")
                    .doesNotContain(columnHeader);
        }
    }

    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("734004")
    @Description("Verify Issues Data export show comments for existing open source issues")
    @Test(groups = {"regression", "hf"})
    public void validateCommentsInDataExports() {
        var app = ApplicationDTO.createDefaultInstance();
        app = ApplicationActions.createApplication(defaultTenantDTO, true);
        TenantScanActions.importScanTenant(app, openSourceJsonFile, FodCustomTypes.ScanType.OpenSource)
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);

        AllureReportUtil.info("Select a open source vulnerability and provide comment");
        new TenantTopNavbar().openApplications().openYourReleases().openDetailsForRelease(app)
                .openIssues()
                .openIssueByIndex(0)
                .setComment(commentValue)
                .pressAddButton();

        var dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        dataExportDTO.setColumnsToSelectAndUnSelect(new HashMap<>() {{
            put("Has Comments", true);
        }});
        var dataExportFile = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(dataExportFile).getAllRows(Collections.singletonList("Comments"));
        assertThat(dataFromCsv.stream().anyMatch(x -> Arrays.stream(x).anyMatch(y -> y.contains(commentValue))))
                .as("Comments should shown in the comments column")
                .isTrue();
    }

    @DataProvider(name = "dataExport", parallel = true)
    public static Object[][] dataExport() {
        return new Object[][]{
                {FodCustomTypes.DataExportTemplate.Applications},
                {FodCustomTypes.DataExportTemplate.ApplicationReleases},
                {FodCustomTypes.DataExportTemplate.Issues}
        };
    }
}

