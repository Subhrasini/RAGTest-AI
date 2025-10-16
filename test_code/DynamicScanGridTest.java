package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.dynamic.DynamicQueuePage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.Ordering;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class DynamicScanGridTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to order dynamic scans by all available columns, export csv, save, apply, and delete custom queries")
    @Test(groups = {"regression"})
    public void dynamicScanGridTest() {

        AdminLoginPage.navigate().
                login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD).adminTopNavbar.openDynamic();

        var dynamicPage = page(DynamicQueuePage.class);
        dynamicPage.appliedFilters.clearAll();
        dynamicPage.paging.setRecordsPerPage(1000);

        //check ordering

        var ordering = new Ordering(dynamicPage.columns, dynamicPage.getColumnValues);

        ordering.verifyOrderForColumn("Scan Id");
        ordering.verifyOrderForColumn("Analysis Status");
        ordering.verifyOrderForColumn("Tenant");
        ordering.verifyOrderForColumn("Assessment Type");
        //ordering.verifyOrderForColumn("Elapsed Time");

        // check filters

        dynamicPage.filters.setFilterByName("Analysis Status").clickFilterCheckboxByName("Completed").apply();
        var statuses = dynamicPage.getAnalysisStatuses();
        for (var status : statuses)
            assertThat(status).as("Validate filtering").isEqualTo("Completed");

        dynamicPage.appliedFilters.clearAll();

//        var csv = dynamicPage.export();
//        assertThat(csv.length()).as("Check downloaded csv is not empty file").isGreaterThan(0);
//        AllureAttachmentsUtil.attachFile(csv, csv.getName(), "", "text/plain");

        dynamicPage.filters.expandAllFilters();

        dynamicPage.filters.setFilterByName("Analysis Status").clickFilterCheckboxByName("Completed");
        dynamicPage.filters.setFilterByName("Tenant").clickFilterCheckboxByName("AUTO-TENANT").applyAllChecked();

        var customQueryName = "CustomQuery-" + UniqueRunTag.generate();
        dynamicPage.appliedFilters.saveQuery(customQueryName);

        dynamicPage.appliedFilters.clearAll();
        dynamicPage.filters.expandAllFilters();
        dynamicPage.filters.setFilterByName("Custom Queries");
        dynamicPage.filters.clickFilterOptionByName(customQueryName);

        statuses = dynamicPage.getAnalysisStatuses();
        for (var status : statuses)
            assertThat(status).as("Validate filtering").isEqualTo("Completed");

        var tenants = dynamicPage.getTenants();
        for (var tenant : tenants)
            assertThat(tenant).as("Validate filtering").isEqualTo("AUTO-TENANT");

        dynamicPage.appliedFilters.deleteCustomQuery(customQueryName);
        assertThat(dynamicPage.appliedFilters.getCustomQueryNames())
                .as("Check if custom query deleted").doesNotContain(customQueryName);
    }

    @Owner("ysmal@opentext.com")
    @MaxRetryCount(3)
    @Description("Ordering by elapsed time has floating issue")
    @FodBacklogItem("166294")
    @Test(groups = {"regression"}, enabled = false)
    public void validateOrderingForElapsedTimeColumn() {
        AdminLoginPage.navigate().
                login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD).adminTopNavbar.openDynamic();
        var dynamicPage = page(DynamicQueuePage.class);
        dynamicPage.appliedFilters.clearAll();
        dynamicPage.paging.setRecordsPerPage(1000);
        var ordering = new Ordering(dynamicPage.columns, dynamicPage.getColumnValues);
        ordering.verifyOrderForColumn("Elapsed Time");
    }

    @FodBacklogItem("591004")
    @Description("Disable data export button functionality check while developers optimize query")
    @Test(enabled = false)
    public void tempDisableCheckForExportButton() {
        /*
        Uncomment rows 58-60
        */
    }
}
