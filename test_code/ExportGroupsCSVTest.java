package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Owner("tmagill@opentext.com")
@FodBacklogItem("420017")
@Slf4j
public class ExportGroupsCSVTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO, secondApplicationDTO, thirdApplicationDTO;
    TenantUserDTO applicationLead, leadDeveloper, developer, executive, secondApplicationLead, secondLeadDeveloper, secondDeveloper,
            secondExecutive, thirdApplicationLead, thirdLeadDeveloper, thirdDeveloper, thirdExecutive;
    String groupName, secondGroupName, thirdGroupName;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site and AUTO-TAM should create static scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        groupName = "Auto-Group" + UniqueRunTag.generate();
        secondGroupName = "Auto-Group" + UniqueRunTag.generate();
        thirdGroupName = "Auto-Group" + UniqueRunTag.generate();

        applicationDTO = ApplicationDTO.createDefaultInstance();
        secondApplicationDTO = ApplicationDTO.createDefaultInstance();
        thirdApplicationDTO = ApplicationDTO.createDefaultInstance();

        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        ApplicationActions.createApplication(secondApplicationDTO, tenantDTO, false);
        ApplicationActions.createApplication(thirdApplicationDTO, tenantDTO, false);

        applicationLead = TenantUserDTO.createDefaultInstance();
        applicationLead.setTenant(tenantDTO.getTenantCode());
        applicationLead.setUserName(applicationLead.getUserName() + "-APPLEAD");
        applicationLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setTenant(tenantDTO.getTenantCode());
        leadDeveloper.setUserName(leadDeveloper.getUserName() + "-LEADDEV");
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);

        developer = TenantUserDTO.createDefaultInstance();
        developer.setTenant(tenantDTO.getTenantCode());
        developer.setUserName(developer.getUserName() + "-DEVELOPER");
        developer.setRole(FodCustomTypes.TenantUserRole.Developer);

        executive = TenantUserDTO.createDefaultInstance();
        executive.setTenant(tenantDTO.getTenantCode());
        executive.setUserName(executive.getUserName() + "-EXECUTIVE");
        executive.setRole(FodCustomTypes.TenantUserRole.Executive);

        secondApplicationLead = TenantUserDTO.createDefaultInstance();
        secondApplicationLead.setTenant(tenantDTO.getTenantCode());
        secondApplicationLead.setUserName(secondApplicationLead.getUserName() + "-APPLEAD");
        secondApplicationLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        secondLeadDeveloper = TenantUserDTO.createDefaultInstance();
        secondLeadDeveloper.setTenant(tenantDTO.getTenantCode());
        secondLeadDeveloper.setUserName(secondLeadDeveloper.getUserName() + "-LEADDEV");
        secondLeadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);

        secondDeveloper = TenantUserDTO.createDefaultInstance();
        secondDeveloper.setTenant(tenantDTO.getTenantCode());
        secondDeveloper.setUserName(secondDeveloper.getUserName() + "-DEVELOPER");
        secondDeveloper.setRole(FodCustomTypes.TenantUserRole.Developer);

        secondExecutive = TenantUserDTO.createDefaultInstance();
        secondExecutive.setTenant(tenantDTO.getTenantCode());
        secondExecutive.setUserName(secondExecutive.getUserName() + "-EXECUTIVE");
        secondExecutive.setRole(FodCustomTypes.TenantUserRole.Executive);

        thirdApplicationLead = TenantUserDTO.createDefaultInstance();
        thirdApplicationLead.setTenant(tenantDTO.getTenantCode());
        thirdApplicationLead.setUserName(thirdApplicationLead.getUserName() + "-APPLEAD");
        thirdApplicationLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        thirdLeadDeveloper = TenantUserDTO.createDefaultInstance();
        thirdLeadDeveloper.setTenant(tenantDTO.getTenantCode());
        thirdLeadDeveloper.setUserName(thirdLeadDeveloper.getUserName() + "-LEADDEV");
        thirdLeadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);

        thirdDeveloper = TenantUserDTO.createDefaultInstance();
        thirdDeveloper.setTenant(tenantDTO.getTenantCode());
        thirdDeveloper.setUserName(thirdDeveloper.getUserName() + "-DEVELOPER");
        thirdDeveloper.setRole(FodCustomTypes.TenantUserRole.Developer);

        thirdExecutive = TenantUserDTO.createDefaultInstance();
        thirdExecutive.setTenant(tenantDTO.getTenantCode());
        thirdExecutive.setUserName(thirdExecutive.getUserName() + "-EXECUTIVE");
        thirdExecutive.setRole(FodCustomTypes.TenantUserRole.Executive);

        TenantUserActions.createTenantUsers(
                tenantDTO,
                applicationLead,
                leadDeveloper,
                developer,
                executive,
                secondApplicationLead,
                secondLeadDeveloper,
                secondDeveloper,
                secondExecutive,
                thirdApplicationLead,
                thirdLeadDeveloper,
                thirdDeveloper,
                thirdExecutive
        );

        var createGroup1 = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openGroups().pressAddGroup()
                .setName(groupName)
                .assignUser(applicationLead)
                .assignUser(leadDeveloper)
                .assignUser(developer)
                .assignUser(executive);
        createGroup1.pressSave();
        BrowserUtil.clearCookiesLogOff();

        var createGroup2 = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openGroups().pressAddGroup()
                .setName(secondGroupName)
                .assignUser(secondApplicationLead)
                .assignUser(secondLeadDeveloper)
                .assignUser(secondDeveloper)
                .assignUser(secondExecutive);
        createGroup2.pressSave();
        BrowserUtil.clearCookiesLogOff();

        var createGroup3 = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openGroups().pressAddGroup()
                .setName(thirdGroupName)
                .assignUser(thirdApplicationLead)
                .assignUser(thirdLeadDeveloper)
                .assignUser(thirdDeveloper)
                .assignUser(thirdExecutive);
        createGroup3.pressSave();
        BrowserUtil.clearCookiesLogOff();

        var groupsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openGroups();
        groupsPage.getGroupByName(groupName).pressAssignApplication().assignApplication(applicationDTO).pressSave();
        groupsPage.getGroupByName(secondGroupName).pressAssignApplication().assignApplication(secondApplicationDTO).pressSave();
        groupsPage.getGroupByName(thirdGroupName).pressAssignApplication().assignApplication(thirdApplicationDTO).pressSave();

    }

    @MaxRetryCount(3)
    @Description("TAM should be able to assign apps to group and export group.csv and data in csv should be correct")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateGroupExportCsvTest() {

        Map<String, ArrayList<String>> multiValueMap = new HashMap<String, ArrayList<String>>();
        multiValueMap.put("csvData1", new ArrayList<String>());
        multiValueMap.get("csvData1").add(groupName);
        multiValueMap.get("csvData1").add(applicationLead.getFirstName());
        multiValueMap.get("csvData1").add(applicationLead.getLastName());
        multiValueMap.get("csvData1").add(applicationLead.getUserEmail());
        multiValueMap.get("csvData1").add("Application Lead");
        multiValueMap.get("csvData1").add(applicationDTO.getApplicationName());

        multiValueMap.put("csvData2", new ArrayList<String>());
        multiValueMap.get("csvData2").add(groupName);
        multiValueMap.get("csvData2").add(leadDeveloper.getFirstName());
        multiValueMap.get("csvData2").add(leadDeveloper.getLastName());
        multiValueMap.get("csvData2").add(leadDeveloper.getUserEmail());
        multiValueMap.get("csvData2").add("Lead Developer");
        multiValueMap.get("csvData2").add(applicationDTO.getApplicationName());

        multiValueMap.put("csvData3", new ArrayList<String>());
        multiValueMap.get("csvData3").add(groupName);
        multiValueMap.get("csvData3").add(developer.getFirstName());
        multiValueMap.get("csvData3").add(developer.getLastName());
        multiValueMap.get("csvData3").add(developer.getUserEmail());
        multiValueMap.get("csvData3").add("Developer");
        multiValueMap.get("csvData3").add(applicationDTO.getApplicationName());

        multiValueMap.put("csvData4", new ArrayList<String>());
        multiValueMap.get("csvData4").add(groupName);
        multiValueMap.get("csvData4").add(executive.getFirstName());
        multiValueMap.get("csvData4").add(executive.getLastName());
        multiValueMap.get("csvData4").add(executive.getUserEmail());
        multiValueMap.get("csvData4").add("Executive");
        multiValueMap.get("csvData4").add(applicationDTO.getApplicationName());

        multiValueMap.put("csvData5", new ArrayList<String>());
        multiValueMap.get("csvData5").add(secondGroupName);
        multiValueMap.get("csvData5").add(secondApplicationLead.getFirstName());
        multiValueMap.get("csvData5").add(secondApplicationLead.getLastName());
        multiValueMap.get("csvData5").add(secondApplicationLead.getUserEmail());
        multiValueMap.get("csvData5").add("Application Lead");
        multiValueMap.get("csvData5").add(secondApplicationDTO.getApplicationName());

        multiValueMap.put("csvData6", new ArrayList<String>());
        multiValueMap.get("csvData6").add(secondGroupName);
        multiValueMap.get("csvData6").add(secondLeadDeveloper.getFirstName());
        multiValueMap.get("csvData6").add(secondLeadDeveloper.getLastName());
        multiValueMap.get("csvData6").add(secondLeadDeveloper.getUserEmail());
        multiValueMap.get("csvData6").add("Lead Developer");
        multiValueMap.get("csvData6").add(secondApplicationDTO.getApplicationName());

        multiValueMap.put("csvData7", new ArrayList<String>());
        multiValueMap.get("csvData7").add(secondGroupName);
        multiValueMap.get("csvData7").add(secondDeveloper.getFirstName());
        multiValueMap.get("csvData7").add(secondDeveloper.getLastName());
        multiValueMap.get("csvData7").add(secondDeveloper.getUserEmail());
        multiValueMap.get("csvData7").add("Developer");
        multiValueMap.get("csvData7").add(secondApplicationDTO.getApplicationName());

        multiValueMap.put("csvData8", new ArrayList<String>());
        multiValueMap.get("csvData8").add(secondGroupName);
        multiValueMap.get("csvData8").add(secondExecutive.getFirstName());
        multiValueMap.get("csvData8").add(secondExecutive.getLastName());
        multiValueMap.get("csvData8").add(secondExecutive.getUserEmail());
        multiValueMap.get("csvData8").add("Executive");
        multiValueMap.get("csvData8").add(secondApplicationDTO.getApplicationName());

        multiValueMap.put("csvData9", new ArrayList<String>());
        multiValueMap.get("csvData9").add(thirdGroupName);
        multiValueMap.get("csvData9").add(thirdApplicationLead.getFirstName());
        multiValueMap.get("csvData9").add(thirdApplicationLead.getLastName());
        multiValueMap.get("csvData9").add(thirdApplicationLead.getUserEmail());
        multiValueMap.get("csvData9").add("Application Lead");
        multiValueMap.get("csvData9").add(thirdApplicationDTO.getApplicationName());

        multiValueMap.put("csvData10", new ArrayList<String>());
        multiValueMap.get("csvData10").add(thirdGroupName);
        multiValueMap.get("csvData10").add(thirdLeadDeveloper.getFirstName());
        multiValueMap.get("csvData10").add(thirdLeadDeveloper.getLastName());
        multiValueMap.get("csvData10").add(thirdLeadDeveloper.getUserEmail());
        multiValueMap.get("csvData10").add("Lead Developer");
        multiValueMap.get("csvData10").add(thirdApplicationDTO.getApplicationName());

        multiValueMap.put("csvData11", new ArrayList<String>());
        multiValueMap.get("csvData11").add(thirdGroupName);
        multiValueMap.get("csvData11").add(thirdDeveloper.getFirstName());
        multiValueMap.get("csvData11").add(thirdDeveloper.getLastName());
        multiValueMap.get("csvData11").add(thirdDeveloper.getUserEmail());
        multiValueMap.get("csvData11").add("Developer");
        multiValueMap.get("csvData11").add(thirdApplicationDTO.getApplicationName());

        multiValueMap.put("csvData12", new ArrayList<String>());
        multiValueMap.get("csvData12").add(thirdGroupName);
        multiValueMap.get("csvData12").add(thirdExecutive.getFirstName());
        multiValueMap.get("csvData12").add(thirdExecutive.getLastName());
        multiValueMap.get("csvData12").add(thirdExecutive.getUserEmail());
        multiValueMap.get("csvData12").add("Executive");
        multiValueMap.get("csvData12").add(thirdApplicationDTO.getApplicationName());

        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Group Name");
            add("First Name");
            add("Last Name");
            add("Email");
            add("Role Name");
            add("Assigned Applications");
        }};
        File f = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openGroups().downloadGroupsExportFile();
        var dataFromCsv = new CSVHelper(f).getAllRows(csvColumnsToValidate);
        SoftAssertions softAssert = new SoftAssertions();
        for (Map.Entry<String, ArrayList<String>> e : multiValueMap.entrySet()) {
            softAssert.assertThat(dataFromCsv).as("Contents in hashmap should be in csv").contains(e.getValue().toArray(new String[0]));
        }
        softAssert.assertAll();
    }

}
