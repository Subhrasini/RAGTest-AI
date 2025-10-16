package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.elements.AppliedFilters;
import com.fortify.fod.common.elements.DatePicker;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.common.common.cells.EventLogCellMain;
import com.fortify.fod.ui.pages.common.common.pages.EventLogPageMain;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
public class SearchAndExportOfEventLogTest extends FodBaseTest {
    String expectedQueryString = "sd=%s&ed=%s";
    TenantUserDTO localUser;

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("682011")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Search and export functionality in tenant side event log page")
    @Test(groups = {"regression", "hf"})
    public void verifySearchAndExportOnEventLog() throws IOException, CsvException {

        AllureReportUtil.info("Verify exported CSV file should list the Events based on the specified Date Query string");
        var eventLogPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration().openEventLog();
        open(FodConfig.TEST_ENDPOINT_TENANT + "/Admin/EventLog");
        var logsDates = eventLogPage.getAllLogs().stream()
                .map(EventLogCellMain::getEventDate)
                .collect(Collectors.toList());
        var lastDate = logsDates.get(logsDates.size() - 1);
        var middleDate = logsDates.size() % 2 == 0
                ? logsDates.get(logsDates.size() / 2)
                : logsDates.get((logsDates.size() - 1) / 2);

        String eventUrl = FodConfig.TEST_ENDPOINT_TENANT + "/Admin/EventLog?"
                + String.format(expectedQueryString, lastDate, middleDate);

        var paging = new Paging();
        open(eventUrl);
        var eventFile = eventLogPage.downloadEventLogFile();
        assertThat(eventFile)
                .as("CSV name should be history.csv")
                .hasName("history.csv")
                .as("File should not be empty")
                .isNotEmpty();
        int csvDataCount = getRowCount(eventFile.getAbsolutePath());
        var totalRecords = paging.getTotalRecords();
        assertThat(totalRecords)
                .as("Record count of CSV File should be equal to Event Log Record")
                .isLessThanOrEqualTo(csvDataCount - 1);
        if (csvDataCount - 1 != totalRecords)
            AllureReportUtil.warn(String.format("Csv file has: %d records. UI total count has: %d records",
                    csvDataCount - 1, totalRecords));

        AllureReportUtil.info("Verify exported CSV file should list the Events based on the specified Date Query string " +
                "and/or Search Entry");
        var records = eventLogPage.getAllLogs();
        var searchUser = records.get(records.size() - 1).getUser();
        eventLogPage.findWithSearchBox(searchUser);
        var searchStringFile = eventLogPage.downloadEventLogFile();
        var totalCountUI = paging.getTotalRecords();
        int csvDataCount1 = getRowCount(searchStringFile.getAbsolutePath());
        assertThat(totalCountUI)
                .as("Record count of CSV File should be equal to Event Log Record")
                .isEqualTo(csvDataCount1 - 1);
        assertThat(searchStringFile)
                .as("CSV name should be history.csv")
                .hasName("history.csv")
                .as("File should not be empty")
                .isNotEmpty();

        assertThat(fileContainsValue(searchStringFile.getAbsolutePath(), searchUser))
                .as("Search string  value should be present in the file ")
                .isTrue();
    }

    @SneakyThrows
    int getRowCount(String filename) {
        File inputFile = new File(filename);
        CSVReader reader = new CSVReader(new FileReader(inputFile));
        return reader.readAll().size();
    }

    boolean fileContainsValue(String filename, String searchString) throws IOException, CsvException {
        File inputFile = new File(filename);
        CSVReader reader = new CSVReader(new FileReader(inputFile));
        List<String[]> csvBody = reader.readAll();
        return csvBody.stream().anyMatch(x -> Arrays.stream(x).anyMatch(y -> y.contains(searchString)));
    }

    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("596043")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant Portal Event Log Introduce filtering capabilities to Event Log grid")
    @Test(groups = {"regression"}, dataProvider = "pagesToVerify")
    public void verifyFilterInEventLogTest(String path, String dateFormat, String[] expectedColumns) {

        SoftAssertions softAssertions = new SoftAssertions();
        EventLogPageMain eventLogPage = null;
        ApplicationDTO applicationDTO;
        AppliedFilters appliedfilters = new AppliedFilters();
        switch (path) {
            case "Admin":
                eventLogPage = LogInActions.adminLogIn().adminTopNavbar.openEventLog();
                break;
            case "TenantAdministration":
                eventLogPage = LogInActions.tamUserLogin(defaultTenantDTO)
                        .tenantTopNavbar.openAdministration().openEventLog();
                break;
            case "TenantApplication":
                applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
                eventLogPage = new TenantTopNavbar().openApplications()
                        .openDetailsFor(applicationDTO.getApplicationName()).openEventLog();
                break;
        }

        assertThat(new Paging().getTotalRecords()).as("Log should be in log event")
                .isNotZero();
        softAssertions.assertThat(eventLogPage.filterButton.isDisplayed())
                .as("Filter button exists and visible")
                .isTrue();
        assertThat(eventLogPage.exportButton.isDisplayed())
                .as("Export button exists and visible")
                .isTrue();
        softAssertions.assertThat(eventLogPage.searchTextField.isDisplayed())
                .as("Search field exists and visible")
                .isTrue();

        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE FROM:").isFilterDisplayed())
                .as("Verify Event Date From filter is already applied")
                .isTrue();
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE TO:").isFilterDisplayed())
                .as("Verify Event Date To filter is already applied")
                .isTrue();

        softAssertions.assertThat(new CSVHelper(eventLogPage.exportEventLogFile()).getColumnHeaders())
                .as("Verify csv file contains the following columns : " + Arrays.toString(expectedColumns))
                .isEqualTo(expectedColumns);
        softAssertions.assertThat(new Filters().expandAllFilters().getAllFilters().get(0))
                .as("Verify the right filter panel contains a section for the Event Date")
                .isEqualTo("Event Date");
        softAssertions.assertThat(eventLogPage.fromFilter.isDisplayed())
                .as("Verify event Date filter contains input fields: From")
                .isTrue();
        softAssertions.assertThat(eventLogPage.toFilter.isDisplayed())
                .as("Verify event Date filter contains input fields: To")
                .isTrue();


        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE FROM:").isClearIconDisplayed())
                .as("Verify clear icon isn't displayed in default filter-Event Date From")
                .isFalse();
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE TO:").isClearIconDisplayed())
                .as("Verify clear icon isn't displayed in default filter - Event Date To")
                .isFalse();
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE FROM:").getBackgroundColour())
                .as("Verify default filter Event Date From use a grey background")
                .isEqualTo("rgba(220, 222, 223, 1)");
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE TO:").getBackgroundColour())
                .as("Verify default filter Event Date To use a grey background")
                .isEqualTo("rgba(220, 222, 223, 1)");
        softAssertions.assertThat(appliedfilters.isClearAllDisplayed())
                .as("Clear Filters option in the filter summary section is not shown for DEFAULT filters")
                .isFalse();
        softAssertions.assertThat(eventLogPage.fromFilter.getValue())
                .as("Verify DEFAULT filter vaslues are not displayed in the right filter panel: From")
                .isEqualTo("");
        softAssertions.assertThat(eventLogPage.toFilter.getValue())
                .as("Verify DEFAULT filter values are not displayed in the right filter panel: To")
                .isEqualTo("");

        eventLogPage.fromFilter.click();
        var currentDate = LocalDate.now();
        var updatedDate = currentDate.minusYears(1).minusMonths(1);
        var expectedMon = updatedDate.getMonth().toString() + " " + updatedDate.getYear();

        var dateList = List.of(
                dateToFormat(currentDate.minusDays(1), dateFormat),
                dateToFormat(currentDate.minusDays(2), dateFormat),
                dateToFormat(currentDate.plusDays(1), dateFormat),
                dateToFormat(currentDate, dateFormat)
        );
        softAssertions.assertThat(new DatePicker().goToCalenderEnd().getDatePickerTitle())
                .as("Verify the date range of either control is limited to dates from 13 months ago through the current date")
                .isEqualToIgnoringCase(expectedMon);
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE FROM:").getValue())
                .as("Verify EVENT DATE FROM: default value is previous day")
                .isIn(dateList);
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE TO:").getValue())
                .as("Verify EVENT DATE TO: default value is current day")
                .isIn(dateList);

        eventLogPage.fromFilter.setValue(dateToFormat(currentDate.minusDays(5), dateFormat));
        new Filters().setFilterByName("EVENT DATE").apply();
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE TO:").getValue())
                .as("Verify If the user only specifies the From filter then a" +
                        " *DEFAULT* To filter will be set to the current day")
                .isIn(dateList);

        eventLogPage.fromFilter.clear();
        appliedfilters.getFilterByName("EVENT DATE FROM:").clear();
        eventLogPage.toFilter.setValue(dateToFormat(currentDate.minusDays(6), dateFormat));
        new Filters().setFilterByName("EVENT DATE").apply();
        softAssertions.assertThat(appliedfilters.getDefaultFilter("EVENT DATE FROM:").getValue())
                .as("If the user only specifies a To filter then the From filter " +
                        "will be automatically populated with the previous day")
                .isEqualTo(dateToFormat(currentDate.minusDays(7), dateFormat));
        if (!path.equals("Admin")) {
            softAssertions.assertThat(eventLogPage.helpIcon.getAttribute("href"))
                    .as("Verify help icon after the Event Log title that links to the Data " +
                            "Retention Policy help page")
                    .contains("/Docs/en/index.htm#cshid=120");
        }
        softAssertions.assertAll();
    }

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("800012")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify username value should be included in the event log when tenant user record is created/modified/deleted")
    @Test(groups = {"regression"})
    public void verifyUsernameInEventLog() {
        localUser = TenantUserDTO.createDefaultInstance();
        localUser.setTenant(defaultTenantDTO.getTenantName());
        LogInActions.tamUserLogin(defaultTenantDTO);
        var tenantTopNav = new TenantTopNavbar();
        tenantTopNav.openAdministration().openUsers().createUser(localUser);
        assertThat(isUsernamePresent("Local User Created", localUser.getUserName()))
                .as("Username value should be present in the local user created entry in the notes section")
                .isTrue();

        tenantTopNav.openAdministration().openUsers().findUserByName(localUser.getUserName())
                .editUserByName(localUser.getUserName())
                .setFirstName("AlphaUser")
                .pressSaveBtn();
        assertThat(isUsernamePresent("Local User Updated", localUser.getUserName()))
                .as("Username value should be present in the local user updated entry in the notes section")
                .isTrue();

        tenantTopNav.openAdministration().openUsers().findWithSearchBox(localUser.getUserName())
                .deleteUserByName(localUser.getUserName());
        assertThat(isUsernamePresent("Local User Deleted", localUser.getUserName()))
                .as("Username value should be present in the local user deleted entry in the notes section")
                .isTrue();
    }

    public boolean isUsernamePresent(String typeValue, String userName) {
        return WaitUtil.waitForTrue(() -> new TenantTopNavbar().openAdministration().openEventLog().getAllLogs().stream()
                        .filter(x -> x.getType().equals(typeValue))
                        .filter(x -> x.getNotes().contains(userName)).findFirst().isPresent(),
                Duration.ofMinutes(1), true);
    }

    @DataProvider(name = "pagesToVerify", parallel = true)
    public Object[][] testData() {
        var adminFields = new String[]{"Event Log Id", "Event Date", "Type", "User", "Tenant", "Notes"};
        var tenantAdministrationFields = new String[]{"Event Log Id", "Event Date", "Type", "User", "Application", "Notes"};
        var tenantApplicationFields = new String[]{"Event Log Id", "Event Date", "Type", "User", "Application", "Notes"};

        return new Object[][]{
                {"Admin", "MM/dd/YYYY", adminFields},
                {"TenantAdministration", "YYYY/MM/dd", tenantAdministrationFields},
                {"TenantApplication", "YYYY/MM/dd", tenantApplicationFields}
        };
    }

    private String dateToFormat(LocalDate date, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }
}
