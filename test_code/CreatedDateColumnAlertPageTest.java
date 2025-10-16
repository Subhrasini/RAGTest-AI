package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.statics.scan.OverviewPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.page;
import static com.fortify.common.utils.WaitUtil.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("288025")
@Slf4j
public class CreatedDateColumnAlertPageTest extends FodBaseTest {
    public List<String> expectedAlertColumns = new ArrayList<>() {{
        add("Alert Type");
        add("Status");
        add("Created Date");
        add("Last Modified By");
        add("Last Modified Date");
        add("Notes");
        add("");
    }};
    TenantDTO tenantDTO;
    ApplicationDTO staticApp, dynamicApp;
    StaticScanDTO staticScanDTO;
    DynamicScanDTO dynamicScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant from admin site")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO, true);
        staticApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(staticApp, tenantDTO, true);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.InProgress);
//        dynamicApp = ApplicationDTO.createDefaultInstance();
//        ApplicationActions.createApplication(dynamicApp, tenantDTO, false);
//        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
//        dynamicScanDTO.setAssessmentType("AUTO-DYNAMIC-ALERT");
//        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate created date column in alert page of static app")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateStaticScanAlertPage() {
        LogInActions.adminLogIn().adminTopNavbar.openStatic().findScanByAppName(staticApp.getApplicationName())
                .openDetails();
        var overviewPage = page(OverviewPage.class);
        overviewPage.openAlerts();

        assertThat(overviewPage.openAlerts().getAlertColumns())
                .as("Static Scan Alert page table should contain Created date as a column")
                .hasSameElementsAs(expectedAlertColumns);
        assertThat(overviewPage.openAlerts().alertTable.getColumnIndex("Created Date"))
                .as("Created date should be  2nd column between Status and Last Modified By columns")
                .isEqualTo(2);
        assertThat(overviewPage.openAlerts().alertTable.getAllColumnValues(2))
                .as("Created date column should not be empty")
                .isNotEmpty();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate created date column in alert page of dynamic app")
    @Test(groups = {"regression"}, dependsOnMethods = {"validateStaticScanAlertPage"},enabled = false)
    public void validateDynamicScanAlertPage() {
        //The assessment type AUTO-DYNAMIC-ALERT isn't a valid assessment type as it has 0SLA. So it makes this test invalid
        var overviewPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName());
        overviewPage.openAlerts();

        Supplier<Boolean> sup = () -> overviewPage.openAlerts().alertTable.getTableElement().isDisplayed();
        waitFor(WaitUtil.Operator.Equals, true, sup, Duration.ofMinutes(30), true);

        assertThat(overviewPage.openAlerts().getAlertColumns())
                .as("Dynamic Scan Alert page table should contain Created date as a column")
                .hasSameElementsAs(expectedAlertColumns);
        assertThat(overviewPage.openAlerts().alertTable.getColumnIndex("Created Date"))
                .as("Created date should be  2nd column between Status and Last Modified By columns")
                .isEqualTo(2);
        assertThat(overviewPage.openAlerts().alertTable.getAllColumnValues(2))
                .as("Created date column should not be empty")
                .isNotEmpty();
    }
}