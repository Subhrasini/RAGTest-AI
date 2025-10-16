package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.parser.JSONParser;
import com.nimbusds.jose.shaded.json.parser.ParseException;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("1384009")
@Slf4j
public class SwaggerLookupAssessmentTypeTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("On Swagger page, we are validating LookUpItems -> AssessmentTypes json")
    @Test(groups = {"regression"})
    public void swaggerLookUpAssessmentTypesTest() throws ParseException {
        var autoTenant = "AUTO-TENANT";
        var secLeadName = "AUTO-SL";
        var swaggerPage = LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, autoTenant)
                .tenantTopNavbar.openApiSwagger();
        swaggerPage.titleElement.shouldBe(Condition.visible, Duration.ofMinutes(1));
        swaggerPage.authenticate(secLeadName, FodConfig.TAM_PASSWORD, autoTenant);
        swaggerPage.expandResource("LookupItems").expandOperation("LookupItems_LookupItemsV3_GetLookupItems")
                .setLookupParameterType("AssessmentTypes")
                .clickTryItOut();
        assertThat(swaggerPage.getResponseCode()).as("Check if response code is 200").isEqualTo("200");
        JSONParser jsonParser = new JSONParser(2048);
        Object obj = jsonParser.parse(swaggerPage.getResponseBody());
        JSONObject jsonObject = (JSONObject) obj;
        assertThat(jsonObject)
                .as("JSON Object keys should include 'items'")
                .containsKey("items");
        JSONArray items = (JSONArray) jsonObject.get("items");
        AllureReportUtil.info("""
                Reading JSON Array... NOTE: value pairs are NOT returned in order\s
                as shown on Swagger page response body ex 'value:...,text:...,group:...'. Instead,\s
                they are returned as 'text:...,value:... .'""");
        for (Object item : items) {
            assertThat(item.toString())
                    .as("Returned " + item + "should match given regex pattern")
                    .containsPattern("^\\{\"text\":[a-zA-Z0-9_ \\-+\"]*," +
                            "\"value\":\"\\d{1,5}\",\"group\":null}");
        }
    }
}

