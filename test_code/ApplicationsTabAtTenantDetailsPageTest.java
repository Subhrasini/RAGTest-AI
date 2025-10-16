package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Owner("ysmal@opentext.com")
public class ApplicationsTabAtTenantDetailsPageTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO firstApp, secondApp, thirdApp, fourthApp, fifthApp;
    ReleaseDTO secondRelease, thirdRelease;
    String confirmPurgeMessage = "This action will purge the database and file storage. Are you sure you want to continue?";
    String purgingMessage = "Please wait while the system purges the selected data.";
    String undeleteMessage = "Do you wish to undelete this item?";
    String columnNameToValidate = "Release Id";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate application page functionality on application details page.")
    @Test(groups = {"regression"})
    public void applicationsTabAtTenantDetailsPageTest() {
        AllureReportUtil.info("Test Data Preparation");
        tenantDTO = TenantDTO.createDefaultInstance();
        firstApp = ApplicationDTO.createDefaultInstance();
        secondApp = ApplicationDTO.createDefaultInstance();
        thirdApp = ApplicationDTO.createDefaultInstance();
        fourthApp = ApplicationDTO.createDefaultInstance();
        fifthApp = ApplicationDTO.createDefaultInstance();
        secondRelease = ReleaseDTO.createDefaultInstance();
        thirdRelease = ReleaseDTO.createDefaultInstance();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplications(firstApp, secondApp, thirdApp, fourthApp, fifthApp);
        ApplicationActions.purgeApplications(secondApp, fourthApp, fifthApp);

        ReleaseActions.createReleases(thirdApp, secondRelease, thirdRelease);
        ReleaseActions.purgeRelease(thirdApp, thirdRelease);

        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Applications Tab At Tenant Details Page Test");

        var appForValidateSearch = secondApp.getApplicationName();
        var appForValidateReleases = thirdApp.getApplicationName();
        var expectedTitle = appForValidateReleases + " | Releases";

        var applicationsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantCode())
                .openApplications();

        var expectedColumnHeaders = new ArrayList<String>() {{
            add("Application Id");
            add("Name");
            add("Modified Date");
            add("Deleted");
            add("");
        }};
        assertThat(applicationsPage.table.getColumnHeaders())
                .hasSameElementsAs(expectedColumnHeaders);

        var ordering = new Ordering(applicationsPage.table);
        expectedColumnHeaders.remove("");
        for (var column : expectedColumnHeaders) {
            ordering.verifyOrderForColumn(column);
        }

        new PagingActions().validatePaging();
        new PagingActions().validatePagination();

        applicationsPage.findWithSearchBox(appForValidateSearch);
        var applications = applicationsPage.getAllApplications();

        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getName()).isEqualTo(appForValidateSearch);
        assertThat(applications.get(0).getDeleted()).isEqualTo("True");

        applicationsPage.findWithSearchBox(appForValidateReleases);
        applications = applicationsPage.getAllApplications();
        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getName()).isEqualTo(appForValidateReleases);

        var additionalDataPopup = applications.get(0).pressAdditionalData();
        assertThat(additionalDataPopup.popup.isDisplayed()).isTrue();

        assertThat(additionalDataPopup.table.getColumnHeaders()).contains("Name", "Value");
        assertThat(additionalDataPopup.getValueByDataName("ApplicationName")).isEqualTo(appForValidateReleases);
        assertThat(additionalDataPopup.getValueByDataName("ApplicationMonitoringEnabled")).isEqualTo("False");

        additionalDataPopup.pressClose();

        applicationsPage.findWithSearchBox(appForValidateReleases);
        var releasesPage = applicationsPage.getAllApplications().get(0).pressReleases();

        assertThat(releasesPage.getTitle()).isEqualTo(expectedTitle);

        var expectedReleasesHeaders = new ArrayList<String>() {{
            add("Release Id");
            add("Name");
            add("Modified Date");
            add("Deleted");
            add("");
        }};
        assertThat(releasesPage.table.getColumnHeaders())
                .hasSameElementsAs(expectedReleasesHeaders);

        ordering = new Ordering(releasesPage.table);
        ordering.verifyOrderForColumn(columnNameToValidate);

        applicationsPage = releasesPage.adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantCode())
                .openApplications();

        var modal = applicationsPage.getApplicationByName(fourthApp.getApplicationName())
                .pressPurge();
        assertThat(modal.modalElement.isDisplayed()).isTrue();
        assertThat(modal.getMessage()).isEqualTo(confirmPurgeMessage);
        modal.pressYes();

        modal = new ModalDialog($("#modalPurge"));
        assertThat(modal.modalElement.shouldBe(Condition.visible, Duration.ofSeconds(10)).$("div p").text().trim())
                .isEqualTo(purgingMessage);
        modal.modalElement.shouldNotBe(Condition.visible, Duration.ofMinutes(3));

        refresh();
        assertThat(applicationsPage.getApplicationByName(fourthApp.getApplicationName()))
                .as("App should be purged")
                .isNull();

        applicationsPage.getApplicationByName(fifthApp.getApplicationName()).pressUndelete();
        modal = new ModalDialog();

        assertThat(modal.getMessage()).isEqualTo(undeleteMessage);
        modal.pressYes();
        applicationsPage.spinner.waitTillLoading();
        new BrowserUtil().waitAjaxLoaded();
        var deletedValue = applicationsPage.getApplicationByName(fifthApp.getApplicationName()).getDeleted();
        assertThat(deletedValue).isEqualTo("False");

        BrowserUtil.clearCookiesLogOff();

        var applicationPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openApplications();
        new Paging().setRecordsPerPage_100();
        var apps = applicationPage.getAllApps()
                .stream().map(YourApplicationCell::getName).collect(Collectors.toList());

        assertThat(apps).hasSize(3)
                .contains(fifthApp.getApplicationName())
                .doesNotContain(fourthApp.getApplicationName());
    }
}
