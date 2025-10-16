package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.entities.DataExportDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.DataExportActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("783001")
@Slf4j
public class TaskQueueCleanupTest extends FodBaseTest {
    String taskQueueId1, taskQueueId2, taskQueueId3, taskQueueId4, previousDay, beforeFiveDays;
    String threadName = "TaskQueueCleanup";
    String taskName = "DataExportTask";
    String dateTimePattern = "yyyy-MM-dd HH:mm:ss.SS";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePattern);
    DataExportDTO dataExportDTO;
    List<String> removedTasksList = new ArrayList<>() {{
        add("TimerCalculateProjectScanDetailElapsedTime");
        add("TimerCancelOldScans");
        add("TimerCleanApiImportFileSession");
        add("TimerCleanDatastore");
        add("TimerCleanOldStats");
        add("TimerCleanTaskQueueHistory");
        add("TimerCustomerTenantRenewalEmail");
        add("TimerNotificationCleanup");
        add("TimerProjectVersionScanAlertCheck");
        add("TimerQueuePurgeTasks");
        add("TimerRefreshScanStarRating");
        add("TimerRemoveTempTamTenantAccess");
        add("TimerRepositoryHealthCheck");
        add("TimerRepositoryHealthCheckNoRebuild");
        add("TimerScanReschedule");
        add("TimerTenantFileUploadCleanupTask");
        add("TimerUserPersonalAccessTokenReminder");
    }};

    HashMap<String, Integer> expectedTaskAndPollingInterval = new HashMap<>() {{
        put("ApiImportFileSessionCleanupTask", 604800);
        put("CalculateProjectScanDetailElapsedTask", 900);
        put("CancelOldScansTask", 86400);
        put("NotificationCleanupTask", 86400);
        put("ReleaseScanAlertCheckTask", 300);
        put("QueuePurgeTask", 86400);
        put("RefreshScanStarRatingTask", 604800);
        put("RemoveTempTamTenantAccessTask", 3600);
        put("RepositoryOldStatsCleanupTask", 86400);
        put("RepositoryUserPermissionCleanupTask", 86400);
        put("ScanRescheduleTask", 86400);
        put("TenantFileUploadCleanupTask", 86400);
        put("TenantRenewalEmailTask", 86400);
        put("UserPersonalAccessTokenReminderTask", 86400);
    }};

    List<String> renamedTasksList = new ArrayList<>(expectedTaskAndPollingInterval.keySet());

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tasks for test")
    @Test(groups = {"regression"})
    public void prepareTaskForTest() {
        AllureReportUtil.info("Delete existing TaskQueueCleanUp Thread");
        var threadCell = LogInActions.adminLogIn().adminTopNavbar.openConfiguration().openTaskService()
                .getAllThreadGroups().get(0).expand().getThreadByName(threadName);
        if (Integer.parseInt(threadCell.getThreadsCount()) > 0) {
            threadCell.deleteThreads().pressSave();
        }
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Start 4 tasks by generating data exports");
        LogInActions.tamUserLogin(defaultTenantDTO);
        dataExportDTO = DataExportDTO.createDefaultInstance();
        DataExportActions.createExport(dataExportDTO);
        DataExportActions.createExport(dataExportDTO);
        DataExportActions.createExport(dataExportDTO);
        DataExportActions.createExport(dataExportDTO);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Update status and date for taskQueue Ids using database scripts")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTaskForTest"})
    public void verifyTaskStatusAfterDbUpdation() {
        var tasksPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration().openTaskService().openTasks();
        tasksPage.filters.setFilterByName("Task Type").expand().clickFilterOptionByName(taskName);
        var taskList = tasksPage.getTaskGrid().getAllColumnValues(1);
        taskQueueId1 = taskList.get(0);
        taskQueueId2 = taskList.get(1);
        taskQueueId3 = taskList.get(2);
        taskQueueId4 = taskList.get(3);
        tasksPage.appliedFilters.clearAll();

        LocalDateTime localDateTime = LocalDateTime.now();
        previousDay = localDateTime.minusDays(1).format(formatter);
        beforeFiveDays = localDateTime.minusDays(5).format(formatter);

        AllureReportUtil.info("Update date and status for the tasks");
        var db = new FodSQLUtil();
        var updateQueryTask1 = "update TaskQueueMeta set  taskstatus = 255  ,taskqueuedatetime = " + "'"
                + beforeFiveDays + "'" + " where taskqueueid = "
                + "'" + taskQueueId1 + "'" + ";";
        db.executeQuery(updateQueryTask1);
        var updateQueryTask2 = "update TaskQueueMeta set  taskstatus = 2  ,taskqueuedatetime = " + "'"
                + previousDay + "'" + " where taskqueueid = "
                + "'" + taskQueueId2 + "'" + ";";
        db.executeQuery(updateQueryTask2);
        var updateQueryTask3 = "update TaskQueueMeta set  taskstatus = 1  where taskqueueid = "
                + "'" + taskQueueId3 + "'" + ";";
        db.executeQuery(updateQueryTask3);
        var updateQueryTask4 = "update TaskQueueMeta set  taskstatus = 4 where taskqueueid = "
                + "'" + taskQueueId4 + "'" + ";";
        db.executeQuery(updateQueryTask4).close();

        AllureReportUtil.info("Verify status of the tasks are updated");
        tasksPage.filters.setFilterByName("Task Type").expand().clickFilterOptionByName(taskName);
        assertThat(tasksPage.getStatusByID(taskQueueId1))
                .as("Status of the Task1 should be updated as Failed")
                .isEqualTo("Failed");
        assertThat(tasksPage.getStatusByID(taskQueueId2))
                .as("Status of the Task1 should be updated as Failed")
                .isEqualTo("Completed (keep one day)");
        assertThat(tasksPage.getStatusByID(taskQueueId3))
                .as("Status of the Task1 should be updated as Failed")
                .isEqualTo("Completed");
        assertThat(tasksPage.getStatusByID(taskQueueId4))
                .as("Status of the Task1 should be updated as Failed")
                .isEqualTo("Requeued");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify tasks are purged")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyTaskStatusAfterDbUpdation"})
    public void verifyPurgingOfTasks() {
        var threadsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration().openTaskService();
        threadsPage.getAllThreadGroups().get(0).expand()
                .getThreadByName(threadName)
                .pressAddThread()
                .pressSave();
        AllureReportUtil.info("Verify 4 DataExportTasks are purged");
        var taskPage = threadsPage.openTasks();
        taskPage.filters.setFilterByName("Task Type").expand().clickFilterOptionByName(taskName);
        WaitUtil.waitForTrue(() -> taskPage.getTaskGrid().isEmpty(), Duration.ofMinutes(10), true);
        assertThat(taskPage.getTaskGrid().isEmpty())
                .as("Data export task should be purged")
                .isTrue();
    }

    @FodBacklogItem("972008")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify migration of timer task as polling task")
    @Test(groups = {"regression"})
    public void verifyMigrationOfTimerTaskAsPollingTask() {
        AllureReportUtil.info("Verify given list of timer tasks should not be present in Configure Timed Task Popup");
        var threadsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration().openTaskService();
        var timedTasksPopup = threadsPage.pressConfigureTimedTasks();
        assertThat(timedTasksPopup.getAllTimedTasksNames())
                .as("Timer tasks list should not be present")
                .doesNotContainAnyElementsOf(removedTasksList);
        timedTasksPopup.pressClose();

        AllureReportUtil.info("Verify allowed threads list should contain new tasks");
        assertThat(threadsPage.getAllTaskTypes())
                .as("Allowed threads list should contain new renamed tasks")
                .containsAll(renamedTasksList)
                .as("Allowed threads list should not contain given tasks")
                .doesNotContain("TaskQueueHistoryCleanupTask", "RepositoryHealthCheckTask", "RepositoryHealthCheckNoRebuildTask");

        AllureReportUtil.info("Verify polling interval should be same as required");
        for (var taskName : expectedTaskAndPollingInterval.entrySet()) {
            var query = "Select PollingIntervalSeconds from ServerTaskType where Name=" + "'" + taskName.getKey() + "'" + ";";
            var pollingInterval = Integer.parseInt(new FodSQLUtil().getStringValueFromDB(query));
            assertThat(pollingInterval)
                    .as("Polling interval time should be same as required")
                    .isEqualTo(taskName.getValue());
        }
    }
}
