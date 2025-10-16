package com.fortify.fod.ui.test.regression;

import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("595026")
@Slf4j
public class SwaggerNoWindowsNoBlackberryTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Description("TAM check Swagger API-MobileScans and listed operation to validate removal of list elements")
    @Test(groups = {"regression"})
    public void verifyListElementsRemovedTest() {

        var swaggerPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openApiSwagger()
                .expandResource("MobileScans")
                .expandOperation("MobileScans_MobileScansV3_Scan")
                .getFrameworkType();
        assertThat(swaggerPage)
                .as("Framework Type should not contain 'Windows' or 'Blackberry'")
                .doesNotContain("Windows", "Blackberry");
        assertThat(swaggerPage)
                .as("List should contain iOS and Android")
                .contains("iOS", "Android");
    }


}
