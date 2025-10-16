package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.ui.pages.common.common.cells.IssueCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("544009")
@Slf4j
public class FileNameSortingIssuesTest extends FodBaseTest {
    ApplicationDTO applicationDTO;
    DynamicScanDTO dynamicScanDTO;
    String fileName = "payloads/fod/dynamic.zero.fpr";
    IssuesCounters expectedIssueCounters = new IssuesCounters(1, 3, 4, 5, 25);

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Application and Dynamic Scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Start and complete a Dynamic Scan with Vulnerabilities");
        applicationDTO = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), fileName, true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and activate Security Lead role")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyFileSortingInIssuesPage() {
        var adminIssuesPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();
        var issues = adminIssuesPage.getIssuesCounters();
        assertThat(issues)
                .as("Verify Issue counts on admin issues pages")
                .isEqualTo(expectedIssueCounters);

        AllureReportUtil.info("Verify in admin site issues are sorting by filename and code line on Issues page");
        var headerList = adminIssuesPage.clickAll().getGroupHeaders();
        for (var header : headerList) {
            var subIssues = adminIssuesPage.getIssuesByHeaders(String.valueOf(header));
            var subIssueTitles = subIssues.stream().map(IssueCell::getTitle).collect(Collectors.toList());
            assertThat(subIssueTitles.stream().map(String::toLowerCase).collect(Collectors.toList()))
                    .as("Admin site-sublist are sorting by filename and code line")
                    .isSorted();
        }
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Login as Security lead");
        var secLeadIssuesPage = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForApplication(applicationDTO.getApplicationName())
                .openIssues();
        issues = secLeadIssuesPage.getIssuesCounters();
        assertThat(issues)
                .as("Verify Issue counts on tenant issues page")
                .isEqualTo(expectedIssueCounters);

        AllureReportUtil.info("Verify in Tenant site issues are sorting by filename and code line on Issues page");
        var tenantHeaderList = secLeadIssuesPage.clickAll().getGroupHeaders();
        for (var header : tenantHeaderList) {
            var subIssues = secLeadIssuesPage.getIssuesByHeaders(String.valueOf(header));
            var subIssueTitles = subIssues.stream().map(IssueCell::getTitle).collect(Collectors.toList());
            assertThat(subIssueTitles.stream().map(String::toLowerCase).collect(Collectors.toList()))
                    .as("Tenant site - sublist are sorting by filename and code line")
                    .isSorted();
        }
    }
}