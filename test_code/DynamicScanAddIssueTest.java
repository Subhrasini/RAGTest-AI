package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.dynamic.DynamicQueuePage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.IssuesActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class DynamicScanAddIssueTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
    String requestText = "TestRequest";
    String responseText = "TestResponse";
    String deleteNote = "Deleting the issue";
    String dummyText = StringUtils.repeat("requestText \n", 50);

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create an Application in Tenant, login and start dynamic scan")
    @Test(groups = {"hf", "regression"})
    public void startDynamicScanTest() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        TenantLoginPage.navigate().tenantTopNavbar.openApplications().openYourReleases();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Owner("svpillai@opentext.com")
    @Description("Verify Textbox size and scroll bar of Add Issue Popup")
    @FodBacklogItem("620001")
    @FodBacklogItem("693001")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"startDynamicScanTest"})
    public void verifyTextBoxOfAddIssue() {
        var issuesPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        var popup = issuesPage.pressAddIssue();

        AllureReportUtil.info("Verify the Request, Response, and External Notes tab have large text boxes and there " +
                "should be  one scroll bar");
        var requestTabTextBoxSize = popup.pressNextButton().goToRequestTab().getTextBoxSize();
        assertThat(requestTabTextBoxSize.getHeight())
                .as("Height of textbox should be greater than 100")
                .isGreaterThan(100);
        assertThat(requestTabTextBoxSize.getWidth())
                .as("Width of textbox should be greater than 500")
                .isGreaterThan(500);
        popup.setValueInRequestResponse(dummyText);
        int scrollHeight = Integer.parseInt(popup.scrollBar.filterBy(Condition.visible).get(0)
                .getAttribute("scrollHeight"));
        assertThat(scrollHeight > requestTabTextBoxSize.getHeight())
                .as("Scroll bar height should be more than textbox container height")
                .isTrue();
        assertThat(popup.scrollBar.filterBy(Condition.visible).get(0).getCssValue("overflow"))
                .as("Scroll bar should exist")
                .isEqualTo("scroll");

        var responseTabTextBoxSize = popup.goToResponseTab().getTextBoxSize();
        assertThat(responseTabTextBoxSize.getHeight())
                .as("Height of textbox should be greater than 100")
                .isGreaterThan(100);
        assertThat(responseTabTextBoxSize.getWidth())
                .as("Width of textbox should be greater than 500")
                .isGreaterThan(500);
        popup.setValueInRequestResponse(dummyText);
        int scrollHeight1 = Integer.parseInt(popup.scrollBar.filterBy(Condition.visible).get(0)
                .getAttribute("scrollHeight"));
        assertThat(scrollHeight1 > responseTabTextBoxSize.getHeight())
                .as("Scroll bar height should be more than textbox container height")
                .isTrue();
        assertThat(popup.scrollBar.filterBy(Condition.visible).get(0).getCssValue("overflow"))
                .as("Scrollbar should exist")
                .isEqualTo("scroll");

        var extNoteTextBoxSize = popup.goToExternalNotesTab().externalNoteContainer.filterBy(Condition.visible)
                .get(0).getSize();
        assertThat(extNoteTextBoxSize.getHeight())
                .as("Height of textbox should be greater than 100")
                .isGreaterThan(100);
        assertThat(extNoteTextBoxSize.getWidth())
                .as("Width of textbox should be greater than 500")
                .isGreaterThan(500);
        popup.setExternalNotes(dummyText);
        int scrollHeight2 = Integer.parseInt(popup.externalNoteContainer.filterBy(Condition.visible).get(0)
                .getAttribute("scrollHeight"));
        assertThat(scrollHeight2 > extNoteTextBoxSize.getHeight())
                .as("Scroll bar height should be more than textbox container height")
                .isTrue();
        popup.pressCloseButton();
        new ModalDialog().pressYes();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Add a New Issue")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"verifyTextBoxOfAddIssue"},
            alwaysRun = true)
    public void addNewIssueTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD);
        IssuesActions.addManualIssueAdmin(requestText, responseText, FodCustomTypes.ScanType.Dynamic, applicationDTO.getApplicationName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("519001")
    @Description("Error Popup When Deleting Manually Added Vulnerability")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"addNewIssueTest"})
    public void verifyDeleteIssueTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openDynamic();
        var dynamicScanOverviewPage = page(DynamicQueuePage.class).openDetailsFor(applicationDTO.getApplicationName());
        dynamicScanOverviewPage.openIssues();

        IssuesActions.deleteIssue(deleteNote);
    }
}