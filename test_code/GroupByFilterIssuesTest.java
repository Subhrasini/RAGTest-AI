package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.elements.Dropdown;
import com.fortify.fod.common.elements.Spinner;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.MobileScanResultsDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class GroupByFilterIssuesTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    MobileScanDTO mobileScanDTO;
    MobileScanResultsDTO mobileScanResultsDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("407016")
    @FodBacklogItem("688002")
    @Description("Check Name and Source Group By filters are not working on scan types")
    @Test(groups = {"hf", "regression"})
    public void validateGroupByIssuesTest() {
        applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanResultsDTO = MobileScanResultsDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.completeMobileScan(applicationDTO, mobileScanResultsDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var mobileQueuePage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME,
                FodConfig.ADMIN_PASSWORD).adminTopNavbar.openMobile();
        mobileQueuePage.appliedFilters.clearAll();
        var issuesPage = mobileQueuePage
                .openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        int issuesCountBefore = issuesPage.getAllCount();
        issuesPage.clickAll();
        issuesPage.groupBy("Check Name");
        assertThat(issuesPage.getAllCount())
                .as("Total issues count shouldn't change after grouping by Check Name")
                .isEqualTo(issuesCountBefore);
        assertThat(issuesPage.issueGroups)
                .as("Validate issues group count after grouping by Check Name").isNotEmpty();

        new Dropdown(issuesPage.groupByElement).selectOptionByText("Scan Tool");
        new Spinner().waitTillLoading();
        assertThat(issuesPage.getAllCount())
                .as("Total issues count shouldn't change after grouping by Scan Tool")
                .isEqualTo(issuesCountBefore);
        assertThat(issuesPage.issueGroups)
                .as("Validate issues group count after grouping by Scan Tool").isNotEmpty();
        var issuesMap = issuesPage.getIssueGroupsCounts();
        for (var issue : issuesMap.entrySet()) {
            assertThat(issue.getKey()).as("Validate source name").isEqualTo("MAST");
            assertThat(issue.getValue()).as("Validate issue count")
                    .isEqualTo(mobileScanResultsDTO.getAllCount());
        }
    }

}
